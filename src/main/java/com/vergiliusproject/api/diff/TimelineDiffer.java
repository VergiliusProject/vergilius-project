package com.vergiliusproject.api.diff;

import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.api.diff.TypeDiffer.Change;
import com.vergiliusproject.api.diff.TypeDiffer.ChangeKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates consecutive pairwise diffs across an ordered build list into a
 * timeline: one entry per transition, per-field offset histories (stable runs
 * between change points), presence intervals for added/removed fields, and a
 * flag for whether any field moves at all (for the dispatch generation).
 */
public final class TimelineDiffer {

    public record BuildEntry(String label, TypeNode tree, boolean present) {}

    public record Transition(String from, String to, List<Change> changes) {}

    public record OffsetPoint(String build, int offset) {}

    public record FieldHistory(
            String path,
            List<OffsetPoint> points,
            String addedIn,
            String removedIn,
            List<Transition> typeChanges) {}

    public record Timeline(
            List<Transition> transitions,
            List<FieldHistory> fieldHistories) {

        public boolean hasMovement() {
            return fieldHistories.stream().anyMatch(h -> h.points().size() > 1);
        }
    }

    private TimelineDiffer() {}

    public static Timeline timeline(List<BuildEntry> entries) {
        List<Transition> transitions = new ArrayList<>();
        Map<String, List<OffsetPoint>> offsets = new LinkedHashMap<>();
        Map<String, String> addedIn = new LinkedHashMap<>();
        Map<String, String> removedIn = new LinkedHashMap<>();
        Map<String, List<Transition>> typeChanges = new LinkedHashMap<>();

        // seed histories from the first build where the type exists
        for (BuildEntry entry : entries) {
            if (entry.present()) {
                collectOffsets(entry.tree(), "", entry.label(), offsets, addedIn);
                break;
            }
        }

        for (int i = 0; i + 1 < entries.size(); i++) {
            BuildEntry from = entries.get(i);
            BuildEntry to = entries.get(i + 1);

            List<Change> changes = TypeDiffer.diff(from.tree(), to.tree());
            if (!changes.isEmpty()) {
                transitions.add(new Transition(from.label(), to.label(), changes));
            }

            for (Change c : changes) {
                switch (c.kind()) {
                    case ADDED -> {
                        addedIn.put(c.path(), to.label());
                        removedIn.remove(c.path());
                        if (c.toOffset() != null) {
                            offsets.computeIfAbsent(c.path(), k -> new ArrayList<>())
                                    .add(new OffsetPoint(to.label(), c.toOffset()));
                        }
                    }
                    case REMOVED -> {
                        removedIn.put(c.path(), to.label());
                        addedIn.remove(c.path());
                    }
                    case MOVED -> offsets.computeIfAbsent(c.path(), k -> new ArrayList<>())
                            .add(new OffsetPoint(to.label(), c.toOffset()));
                    default -> typeChanges.computeIfAbsent(c.path(), k -> new ArrayList<>())
                            .add(new Transition(from.label(), to.label(), List.of(c)));
                }
            }
        }

        List<FieldHistory> histories = new ArrayList<>();
        offsets.forEach((path, points) -> histories.add(new FieldHistory(
                path, collapse(points),
                addedIn.get(path),
                removedIn.get(path),
                typeChanges.getOrDefault(path, List.of()))));
        histories.sort(Comparator.comparing(FieldHistory::path));

        return new Timeline(transitions, histories);
    }

    /** Drops consecutive duplicate offsets, keeping the first build of each run. */
    private static List<OffsetPoint> collapse(List<OffsetPoint> points) {
        List<OffsetPoint> out = new ArrayList<>();
        for (OffsetPoint p : points) {
            if (out.isEmpty() || out.get(out.size() - 1).offset() != p.offset()) {
                out.add(p);
            }
        }
        return out;
    }

    private static void collectOffsets(TypeNode node, String path, String build,
            Map<String, List<OffsetPoint>> offsets, Map<String, String> addedIn) {
        if (node == null || node.fields() == null) {
            return;
        }
        for (var f : node.fields()) {
            String fieldPath = path + (f.name() != null ? f.name() : "<anon>");
            if (f.offset() != null) {
                offsets.computeIfAbsent(fieldPath, k -> new ArrayList<>())
                        .add(new OffsetPoint(build, f.offset()));
                addedIn.put(fieldPath, build);
            }
            TypeNode t = f.type();
            if (t != null && t.name() == null && t.fields() != null) {
                collectOffsets(t, fieldPath + ".", build, offsets, addedIn);
            }
        }
    }
}
