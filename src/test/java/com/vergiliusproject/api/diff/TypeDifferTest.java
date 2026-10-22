package com.vergiliusproject.api.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.vergiliusproject.api.ApiDtos.EnumConstant;
import com.vergiliusproject.api.ApiDtos.FieldNode;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.api.diff.TypeDiffer.Change;
import com.vergiliusproject.api.diff.TypeDiffer.ChangeKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypeDifferTest {

    private static TypeNode base(String name) {
        return new TypeNode("BASE", name, 4, false, false, null, null, null,
                null, null, name, null, null, null, null, null, null, null, null);
    }

    private static TypeNode refNode(String name) {
        return TypeNode.ref("struct", name, "/x/" + name);
    }

    private static TypeNode struct(Integer size, FieldNode... fields) {
        return new TypeNode("STRUCT", "S", size, false, false, null, null, null,
                null, null, null, null, null, null, null, null, List.of(fields), null, null);
    }

    private static TypeNode anonStruct(FieldNode... fields) {
        return new TypeNode("STRUCT", null, null, false, false, "anon:0", null, null,
                null, null, null, null, null, null, null, null, List.of(fields), null, null);
    }

    private static FieldNode field(String name, Integer offset, TypeNode type) {
        return new FieldNode(name, offset, null, type);
    }

    @Test
    void identicalStructsProduceNoChanges() {
        TypeNode a = struct(16, field("A", 0, base("ULONG")), field("B", 8, base("ULONG")));
        TypeNode b = struct(16, field("A", 0, base("ULONG")), field("B", 8, base("ULONG")));

        assertThat(TypeDiffer.diff(a, b)).isEmpty();
    }

    @Test
    void movedFieldReportsOffsetDelta() {
        TypeNode a = struct(16, field("A", 0, base("ULONG")), field("B", 4, base("ULONG")));
        TypeNode b = struct(24, field("A", 0, base("ULONG")), field("B", 8, base("ULONG")));

        List<Change> changes = TypeDiffer.diff(a, b);
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).kind()).isEqualTo(ChangeKind.MOVED);
        assertThat(changes.get(0).path()).isEqualTo("B");
        assertThat(changes.get(0).offsetDelta()).isEqualTo(4);
    }

    @Test
    void addedAndRemovedFieldsAreListed() {
        TypeNode a = struct(8, field("A", 0, base("ULONG")), field("Old", 4, base("ULONG")));
        TypeNode b = struct(8, field("A", 0, base("ULONG")), field("New", 4, base("ULONG")));

        List<Change> changes = TypeDiffer.diff(a, b);
        assertThat(changes).extracting(Change::kind)
                .containsExactlyInAnyOrder(ChangeKind.REMOVED, ChangeKind.ADDED);
        assertThat(changes).extracting(Change::path).contains("Old", "New");
    }

    @Test
    void typeChangeDetectedOnSameNameDifferentType() {
        TypeNode a = struct(8, field("A", 0, base("ULONG")));
        TypeNode b = struct(8, field("A", 0, refNode("LIST_ENTRY")));

        List<Change> changes = TypeDiffer.diff(a, b);
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).kind()).isEqualTo(ChangeKind.TYPE_CHANGED);
        assertThat(changes.get(0).fromType()).isEqualTo("ULONG");
        assertThat(changes.get(0).toType()).isEqualTo("struct LIST_ENTRY");
    }

    @Test
    void bitfieldWidthChangeDetected() {
        FieldNode from = new FieldNode("Flags", 0, 3, base("ULONG"));
        FieldNode to = new FieldNode("Flags", 0, 7, base("ULONG"));

        List<Change> changes = TypeDiffer.diff(struct(4, from), struct(4, to));
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).kind()).isEqualTo(ChangeKind.BITFIELD_CHANGED);
    }

    @Test
    void inlineAnonymousAggregateIsRecursed() {
        TypeNode fromInner = anonStruct(
                field("Lo", 0, base("ULONG")),
                field("Hi", 4, base("ULONG")));
        TypeNode toInner = anonStruct(
                field("Lo", 0, base("ULONG")),
                field("Hi", 8, base("ULONG")));

        TypeNode a = struct(8, field("Value", 0, fromInner));
        TypeNode b = struct(16, field("Value", 0, toInner));

        List<Change> changes = TypeDiffer.diff(a, b);
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).kind()).isEqualTo(ChangeKind.MOVED);
        assertThat(changes.get(0).path()).isEqualTo("Value.Hi");
    }

    @Test
    void enumConstantChangesDetected() {
        TypeNode from = new TypeNode("ENUM", "E", 4, false, false, null, null, null,
                null, null, null, null, null, null, null, null, null,
                List.of(new EnumConstant("A", 0), new EnumConstant("B", 1)), null);
        TypeNode to = new TypeNode("ENUM", "E", 4, false, false, null, null, null,
                null, null, null, null, null, null, null, null, null,
                List.of(new EnumConstant("A", 0), new EnumConstant("B", 5),
                        new EnumConstant("C", 6)), null);

        List<Change> changes = TypeDiffer.diff(from, to);
        assertThat(changes).extracting(Change::kind)
                .containsExactlyInAnyOrder(ChangeKind.VALUE_CHANGED, ChangeKind.ADDED);
    }

    @Test
    void arrayCountChangeDetected() {
        TypeNode fromArr = new TypeNode("ARRAY", null, 64, false, false, null, null, null,
                null, null, null, null, 16, base("ULONG"), null, null, null, null, null);
        TypeNode toArr = new TypeNode("ARRAY", null, 128, false, false, null, null, null,
                null, null, null, null, 32, base("ULONG"), null, null, null, null, null);

        List<Change> changes = TypeDiffer.diff(
                struct(64, field("Buf", 0, fromArr)),
                struct(128, field("Buf", 0, toArr)));
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).detail()).contains("array count 16 -> 32");
    }

    @Test
    void missingSideReportsWholeTypeChange() {
        TypeNode b = struct(8, field("A", 0, base("ULONG")));

        List<Change> changes = TypeDiffer.diff(null, b);
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).kind()).isEqualTo(ChangeKind.TYPE_CHANGED);
        assertThat(changes.get(0).detail()).contains("added");
    }
}
