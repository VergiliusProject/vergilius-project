package com.vergiliusproject.api;

import com.vergiliusproject.api.ApiDtos.EnumConstant;
import com.vergiliusproject.api.ApiDtos.FieldNode;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a machine-readable type tree from a root Ttype plus the closure map
 * produced by TypeLoader. Mirrors FieldBuilder's semantics: named non-root
 * types become REF nodes (which cuts cycles structurally), anonymous types
 * expand inline, bitfield names are parsed, FUNCTION return-sentinel rows
 * become returnType, and anonymous union/struct nesting is inferred from
 * duplicate offsets exactly as the web renderer does. Zero database access.
 */
public class TypeTreeBuilder {

    public static final int NODE_BUDGET = 1024;

    private final Map<Integer, Ttype> closure;
    private final String hrefBase;
    private int anonCounter = 0;
    private int nodesCreated = 0;

    public TypeTreeBuilder(Map<Integer, Ttype> closure, String hrefBase) {
        this.closure = closure;
        this.hrefBase = hrefBase;
    }

    public TypeNode buildRoot(Ttype root) {
        return build(root, true, new ArrayDeque<>(), 0);
    }

    TypeNode build(Ttype type, boolean isRoot, Deque<Integer> expansionPath, int baseOffset) {
        if (nodesCreated++ > NODE_BUDGET) {
            return truncatedNode(type);
        }

        if (!isRoot && isAggregate(type) && type.getName() != null) {
            return ref(type);
        }

        if (isInlineExpansion(type) && expansionPath.contains(type.getId())) {
            throw new IllegalStateException(
                    "Cycle invariant violated: type id " + type.getId()
                            + " cycles without passing through a named type");
        }

        return switch (type.getKind()) {
            case BASE -> base(type);
            case POINTER -> pointer(type, isRoot, expansionPath, baseOffset);
            case ARRAY -> array(type, expansionPath);
            case FUNCTION -> function(type, expansionPath);
            case STRUCT -> struct(type, isRoot, expansionPath, baseOffset);
            case UNION -> union(type, isRoot, expansionPath, baseOffset);
            case ENUM -> enumNode(type, isRoot);
        };
    }

    private static boolean isAggregate(Ttype t) {
        return t.getKind() == Ttype.Kind.STRUCT
                || t.getKind() == Ttype.Kind.UNION
                || t.getKind() == Ttype.Kind.ENUM;
    }

    private static boolean isInlineExpansion(Ttype t) {
        return t.getKind() == Ttype.Kind.STRUCT || t.getKind() == Ttype.Kind.UNION;
    }

    private TypeNode ref(Ttype type) {
        String refKind = switch (type.getKind()) {
            case UNION -> "union";
            case ENUM -> "enum";
            default -> "struct";
        };
        return TypeNode.ref(refKind, type.getName(), hrefBase + type.getName());
    }

    private TypeNode truncatedNode(Ttype type) {
        String name = type != null ? type.getName() : null;
        return new TypeNode("REF", name, null, null, null, null, null, true,
                null, null, null, null, null, null, null, null, null, null, name);
    }

    private TypeNode base(Ttype type) {
        return new TypeNode("BASE", type.getName(), type.getSizeof(),
                type.isIsConst(), type.isIsVolatile(), null, null, null,
                null, null, type.getName(), null, null, null, null, null, null, null, null);
    }

    private TypeNode pointer(Ttype type, boolean isRoot, Deque<Integer> expansionPath, int baseOffset) {
        Ttype target = requireType(type);
        expansionPath.push(type.getId());
        try {
            TypeNode targetNode = build(target, isRoot, expansionPath, baseOffset);
            return new TypeNode("POINTER", null, type.getSizeof(),
                    type.isIsConst(), type.isIsVolatile(), null, null, null,
                    null, null, null, targetNode, null, null, null, null, null, null, null);
        } finally {
            expansionPath.pop();
        }
    }

    private TypeNode array(Ttype type, Deque<Integer> expansionPath) {
        Tdata ref = firstField(type);
        Ttype element = closure.get(ref.getId());
        expansionPath.push(type.getId());
        try {
            TypeNode elementNode = element != null
                    ? build(element, false, expansionPath, 0)
                    : truncatedNode(type);
            return new TypeNode("ARRAY", null, type.getSizeof(),
                    type.isIsConst(), type.isIsVolatile(), null, null, null,
                    null, null, null, null, ref.getOffset(), elementNode,
                    null, null, null, null, null);
        } finally {
            expansionPath.pop();
        }
    }

    private TypeNode function(Ttype type, Deque<Integer> expansionPath) {
        TypeNode returnType = null;
        List<TypeNode> parameters = new ArrayList<>();
        expansionPath.push(type.getId());
        try {
            for (Tdata component : sortedFields(type)) {
                Ttype componentType = closure.get(component.getId());
                if (componentType == null) {
                    continue;
                }
                TypeNode node = build(componentType, false, expansionPath, 0);
                if ("return".equals(component.getName())) {
                    returnType = node;
                } else {
                    parameters.add(node);
                }
            }
            return new TypeNode("FUNCTION", type.getName(), type.getSizeof(),
                    type.isIsConst(), type.isIsVolatile(), anonIdFor(type), null, null,
                    null, null, null, null, null, null, returnType, parameters,
                    null, null, null);
        } finally {
            expansionPath.pop();
        }
    }

    private TypeNode struct(Ttype type, boolean isRoot, Deque<Integer> expansionPath, int baseOffset) {
        List<FieldNode> fields = new ArrayList<>();
        if (type.getData() != null && type.getSizeof() != 0) {
            expansionPath.push(type.getId());
            try {
                List<Tdata> remaining = TypeLoader.fieldsSortedByOrdinal(type);
                while (!remaining.isEmpty()) {
                    List<List<Tdata>> pieces = firstPieceOfStruct(remaining);
                    if (pieces.size() > 1) {
                        fields.add(unionPiece(pieces, expansionPath, baseOffset));
                    } else {
                        fields.add(fieldNode(pieces.get(0).get(0), expansionPath, baseOffset));
                    }
                }
            } finally {
                expansionPath.pop();
            }
        }
        return new TypeNode("STRUCT", type.getName(), type.getSizeof(),
                type.isIsConst(), type.isIsVolatile(), anonIdFor(type), null, null,
                null, null, null, null, null, null, null, null, fields, null, null);
    }

    private FieldNode unionPiece(List<List<Tdata>> pieces, Deque<Integer> expansionPath, int baseOffset) {
        List<FieldNode> unionFields = new ArrayList<>();
        for (List<Tdata> piece : pieces) {
            if (piece.size() > 1) {
                List<FieldNode> inner = new ArrayList<>();
                for (Tdata field : piece) {
                    inner.add(fieldNode(field, expansionPath, baseOffset));
                }
                unionFields.add(new FieldNode(null, piece.get(0).getOffset(), null,
                        new TypeNode("STRUCT", null, null, null, null,
                                nextAnonId(), null, null,
                                null, null, null, null, null, null, null, null,
                                inner, null, null)));
            } else {
                unionFields.add(fieldNode(piece.get(0), expansionPath, baseOffset));
            }
        }
        TypeNode unionNode = new TypeNode("UNION", null, null, null, null,
                nextAnonId(), null, null,
                null, null, null, null, null, null, null, null, unionFields, null, null);
        return new FieldNode(null, pieces.get(0).get(0).getOffset(), null, unionNode);
    }

    private TypeNode union(Ttype type, boolean isRoot, Deque<Integer> expansionPath, int baseOffset) {
        List<FieldNode> fields = new ArrayList<>();
        if (type.getData() != null && type.getSizeof() != 0) {
            expansionPath.push(type.getId());
            try {
                fields.addAll(unionFields(TypeLoader.fieldsSortedByOrdinal(type), expansionPath, baseOffset));
            } finally {
                expansionPath.pop();
            }
        }
        return new TypeNode("UNION", type.getName(), type.getSizeof(),
                type.isIsConst(), type.isIsVolatile(), anonIdFor(type), null, null,
                null, null, null, null, null, null, null, null, fields, null, null);
    }

    /**
     * Port of FieldBuilder.printUnionFields, quirks included: a run of
     * distinct-offset fields opens an implicit anonymous struct at the first
     * such field; the struct closes at the next offset change (that field
     * included inside) or at the last field (excluded, rendered plain).
     */
    private List<FieldNode> unionFields(List<Tdata> fields, Deque<Integer> expansionPath, int baseOffset) {
        List<FieldNode> result = new ArrayList<>();
        List<FieldNode> openRun = null;

        for (int i = 0; i < fields.size(); i++) {
            FieldNode node = fieldNode(fields.get(i), expansionPath, baseOffset);
            boolean last = i == fields.size() - 1;
            boolean sameAsNext = !last && fields.get(i).getOffset().equals(fields.get(i + 1).getOffset());

            if (last) {
                if (openRun != null) {
                    result.add(closeStruct(openRun, openRun.get(openRun.size() - 1).offset()));
                    openRun = null;
                }
                result.add(node);
            } else if (sameAsNext) {
                result.add(node);
            } else if (openRun == null) {
                openRun = new ArrayList<>(List.of(node));
            } else {
                openRun.add(node);
                result.add(closeStruct(openRun, openRun.get(0).offset()));
                openRun = null;
            }
        }
        return result;
    }

    private FieldNode closeStruct(List<FieldNode> inner, Integer offset) {
        TypeNode structNode = new TypeNode("STRUCT", null, null, null, null,
                nextAnonId(), null, null,
                null, null, null, null, null, null, null, null, inner, null, null);
        return new FieldNode(null, offset, null, structNode);
    }

    private FieldNode fieldNode(Tdata field, Deque<Integer> expansionPath, int baseOffset) {
        String name = field.getName();
        Integer bitWidth = null;
        if (name != null && name.contains(":")) {
            int colon = name.indexOf(':');
            bitWidth = Integer.parseInt(name.substring(colon + 1).trim());
            name = name.substring(0, colon).trim();
        }
        int displayOffset = baseOffset + field.getOffset();
        Ttype fieldType = closure.get(field.getId());
        TypeNode typeNode = fieldType != null
                ? build(fieldType, false, expansionPath, displayOffset)
                : truncatedNode(null);
        return new FieldNode(name, displayOffset, bitWidth, typeNode);
    }

    private TypeNode enumNode(Ttype type, boolean isRoot) {
        List<EnumConstant> constants = new ArrayList<>();
        if (type.getData() != null && type.getSizeof() != 0) {
            for (Tdata constant : sortedFields(type)) {
                constants.add(new EnumConstant(constant.getName(), constant.getOffset()));
            }
        }
        return new TypeNode("ENUM", type.getName(), type.getSizeof(),
                type.isIsConst(), type.isIsVolatile(), anonIdFor(type), null, null,
                null, null, null, null, null, null, null, null, null, constants, null);
    }

    private String anonIdFor(Ttype type) {
        return type.getName() == null ? nextAnonId() : null;
    }

    private String nextAnonId() {
        return "anon:" + (anonCounter++);
    }

    private Ttype requireType(Ttype pointerType) {
        Tdata ref = firstField(pointerType);
        Ttype target = closure.get(ref.getId());
        return target != null ? target : pointerType;
    }

    private static Tdata firstField(Ttype type) {
        return TypeLoader.fieldsSortedByOrdinal(type).getFirst();
    }

    private static List<Tdata> sortedFields(Ttype type) {
        return TypeLoader.fieldsSortedByOrdinal(type);
    }

    /**
     * Port of FieldBuilder.getFirstPieceOfStruct (on a defensive copy):
     * detects the union at the first field's offset and splits its pieces.
     * The input list is consumed (matching FieldBuilder's remove-all behavior).
     */
    private List<List<Tdata>> firstPieceOfStruct(List<Tdata> structFields) {
        List<List<Tdata>> retLists = new ArrayList<>();
        Tdata currentField = structFields.get(0);

        List<Integer> dupl = findDuplicates(structFields, currentField.getOffset());
        if (!dupl.isEmpty()) {
            for (int j = 0; j < dupl.size() - 1; j++) {
                List<Tdata> piece = new ArrayList<>();
                for (int n = dupl.get(j); n < dupl.get(j + 1); n++) {
                    piece.add(structFields.get(n));
                }
                retLists.add(piece);
            }

            int sizeOfUnion = sizeOfUnion(structFields, dupl);
            int maxPossibleOffset = sizeOfUnion - 1;

            int bottomBorder = dupl.get(dupl.size() - 1);
            for (int k = bottomBorder + 1; k < structFields.size(); k++) {
                if (structFields.get(k).getOffset() > maxPossibleOffset) {
                    bottomBorder = k - 1;
                    break;
                }
                bottomBorder++;
            }

            List<Tdata> piece = new ArrayList<>();
            for (int n = dupl.get(dupl.size() - 1); n < bottomBorder + 1; n++) {
                piece.add(structFields.get(n));
            }
            retLists.add(piece);
        } else {
            retLists.add(new ArrayList<>(List.of(structFields.get(0))));
        }

        Set<Tdata> consumed = new HashSet<>();
        retLists.forEach(consumed::addAll);
        structFields.removeIf(consumed::contains);
        return retLists;
    }

    private static List<Integer> findDuplicates(List<Tdata> list, int val) {
        List<Integer> dupl = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getOffset() == val) {
                dupl.add(i);
                if (isBitField(list.get(i).getName())) {
                    while (i + 1 < list.size() && isBitField(list.get(i + 1).getName())) {
                        i++;
                    }
                }
            }
        }
        if (dupl.size() <= 1) {
            dupl.clear();
        }
        return dupl;
    }

    private int sizeOfUnion(List<Tdata> structFields, List<Integer> dupls) {
        int[] sizes = new int[dupls.size() - 1];
        int k = 0;
        for (int i = dupls.size() - 1; i > 0; i--) {
            Tdata prev = structFields.get(dupls.get(i) - 1);
            Ttype prevType = closure.get(prev.getId());
            int sizeof = prevType != null && prevType.getSizeof() != null
                    ? prevType.getSizeof() : 0;
            sizes[k++] = sizeof + prev.getOffset();
        }
        Arrays.sort(sizes);
        return sizes[sizes.length - 1];
    }

    private static boolean isBitField(String fieldName) {
        return fieldName != null && fieldName.contains(":");
    }
}
