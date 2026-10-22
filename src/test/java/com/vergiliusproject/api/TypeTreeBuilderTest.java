package com.vergiliusproject.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vergiliusproject.api.ApiDtos.FieldNode;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TypeTreeBuilderTest {

    private final Map<Integer, Ttype> closure = new HashMap<>();

    private Ttype type(int id, Ttype.Kind kind, String name, int sizeof) {
        Ttype t = new Ttype();
        t.setId(id);
        t.setKind(kind);
        t.setName(name);
        t.setSizeof(sizeof);
        t.setOpersys(new Os());
        closure.put(id, t);
        return t;
    }

    private void field(Ttype owner, String name, int refId, int offset, int ordinal) {
        Tdata d = new Tdata();
        d.setName(name);
        d.setId(refId);
        d.setOffset(offset);
        d.setOrdinal(ordinal);
        d.setTtype(owner);
        if (owner.getData() == null) {
            owner.setData(new HashSet<>());
        }
        owner.getData().add(d);
    }

    private TypeNode build(Ttype root) {
        return new TypeTreeBuilder(closure, "/api/v1/kernels/x64/f/o/types/").buildRoot(root);
    }

    @Test
    void namedNonRootAggregateBecomesRefWithHref() {
        type(1, Ttype.Kind.BASE, "ULONG", 4);
        Ttype listEntry = type(2, Ttype.Kind.STRUCT, "LIST_ENTRY", 16);
        field(listEntry, "Flink", 3, 0, 0);
        field(listEntry, "Blink", 3, 8, 1);
        type(3, Ttype.Kind.POINTER, null, 8);
        Ttype root = type(4, Ttype.Kind.STRUCT, "HOLDER", 16);
        field(root, "List", 2, 0, 0);

        TypeNode tree = build(root);

        TypeNode listField = tree.fields().get(0).type();
        assertThat(listField.kind()).isEqualTo("REF");
        assertThat(listField.name()).isEqualTo("LIST_ENTRY");
        assertThat(listField.href()).isEqualTo("/api/v1/kernels/x64/f/o/types/LIST_ENTRY");
    }

    @Test
    void selfReferentialStructCutsCycleAtNamedNode() {
        type(1, Ttype.Kind.BASE, "LONG", 8);
        Ttype list = type(2, Ttype.Kind.STRUCT, "LIST_ENTRY", 16);
        field(list, "Flink", 3, 0, 0);
        Ttype ptr = type(3, Ttype.Kind.POINTER, null, 8);
        field(ptr, "ptr", 2, 0, 0);

        TypeNode tree = build(list);

        TypeNode flink = tree.fields().get(0).type();
        assertThat(flink.kind()).isEqualTo("POINTER");
        assertThat(flink.target().kind()).isEqualTo("REF");
        assertThat(flink.target().name()).isEqualTo("LIST_ENTRY");
    }

    @Test
    void enumConstantsInlineWithValuesFromOffset() {
        Ttype pool = type(1, Ttype.Kind.ENUM, "POOL_TYPE", 4);
        field(pool, "NonPaged", 0, 0, 0);
        field(pool, "Paged", 0, 1, 1);
        Ttype root = type(2, Ttype.Kind.STRUCT, "HOLDER", 4);
        field(root, "Pool", 1, 0, 0);

        TypeNode tree = build(root);

        TypeNode ref = tree.fields().get(0).type();
        assertThat(ref.kind()).isEqualTo("REF");
        TypeNode rootEnum = build(pool);
        assertThat(rootEnum.kind()).isEqualTo("ENUM");
        assertThat(rootEnum.constants()).hasSize(2);
        assertThat(rootEnum.constants().get(1).value()).isEqualTo(1);
    }

    @Test
    void bitfieldNamesParseIntoNameAndWidth() {
        type(1, Ttype.Kind.BASE, "ULONGLONG", 8);
        Ttype root = type(2, Ttype.Kind.STRUCT, "BITS", 8);
        field(root, "Dirty:1", 1, 0, 0);
        field(root, "Count:62", 1, 0, 1);
        field(root, "Whole", 1, 8, 2);

        TypeNode tree = build(root);

        FieldNode first = tree.fields().get(0);
        assertThat(first.name()).isEqualTo("Dirty");
        assertThat(first.bitWidth()).isEqualTo(1);
        assertThat(tree.fields().get(2).bitWidth()).isNull();
    }

    @Test
    void arrayCountComesFromFieldOffset() {
        type(1, Ttype.Kind.BASE, "WCHAR", 2);
        Ttype arr = type(2, Ttype.Kind.ARRAY, null, 128);
        field(arr, "arr", 1, 64, 0);
        Ttype root = type(3, Ttype.Kind.STRUCT, "BUF", 128);
        field(root, "Buffer", 2, 0, 0);

        TypeNode tree = build(root);

        TypeNode arrNode = tree.fields().get(0).type();
        assertThat(arrNode.kind()).isEqualTo("ARRAY");
        assertThat(arrNode.count()).isEqualTo(64);
        assertThat(arrNode.element().name()).isEqualTo("WCHAR");
    }

    @Test
    void functionReturnSentinelBecomesReturnTypeAndParameters() {
        type(1, Ttype.Kind.BASE, "VOID", 0);
        type(2, Ttype.Kind.BASE, "ULONG", 4);
        Ttype fn = type(3, Ttype.Kind.FUNCTION, null, 8);
        field(fn, "return", 1, 0, 0);
        field(fn, "arg", 2, 0, 1);
        Ttype ptr = type(4, Ttype.Kind.POINTER, null, 8);
        field(ptr, "ptr", 3, 0, 0);
        Ttype root = type(5, Ttype.Kind.STRUCT, "HOLDER", 8);
        field(root, "Cb", 4, 0, 0);

        TypeNode tree = build(root);

        TypeNode fnPtr = tree.fields().get(0).type();
        assertThat(fnPtr.kind()).isEqualTo("POINTER");
        TypeNode fnNode = fnPtr.target();
        assertThat(fnNode.kind()).isEqualTo("FUNCTION");
        assertThat(fnNode.returnType().name()).isEqualTo("VOID");
        assertThat(fnNode.parameters()).hasSize(1);
        assertThat(fnNode.parameters().get(0).name()).isEqualTo("ULONG");
    }

    @Test
    void duplicateOffsetsInStructInferAnonymousUnion() {
        type(1, Ttype.Kind.BASE, "ULONG", 4);
        type(2, Ttype.Kind.BASE, "ULONGLONG", 8);
        Ttype root = type(3, Ttype.Kind.STRUCT, "S", 8);
        field(root, "A", 1, 0, 0);
        field(root, "B", 2, 0, 1);
        field(root, "C", 1, 8, 2);

        TypeNode tree = build(root);

        // A and B share offset 0 -> union piece; C follows separately
        assertThat(tree.fields()).hasSize(2);
        FieldNode unionField = tree.fields().get(0);
        assertThat(unionField.type().kind()).isEqualTo("UNION");
        assertThat(unionField.type().fields()).extracting(FieldNode::name)
                .containsExactly("A", "B");
        assertThat(tree.fields().get(1).name()).isEqualTo("C");
    }

    @Test
    void unionRunOfDistinctOffsetsInfersImplicitStruct() {
        type(1, Ttype.Kind.BASE, "ULONG", 4);
        Ttype root = type(2, Ttype.Kind.UNION, "U", 16);
        field(root, "A", 1, 0, 0);
        field(root, "B", 1, 0, 1);
        field(root, "C", 1, 8, 2);

        TypeNode tree = build(root);

        // Per the union02 golden: A plain (same offset as next); B wrapped
        // alone in an implicit struct; C plain (last field closes the run)
        List<FieldNode> fields = tree.fields();
        assertThat(fields.get(0).name()).isEqualTo("A");
        assertThat(fields.get(1).type().kind()).isEqualTo("STRUCT");
        assertThat(fields.get(1).type().fields()).extracting(FieldNode::name)
                .containsExactly("B");
        assertThat(fields.get(2).name()).isEqualTo("C");
    }

    @Test
    void anonymousNodesGetStableAnonIds() {
        type(1, Ttype.Kind.BASE, "ULONG", 4);
        Ttype root = type(2, Ttype.Kind.STRUCT, "S", 8);
        field(root, "A", 1, 0, 0);
        field(root, "B", 1, 0, 1);

        TypeNode tree = build(root);

        String anonId = tree.fields().get(0).type().anonId();
        assertThat(anonId).startsWith("anon:");
        TypeNode rebuilt = new TypeTreeBuilder(closure, "/x/").buildRoot(root);
        assertThat(rebuilt.fields().get(0).type().anonId()).isEqualTo(anonId);
    }

    @Test
    void anonymousCycleWithoutNamedNodeFailsLoudly() {
        Ttype a = type(1, Ttype.Kind.STRUCT, null, 8);
        field(a, "B", 2, 0, 0);
        Ttype b = type(2, Ttype.Kind.STRUCT, null, 8);
        field(b, "A", 1, 0, 0);

        assertThatThrownBy(() -> build(a))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cycle invariant");
    }

    @Test
    void nodeBudgetBackstopEmitsTruncatedMarker() {
        // chain of 3000 anonymous structs, each pointing to the next
        Ttype first = null;
        Ttype prev = null;
        for (int i = 1; i <= 3000; i++) {
            Ttype s = type(i, Ttype.Kind.STRUCT, null, 8);
            if (prev != null) {
                field(s, "next", i - 1, 0, 0);
            } else {
                field(s, "leaf", 3001, 0, 0);
            }
            if (first == null) {
                first = s;
            }
            prev = s;
        }
        type(3001, Ttype.Kind.BASE, "ULONG", 4);

        // chain: ROOT(3000, named) -> 2999 -> ... -> 1, all anonymous structs
        Ttype rootType = closure.get(3000);
        TypeNode tree = build(rootType);
        TypeNode node = tree;
        int depth = 0;
        while (depth < 5000) {
            if (Boolean.TRUE.equals(node.truncated())) {
                break;
            }
            if (node.fields() == null || node.fields().isEmpty()) {
                break;
            }
            node = node.fields().get(0).type();
            depth++;
        }
        assertThat(node.truncated()).isTrue();
        assertThat(depth).isLessThan(3000);
    }

    @Test
    void pointerToAnonymousStructInlinesTarget() {
        type(1, Ttype.Kind.BASE, "ULONG", 4);
        Ttype anon = type(2, Ttype.Kind.STRUCT, null, 4);
        field(anon, "Value", 1, 0, 0);
        Ttype ptr = type(3, Ttype.Kind.POINTER, null, 8);
        field(ptr, "ptr", 2, 0, 0);
        Ttype root = type(4, Ttype.Kind.STRUCT, "HOLDER", 8);
        field(root, "Ptr", 3, 0, 0);

        TypeNode tree = build(root);

        TypeNode ptrNode = tree.fields().get(0).type();
        assertThat(ptrNode.kind()).isEqualTo("POINTER");
        assertThat(ptrNode.target().kind()).isEqualTo("STRUCT");
        assertThat(ptrNode.target().fields().get(0).name()).isEqualTo("Value");
    }

    @Test
    void constAndVolatileFlagsPropagate() {
        Ttype constBase = type(1, Ttype.Kind.BASE, "ULONG", 4);
        constBase.setIsConst(true);
        Ttype volatileBase = type(2, Ttype.Kind.BASE, "ULONG", 4);
        volatileBase.setIsVolatile(true);
        Ttype root = type(3, Ttype.Kind.STRUCT, "MODS", 8);
        field(root, "A", 1, 0, 0);
        field(root, "B", 2, 4, 1);

        TypeNode tree = build(root);

        assertThat(tree.fields().get(0).type().isConst()).isTrue();
        assertThat(tree.fields().get(0).type().isVolatile()).isFalse();
        assertThat(tree.fields().get(1).type().isVolatile()).isTrue();
        assertThat(tree.fields().get(1).type().isConst()).isFalse();
    }
}
