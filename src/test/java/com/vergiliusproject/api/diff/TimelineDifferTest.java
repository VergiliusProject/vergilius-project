package com.vergiliusproject.api.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.vergiliusproject.api.ApiDtos.FieldNode;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.api.diff.TimelineDiffer.BuildEntry;
import com.vergiliusproject.api.diff.TimelineDiffer.FieldHistory;
import com.vergiliusproject.api.diff.TimelineDiffer.Timeline;
import com.vergiliusproject.api.diff.TypeDiffer.ChangeKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineDifferTest {

    private static TypeNode base(String name) {
        return new TypeNode("BASE", name, 4, false, false, null, null, null,
                null, null, name, null, null, null, null, null, null, null, null);
    }

    private static TypeNode struct(String name, Integer size, FieldNode... fields) {
        return new TypeNode("STRUCT", name, size, false, false, null, null, null,
                null, null, null, null, null, null, null, null, List.of(fields), null, null);
    }

    private static FieldNode field(String name, Integer offset) {
        return new FieldNode(name, offset, null, base("ULONG"));
    }

    private static TypeNode v1() {
        return struct("S", 8, field("A", 0), field("B", 4));
    }

    private static TypeNode v2() {
        return struct("S", 12, field("A", 0), field("NEW", 4), field("B", 8));
    }

    private static TypeNode v3() {
        return struct("S", 12, field("A", 0), field("NEW", 4), field("B", 8),
                new FieldNode("Flags", 0, 3, base("ULONG")));
    }

    @Test
    void consecutiveDiffsWithNoChangesAreOmitted() {
        Timeline t = TimelineDiffer.timeline(List.of(
                new BuildEntry("b1", v1(), true),
                new BuildEntry("b2", v1(), true),
                new BuildEntry("b3", v2(), true)));

        assertThat(t.transitions()).hasSize(1);
        assertThat(t.transitions().get(0).from()).isEqualTo("b2");
        assertThat(t.transitions().get(0).to()).isEqualTo("b3");
    }

    @Test
    void fieldHistoryCollapsesStableRunsAndTracksAddRemove() {
        Timeline t = TimelineDiffer.timeline(List.of(
                new BuildEntry("1507", v1(), true),
                new BuildEntry("1607", v2(), true),
                new BuildEntry("1703", v2(), true),
                new BuildEntry("21h2", v3(), true)));

        FieldHistory b = history(t, "B");
        assertThat(b.points()).hasSize(2);
        assertThat(b.points().get(0).build()).isEqualTo("1507");
        assertThat(b.points().get(0).offset()).isEqualTo(4);
        assertThat(b.points().get(1).build()).isEqualTo("1607");
        assertThat(b.points().get(1).offset()).isEqualTo(8);

        FieldHistory added = history(t, "NEW");
        assertThat(added.addedIn()).isEqualTo("1607");
        assertThat(added.removedIn()).isNull();
        assertThat(added.points()).hasSize(1);

        FieldHistory flags = history(t, "Flags");
        assertThat(flags.addedIn()).isEqualTo("21h2");

        assertThat(t.hasMovement()).isTrue();
    }

    @Test
    void absentBuildsAreTrackedViaPresenceNotCrash() {
        Timeline t = TimelineDiffer.timeline(List.of(
                new BuildEntry("1507", v1(), true),
                new BuildEntry("1607", null, false),
                new BuildEntry("1703", v1(), true)));

        // type removed then re-added: whole-type transitions recorded
        assertThat(t.transitions()).isNotEmpty();
        assertThat(t.transitions().get(0).changes().get(0).kind())
                .isEqualTo(ChangeKind.TYPE_CHANGED);
        assertThat(t.transitions().get(0).changes().get(0).detail())
                .contains("removed");
    }

    @Test
    void pseudocodeEmitsPerTransitionLogAndDispatch() {
        Timeline t = TimelineDiffer.timeline(List.of(
                new BuildEntry("x64/f/1507", v1(), true),
                new BuildEntry("x64/f/1607", v2(), true),
                new BuildEntry("x64/f/21h2", v3(), true)));
        String code = TimelinePseudocode.generate("S",
                List.of("x64/f/1507", "x64/f/1607", "x64/f/21h2"), t);

        assertThat(code).contains("evolution across 3 builds");
        assertThat(code).contains("// In 1607:");
        assertThat(code).contains("+ NEW @0x4");
        assertThat(code).contains("~ B moved +4 (now @0x8)");
        assertThat(code).contains("if (build == x64/f/21h2) {");
        assertThat(code).contains("else if (build == x64/f/1607) {");
        assertThat(code).contains("b = *(typeof(B)*)(base + 0x8);");
        assertThat(code).contains("b = *(typeof(B)*)(base + 0x4);");
        assertThat(code).contains("Stable across all builds:");
        assertThat(code).contains("A @0x0");
    }

    private static FieldHistory history(Timeline t, String path) {
        return t.fieldHistories().stream()
                .filter(h -> h.path().equals(path))
                .findFirst().orElseThrow();
    }
}
