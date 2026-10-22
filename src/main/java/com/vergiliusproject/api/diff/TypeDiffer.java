package com.vergiliusproject.api.diff;

import com.vergiliusproject.api.ApiDtos.EnumConstant;
import com.vergiliusproject.api.ApiDtos.FieldNode;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.api.CDeclRenderer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structural diff between two TypeNode trees of the same type name across
 * builds. Fields are matched by name (anonymous members by kind+position),
 * then compared on offset, bit width, and declared type; inline aggregates
 * and enums are recursed into. Produces a flat, ordered list of changes.
 */
public final class TypeDiffer {

    public enum ChangeKind {
        ADDED, REMOVED, MOVED, TYPE_CHANGED, BITFIELD_CHANGED, VALUE_CHANGED
    }

    public record Change(
            String path,
            ChangeKind kind,
            Integer fromOffset,
            Integer toOffset,
            Integer offsetDelta,
            String fromType,
            String toType,
            String detail) {

        static Change added(String path, FieldNode field) {
            return new Change(path, ChangeKind.ADDED, null, field.offset(), null,
                    null, decl(field.type()), null);
        }

        static Change removed(String path, FieldNode field) {
            return new Change(path, ChangeKind.REMOVED, field.offset(), null, null,
                    decl(field.type()), null, null);
        }
    }

    private TypeDiffer() {}

    public static List<Change> diff(TypeNode from, TypeNode to) {
        List<Change> changes = new ArrayList<>();
        if (from == null && to == null) {
            return changes;
        }
        if (from == null || to == null || !from.kind().equals(to.kind())) {
            String fromDecl = from == null ? null : CDeclRenderer.declaration(from);
            String toDecl = to == null ? null : CDeclRenderer.declaration(to);
            changes.add(new Change("", ChangeKind.TYPE_CHANGED, null, null, null,
                    fromDecl, toDecl, "whole type " + (from == null ? "added" : "removed")));
            return changes;
        }
        walk(from, to, "", changes);
        return changes;
    }

    private static void walk(TypeNode from, TypeNode to, String path, List<Change> out) {
        switch (to.kind()) {
            case "STRUCT", "UNION" -> diffFields(from.fields(), to.fields(), path, out);
            case "ENUM" -> diffConstants(from.constants(), to.constants(), path, out);
            case "POINTER" -> walk(from.target(), to.target(), path + "*", out);
            case "ARRAY" -> {
                if (from.count() != null && to.count() != null && !from.count().equals(to.count())) {
                    out.add(new Change(path, ChangeKind.TYPE_CHANGED, null, null, null,
                            "[" + from.count() + "]", "[" + to.count() + "]",
                            "array count " + from.count() + " -> " + to.count()));
                }
                walk(from.element(), to.element(), path, out);
            }
            default -> {
                // BASE / REF / FUNCTION: compare declarations at the leaf level
                String fromDecl = CDeclRenderer.declaration(from);
                String toDecl = CDeclRenderer.declaration(to);
                if (!fromDecl.equals(toDecl)) {
                    out.add(new Change(path, ChangeKind.TYPE_CHANGED, null, null, null,
                            fromDecl, toDecl, null));
                }
            }
        }
    }

    private static void diffFields(List<FieldNode> from, List<FieldNode> to,
            String path, List<Change> out) {
        Map<String, FieldNode> toByName = new LinkedHashMap<>();
        for (FieldNode f : to) {
            if (f.name() != null) {
                toByName.put(f.name(), f);
            }
        }
        Map<Integer, FieldNode> toAnon = new LinkedHashMap<>();
        int anonIdx = 0;
        for (FieldNode f : to) {
            if (f.name() == null) {
                toAnon.put(anonIdx++, f);
            }
        }

        int anonPos = 0;
        for (FieldNode fromField : from) {
            String fieldPath = path + (fromField.name() != null
                    ? fromField.name() : ("<anon#" + (anonPos) + ">"));

            FieldNode toField = fromField.name() != null
                    ? toByName.get(fromField.name())
                    : toAnon.get(anonPos);
            anonPos++;

            if (toField == null) {
                out.add(Change.removed(fieldPath, fromField));
                continue;
            }
            compareMatched(fieldPath, fromField, toField, out);
        }

        // fields present only in `to` (skip ones matched above)
        java.util.Set<String> fromNames = new java.util.HashSet<>();
        for (FieldNode f : from) {
            if (f.name() != null) {
                fromNames.add(f.name());
            }
        }
        for (FieldNode toField : to) {
            if (toField.name() != null && !fromNames.contains(toField.name())) {
                out.add(Change.added(path + toField.name(), toField));
            }
        }
    }

    private static void compareMatched(String fieldPath, FieldNode fromField,
            FieldNode toField, List<Change> out) {
        Integer delta = toField.offset() - fromField.offset();
        if (delta != 0) {
            out.add(new Change(fieldPath, ChangeKind.MOVED,
                    fromField.offset(), toField.offset(), delta, null, null, null));
        }
        if (!nullSafeEquals(fromField.bitWidth(), toField.bitWidth())) {
            out.add(new Change(fieldPath, ChangeKind.BITFIELD_CHANGED, null, null, null,
                    fromField.bitWidth() == null ? null : "width " + fromField.bitWidth(),
                    toField.bitWidth() == null ? null : "width " + toField.bitWidth(), null));
        }

        TypeNode fromType = fromField.type();
        TypeNode toType = toField.type();

        // anonymous wrapper nodes (inline aggregates, arrays, pointers,
        // functions, inline enums) are compared structurally, not by decl
        boolean bothAnonymous = fromType.name() == null && toType.name() == null
                && fromType.kind().equals(toType.kind())
                && List.of("STRUCT", "UNION", "ARRAY", "POINTER", "FUNCTION", "ENUM")
                        .contains(fromType.kind());

        if (bothAnonymous) {
            walk(fromType, toType, fieldPath + ".", out);
            return;
        }

        String fromDecl = CDeclRenderer.declaration(fromType);
        String toDecl = CDeclRenderer.declaration(toType);
        if (!fromDecl.equals(toDecl)) {
            out.add(new Change(fieldPath, ChangeKind.TYPE_CHANGED, null, null, null,
                    fromDecl, toDecl, null));
        }
    }

    private static void diffConstants(List<EnumConstant> from, List<EnumConstant> to,
            String path, List<Change> out) {
        Map<String, Long> toValues = new LinkedHashMap<>();
        for (EnumConstant c : to) {
            toValues.put(c.name(), c.value());
        }
        for (EnumConstant c : from) {
            Long toValue = toValues.get(c.name());
            if (toValue == null) {
                out.add(new Change(path + c.name(), ChangeKind.REMOVED, null, null, null,
                        "value " + c.value(), null, null));
            } else if (toValue != c.value()) {
                out.add(new Change(path + c.name(), ChangeKind.VALUE_CHANGED, null, null, null,
                        "value " + c.value(), "value " + toValue, null));
            }
        }
        java.util.Set<String> fromNames = new java.util.HashSet<>();
        for (EnumConstant c : from) {
            fromNames.add(c.name());
        }
        for (EnumConstant c : to) {
            if (!fromNames.contains(c.name())) {
                out.add(new Change(path + c.name(), ChangeKind.ADDED, null, null, null,
                        null, "value " + c.value(), null));
            }
        }
    }

    private static boolean nullSafeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    static String decl(TypeNode node) {
        try {
            return CDeclRenderer.declaration(node);
        } catch (RuntimeException e) {
            return node.kind();
        }
    }
}
