package com.vergiliusproject.api.diff;

import com.vergiliusproject.api.diff.TypeDiffer.Change;
import com.vergiliusproject.api.diff.TypeDiffer.ChangeKind;
import java.util.List;

/**
 * Turns a TypeDiffer change list into readable logic/pseudocode: per-build
 * accessor expressions for moved fields, insertion explanations derived from
 * offset shifts, and added/removed field documentation. Pure text; no
 * language-specific claims.
 */
public final class DiffPseudocode {

    private DiffPseudocode() {}

    public record Context(String fromBuild, String toBuild, String typeName,
                          Integer fromSize, Integer toSize) {}

    public static String generate(Context ctx, List<Change> changes) {
        StringBuilder out = new StringBuilder();

        header(out, ctx, changes);
        if (changes.isEmpty()) {
            out.append("\n// No differences between builds.\n");
            return out.toString();
        }

        List<Change> moved = changes.stream().filter(c -> c.kind() == ChangeKind.MOVED).toList();
        List<Change> added = changes.stream().filter(c -> c.kind() == ChangeKind.ADDED).toList();
        List<Change> removed = changes.stream().filter(c -> c.kind() == ChangeKind.REMOVED).toList();
        List<Change> typed = changes.stream()
                .filter(c -> c.kind() == ChangeKind.TYPE_CHANGED
                        || c.kind() == ChangeKind.BITFIELD_CHANGED
                        || c.kind() == ChangeKind.VALUE_CHANGED)
                .toList();

        insertionAnalysis(out, ctx, added, moved);
        branch(out, ctx, moved, typed);
        notes(out, added, removed, typed);
        return out.toString();
    }

    private static void header(StringBuilder out, Context ctx, List<Change> changes) {
        out.append("// ").append(ctx.typeName())
                .append(": layout comparison\n");
        out.append("//   from: ").append(ctx.fromBuild());
        if (ctx.fromSize() != null) {
            out.append(" (sizeof 0x").append(Integer.toHexString(ctx.fromSize())).append(")");
        }
        out.append("\n//   to:   ").append(ctx.toBuild());
        if (ctx.toSize() != null) {
            out.append(" (sizeof 0x").append(Integer.toHexString(ctx.toSize())).append(")");
        }
        out.append("\n");
        if (ctx.fromSize() != null && ctx.toSize() != null && !ctx.fromSize().equals(ctx.toSize())) {
            int delta = ctx.toSize() - ctx.fromSize();
            out.append("// sizeof changed by ").append(delta >= 0 ? "+" : "").append(delta)
                    .append(" bytes (0x").append(Integer.toHexString(ctx.fromSize())).append(" -> 0x")
                    .append(Integer.toHexString(ctx.toSize())).append(")\n");
        }
    }

    private static void insertionAnalysis(StringBuilder out, Context ctx,
            List<Change> added, List<Change> moved) {
        if (added.isEmpty() && moved.isEmpty()) {
            return;
        }
        // Heuristic: a contiguous shift of all fields at/after some offset,
        // matching an inserted field at that offset, reads as an insertion.
        for (Change add : added) {
            if (add.toOffset() == null) {
                continue;
            }
            int insertAt = add.toOffset();
            List<Change> shifted = moved.stream()
                    .filter(m -> m.fromOffset() != null && m.fromOffset() >= insertAt)
                    .toList();
            if (!shifted.isEmpty()) {
                Integer uniformDelta = shifted.stream().map(Change::offsetDelta)
                        .reduce(null, (a, b) -> a == null ? b : a.equals(b) ? a : null);
                String insertedType = add.toType() == null ? "" : add.toType() + " ";
                out.append("\n// A new field (").append(insertedType).append(") was inserted at 0x")
                        .append(Integer.toHexString(insertAt))
                        .append("; every field at or after 0x")
                        .append(Integer.toHexString(insertAt)).append(" shifted");
                if (uniformDelta != null) {
                    out.append(" uniformly by ").append(uniformDelta >= 0 ? "+" : "")
                            .append(uniformDelta).append(" bytes");
                }
                out.append(".\n");
            }
        }
    }

    private static void branch(StringBuilder out, Context ctx,
            List<Change> moved, List<Change> typed) {
        if (moved.isEmpty() && typed.isEmpty()) {
            return;
        }
        out.append("\nif (build == ").append(ctx.toBuild()).append(") {\n");
        for (Change c : typed) {
            if (c.kind() == ChangeKind.TYPE_CHANGED && "whole type removed".equals(c.detail())) {
                continue;
            }
            out.append("    // ").append(c.path()).append(": ")
                    .append(c.fromType() == null ? "?" : c.fromType()).append(" -> ")
                    .append(c.toType() == null ? "?" : c.toType()).append("\n");
        }
        for (Change c : moved) {
            String last = lastSegment(c.path());
            out.append("    ").append(camel(last)).append(" = *(typeof(")
                    .append(last).append(")*)(base + 0x")
                    .append(Integer.toHexString(c.toOffset())).append(");\n");
        }
        out.append("} else if (build == ").append(ctx.fromBuild()).append(" {\n");
        for (Change c : moved) {
            String last = lastSegment(c.path());
            out.append("    ").append(camel(last)).append(" = *(typeof(")
                    .append(last).append(")*)(base + 0x")
                    .append(Integer.toHexString(c.fromOffset())).append(");\n");
        }
        out.append("}\n");
    }

    private static void notes(StringBuilder out, List<Change> added,
            List<Change> removed, List<Change> typed) {
        if (!added.isEmpty()) {
            out.append("\n// Added:\n");
            for (Change c : added) {
                out.append("//   + ").append(c.path());
                if (c.toOffset() != null) {
                    out.append(" @ 0x").append(Integer.toHexString(c.toOffset()));
                }
                if (c.toType() != null) {
                    out.append(" (").append(c.toType()).append(")");
                }
                out.append("\n");
            }
        }
        if (!removed.isEmpty()) {
            out.append("\n// Removed:\n");
            for (Change c : removed) {
                out.append("//   - ").append(c.path());
                if (c.fromOffset() != null) {
                    out.append(" @ 0x").append(Integer.toHexString(c.fromOffset()));
                }
                if (c.fromType() != null) {
                    out.append(" (").append(c.fromType()).append(")");
                }
                out.append("\n");
            }
        }
        if (!typed.isEmpty()) {
            out.append("\n// Type/value changes:\n");
            for (Change c : typed) {
                out.append("//   ~ ").append(c.path()).append(": ")
                        .append(c.fromType() == null ? "?" : c.fromType()).append(" -> ")
                        .append(c.toType() == null ? "?" : c.toType());
                if (c.detail() != null) {
                    out.append(" (").append(c.detail()).append(")");
                }
                out.append("\n");
            }
        }
    }

    private static String lastSegment(String path) {
        String cleaned = path.replace("*", "").replace(".", "");
        int slash = cleaned.lastIndexOf('<');
        String tail = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        return tail.isEmpty() ? "field" : tail;
    }

    private static String camel(String name) {
        String cleaned = name.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.isEmpty()) {
            return "field";
        }
        return Character.toLowerCase(cleaned.charAt(0)) + cleaned.substring(1);
    }
}
