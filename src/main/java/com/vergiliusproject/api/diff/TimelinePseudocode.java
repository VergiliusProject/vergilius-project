package com.vergiliusproject.api.diff;

import com.vergiliusproject.api.diff.TimelineDiffer.FieldHistory;
import com.vergiliusproject.api.diff.TimelineDiffer.OffsetPoint;
import com.vergiliusproject.api.diff.TimelineDiffer.Timeline;
import com.vergiliusproject.api.diff.TimelineDiffer.Transition;
import com.vergiliusproject.api.diff.TypeDiffer.Change;
import com.vergiliusproject.api.diff.TypeDiffer.ChangeKind;
import java.util.List;

/**
 * Renders a timeline as pseudocode: a chronological change log (one block per
 * transition), a multi-branch build dispatch for fields whose offset varies
 * across the range, and single-accessor lines for stable fields.
 */
public final class TimelinePseudocode {

    private TimelinePseudocode() {}

    public static String generate(String typeName, List<String> buildLabels, Timeline timeline) {
        StringBuilder out = new StringBuilder();
        out.append("// ").append(typeName).append(" evolution across ")
                .append(buildLabels.size()).append(" builds\n");
        out.append("//   ").append(buildLabels.getFirst()).append(" -> ")
                .append(buildLabels.getLast()).append("\n");

        changelog(out, timeline);
        if (timeline.hasMovement()) {
            dispatch(out, buildLabels, timeline);
        }
        stableFields(out, timeline);
        return out.toString();
    }

    private static void changelog(StringBuilder out, Timeline timeline) {
        for (Transition t : timeline.transitions()) {
            out.append("\n// In ").append(lastSlugSegment(t.to())).append(":\n");
            for (Change c : t.changes()) {
                out.append("//   ").append(switch (c.kind()) {
                    case ADDED -> "+ " + c.path() + " @0x"
                            + Integer.toHexString(c.toOffset() == null ? 0 : c.toOffset());
                    case REMOVED -> "- " + c.path() + " (was @0x"
                            + Integer.toHexString(c.fromOffset() == null ? 0 : c.fromOffset()) + ")";
                    case MOVED -> "~ " + c.path() + " moved " + fmtDelta(c.offsetDelta())
                            + " (now @0x" + Integer.toHexString(c.toOffset()) + ")";
                    case BITFIELD_CHANGED -> "~ " + c.path() + " bitfield "
                            + c.fromType() + " -> " + c.toType();
                    case VALUE_CHANGED -> "~ " + c.path() + " " + c.fromType() + " -> " + c.toType();
                    case TYPE_CHANGED -> "~ " + c.path() + ": " + c.fromType() + " -> " + c.toType();
                });
                if (c.kind() == ChangeKind.TYPE_CHANGED && c.detail() != null) {
                    out.append(" (").append(c.detail()).append(")");
                }
                out.append("\n");
            }
        }
        if (timeline.transitions().isEmpty()) {
            out.append("\n// No differences across the entire range.\n");
        }
    }

    private static void dispatch(StringBuilder out, List<String> buildLabels, Timeline timeline) {
        out.append("\n");
        List<FieldHistory> movers = timeline.fieldHistories().stream()
                .filter(h -> h.points().size() > 1 && h.removedIn() == null)
                .toList();

        for (String label : buildLabels.reversed()) {
            String header = buildLabels.getLast().equals(label)
                    ? "if (build == " + label + ") {"
                    : "else if (build == " + label + ") {";
            StringBuilder body = new StringBuilder();
            for (FieldHistory m : movers) {
                int offset = offsetAtOrBefore(m.points(), label, buildLabels);
                if (offset >= 0 && (m.addedIn() == null || !isAfter(m.addedIn(), label, buildLabels))) {
                    body.append("    ").append(accessor(m.path(), offset)).append("\n");
                }
            }
            if (body.isEmpty()) {
                continue;
            }
            out.append(header).append("\n").append(body).append("}\n");
        }
    }

    private static void stableFields(StringBuilder out, Timeline timeline) {
        List<FieldHistory> stable = timeline.fieldHistories().stream()
                .filter(h -> h.points().size() == 1 && h.removedIn() == null)
                .toList();
        if (stable.isEmpty()) {
            return;
        }
        out.append("\n// Stable across all builds:\n");
        for (FieldHistory h : stable) {
            OffsetPoint p = h.points().getFirst();
            out.append("//   ").append(accessorComment(h.path(), p.offset()));
            if (h.addedIn() != null) {
                out.append("   // added in ").append(lastSlugSegment(h.addedIn()));
            }
            out.append("\n");
        }
    }

    private static String accessor(String path, int offset) {
        String last = lastSegment(path);
        String var = Character.toLowerCase(last.charAt(0)) + last.substring(1);
        return var + " = *(typeof(" + last + ")*)(base + 0x"
                + Integer.toHexString(offset) + ");";
    }

    private static String accessorComment(String path, int offset) {
        String last = lastSegment(path);
        return last + " @0x" + Integer.toHexString(offset);
    }

    /** Offset in effect at `label`: last point whose build is at or before it. */
    private static int offsetAtOrBefore(List<OffsetPoint> points, String label,
            List<String> buildLabels) {
        int best = -1;
        for (OffsetPoint p : points) {
            if (!isAfter(p.build(), label, buildLabels)) {
                best = p.offset();
            }
        }
        return best;
    }

    private static boolean isAfter(String a, String b, List<String> buildLabels) {
        return buildLabels.indexOf(a) > buildLabels.indexOf(b);
    }

    private static String fmtDelta(Integer delta) {
        return (delta >= 0 ? "+" : "") + delta;
    }

    private static String lastSegment(String path) {
        String cleaned = path.replace("*", "").replace(".", "");
        int lt = cleaned.lastIndexOf('<');
        String tail = lt >= 0 ? cleaned.substring(lt + 1).replace(">", "") : cleaned;
        return tail.isEmpty() ? "field" : tail;
    }

    private static String lastSlugSegment(String label) {
        String[] parts = label.split("/");
        String tail = parts[parts.length - 1];
        int paren = tail.indexOf(" (");
        return paren >= 0 ? tail.substring(0, paren) : tail;
    }
}
