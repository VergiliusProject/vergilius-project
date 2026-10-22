package com.vergiliusproject.api;

import com.vergiliusproject.FieldBuilder;
import com.vergiliusproject.api.ApiDtos.EnumConstant;
import com.vergiliusproject.api.ApiDtos.FieldNode;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import java.util.List;
import java.util.Locale;

/**
 * Renders a TypeTreeBuilder tree back to FieldBuilder's exact C declaration
 * format (indentation, 75-column offset comments, modifier and trailing-space
 * quirks included) so walker/FieldBuilder parity is provable byte-for-byte.
 */
public final class CDeclRenderer {

    private CDeclRenderer() {}

    public static String render(TypeNode root) {
        StringBuilder sb = new StringBuilder();
        if (isAggregate(root)) {
            sb.append("//0x").append(Integer.toHexString(root.size() == null ? 0 : root.size()))
                    .append(" bytes (sizeof)\n");
        }
        sb.append(rootBody(root));
        // FieldBuilder.toString() appends " " + name even at top level, where
        // name is empty: every golden ends with a trailing space.
        sb.append(" ");
        return sb.toString();
    }

    private static boolean isAggregate(TypeNode n) {
        return "STRUCT".equals(n.kind()) || "UNION".equals(n.kind()) || "ENUM".equals(n.kind());
    }

    /** The declaration of a type reference, e.g. "struct LIST_ENTRY" or "ULONG*". */
    public static String declaration(TypeNode node) {
        return switch (node.kind()) {
            case "BASE" -> baseDecl(node);
            case "REF" -> node.base() + " " + node.name();
            case "POINTER" -> pointerDecl(node);
            case "ARRAY" -> declaration(node.element());
            default -> "";
        };
    }

    private static String modifier(TypeNode node) {
        // FieldBuilder's getModifier quirk: const is never rendered
        return Boolean.TRUE.equals(node.isVolatile()) ? "volatile" : "";
    }

    private static String baseDecl(TypeNode node) {
        String mod = modifier(node);
        return mod.isEmpty() ? node.base() : mod + " " + node.base();
    }

    private static String pointerDecl(TypeNode node) {
        String mod = modifier(node);
        return declaration(node.target()) + "*" + (mod.isEmpty() ? "" : " " + mod);
    }

    private static String rootBody(TypeNode root) {
        StringBuilder sb = new StringBuilder();
        sb.append(keywordOf(root.kind())).append(" ").append(root.name());
        if ((root.fields() != null || root.constants() != null)
                && (root.size() == null || root.size() != 0)) {
            sb.append("\n{");
            if (root.fields() != null) {
                renderFields(sb, root.fields(), 1);
            }
            if (root.constants() != null) {
                renderConstants(sb, root.constants());
            }
            sb.append("\n}");
        }
        sb.append(";");
        return sb.toString();
    }

    private static void renderConstants(StringBuilder sb, List<EnumConstant> constants) {
        for (int i = 0; i < constants.size(); i++) {
            sb.append("\n    ").append(constants.get(i).name())
                    .append(" = ").append(constants.get(i).value());
            if (i != constants.size() - 1) {
                sb.append(",");
            }
        }
    }

    public static void renderFields(StringBuilder sb, List<FieldNode> fields, int indent) {
        for (FieldNode field : fields) {
            renderField(sb, field, indent);
        }
    }

    private static void renderField(StringBuilder sb, FieldNode field, int indent) {
        TypeNode type = field.type();

        if (type.name() == null && ("UNION".equals(type.kind()) || "STRUCT".equals(type.kind()))) {
            renderInlineAggregate(sb, type, indent);
            return;
        }

        if ("POINTER".equals(type.kind()) && type.target() != null
                && type.target().name() == null
                && ("STRUCT".equals(type.target().kind()) || "UNION".equals(type.target().kind()))) {
            renderPointerToInlineAggregate(sb, field, type.target(), indent);
            return;
        }

        String decl;
        if ("FUNCTION".equals(type.kind())) {
            decl = functionDecl(type, field.name());
        } else if ("ARRAY".equals(type.kind())) {
            decl = declaration(type.element()) + " " + field.name() + "[" + type.count() + "]";
        } else if (field.bitWidth() != null) {
            decl = declaration(type) + " " + field.name() + ":" + field.bitWidth();
        } else {
            decl = declaration(type) + " " + field.name();
        }

        String line = "\n" + FieldBuilder.retIndent(indent) + decl + ";";
        sb.append(line);
        appendOffsetComment(sb, line, field.offset());
    }

    private static String functionDecl(TypeNode type, String fieldName) {
        // FieldBuilder's retval carries toString()'s trailing space quirk,
        // producing e.g. "VOID (Cb)(...)" rather than "VOID(Cb)(...)"
        String retval = type.returnType() != null ? declaration(type.returnType()) + " " : "";
        StringBuilder args = new StringBuilder();
        List<TypeNode> params = type.parameters();
        for (int i = 0; i < params.size(); i++) {
            args.append(declaration(params.get(i))).append(" arg").append(i + 1);
            if (i != params.size() - 1) {
                args.append(", ");
            }
        }
        return retval + "(" + fieldName + ")" + "(" + args + ")";
    }

    private static void renderInlineAggregate(StringBuilder sb, TypeNode type, int indent) {
        sb.append("\n").append(FieldBuilder.retIndent(indent)).append(keywordOf(type.kind()));
        sb.append("\n").append(FieldBuilder.retIndent(indent)).append("{");
        renderFields(sb, type.fields(), indent + 1);
        sb.append("\n").append(FieldBuilder.retIndent(indent)).append("};");
    }

    private static void renderPointerToInlineAggregate(
            StringBuilder sb, FieldNode field, TypeNode target, int indent) {
        sb.append("\n").append(FieldBuilder.retIndent(indent)).append(keywordOf(target.kind()));
        sb.append("\n").append(FieldBuilder.retIndent(indent)).append("{");
        renderFields(sb, target.fields(), indent + 1);
        String line = "\n" + FieldBuilder.retIndent(indent) + "}* " + field.name() + ";";
        sb.append(line);
        appendOffsetComment(sb, line, field.offset());
    }

    private static void appendOffsetComment(StringBuilder sb, String line, Integer offset) {
        String[] splitted = line.split("\n");
        int displayLength = splitted[splitted.length - 1].length();
        sb.append(FieldBuilder.retSpaces(displayLength)).append(Integer.toHexString(offset));
    }

    private static String keywordOf(String kind) {
        return switch (kind) {
            case "STRUCT" -> "struct";
            case "UNION" -> "union";
            case "ENUM" -> "enum";
            default -> kind.toLowerCase(Locale.US);
        };
    }
}
