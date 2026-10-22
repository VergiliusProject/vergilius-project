package com.vergiliusproject.api.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.vergiliusproject.api.ApiDtos.FieldNode;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.api.diff.TypeDiffer.Change;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiffPseudocodeTest {

    private static TypeNode base(String name) {
        return new TypeNode("BASE", name, 4, false, false, null, null, null,
                null, null, name, null, null, null, null, null, null, null, null);
    }

    private static TypeNode struct(Integer size, FieldNode... fields) {
        return new TypeNode("STRUCT", "MyStruct", size, false, false, null, null, null,
                null, null, null, null, null, null, null, null, List.of(fields), null, null);
    }

    private static FieldNode field(String name, Integer offset, TypeNode type) {
        return new FieldNode(name, offset, null, type);
    }

    @Test
    void insertionScenarioGeneratesShiftExplanationAndBuildBranch() {
        // v1: A@0, B@8   ->   v2: A@0, NEW@8, B@16
        TypeNode v1 = struct(16, field("A", 0, base("ULONG")), field("B", 8, base("ULONGLONG")));
        TypeNode v2 = struct(24, field("A", 0, base("ULONG")), field("NEW", 8, base("ULONGLONG")),
                field("B", 16, base("ULONGLONG")));

        List<Change> changes = TypeDiffer.diff(v1, v2);
        String code = DiffPseudocode.generate(
                new DiffPseudocode.Context("win10/1607", "win11/21h2", "MyStruct", 16, 24),
                changes);

        assertThat(code).contains("sizeof changed by +8 bytes");
        assertThat(code).contains("A new field (ULONGLONG ) was inserted at 0x8");
        assertThat(code).contains("shifted uniformly by +8 bytes");
        assertThat(code).contains("if (build == win11/21h2");
        assertThat(code).contains("b = *(typeof(B)*)(base + 0x10);");
        assertThat(code).contains("b = *(typeof(B)*)(base + 0x8);");
        assertThat(code).contains("+ NEW @ 0x8 (ULONGLONG)");
    }

    @Test
    void noChangesYieldsEmptyNote() {
        TypeNode same = struct(8, field("A", 0, base("ULONG")));
        String code = DiffPseudocode.generate(
                new DiffPseudocode.Context("a", "b", "MyStruct", 8, 8),
                TypeDiffer.diff(same, same));

        assertThat(code).contains("No differences between builds");
        assertThat(code).doesNotContain("if (build");
    }

    @Test
    void removedFieldDocumented() {
        TypeNode v1 = struct(8, field("A", 0, base("ULONG")), field("Legacy", 4, base("ULONG")));
        TypeNode v2 = struct(8, field("A", 0, base("ULONG")));

        String code = DiffPseudocode.generate(
                new DiffPseudocode.Context("a", "b", "MyStruct", 8, 8),
                TypeDiffer.diff(v1, v2));

        assertThat(code).contains("- Legacy @ 0x4 (ULONG)");
    }
}
