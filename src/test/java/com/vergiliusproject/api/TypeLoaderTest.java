package com.vergiliusproject.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TtypeRepository;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class TypeLoaderTest {

    @Autowired
    private OsRepository osRepository;

    @Autowired
    private TtypeRepository typeRepository;

    private final TypeLoader loader = new TypeLoader(null);
    private TypeLoader wiredLoader;
    private Os os;

    @BeforeEach
    void setUp() {
        os = new Os();
        os.setFamilyName("F");
        os.setOsName("O");
        os.setBuildnumber("1.0.0.0");
        os.setArch("x64");
        os.setTimestamp(0L);
        osRepository.save(os);
        wiredLoader = new TypeLoader(typeRepository);
    }

    private Ttype type(int id, Ttype.Kind kind, String name, int sizeof) {
        Ttype t = new Ttype();
        t.setId(id);
        t.setKind(kind);
        t.setName(name);
        t.setSizeof(sizeof);
        t.setOpersys(os);
        return t;
    }

    private void field(Ttype owner, String name, int refId, int offset, int ordinal) {
        Tdata d = new Tdata();
        d.setName(name);
        d.setId(refId);
        d.setOffset(offset);
        d.setOrdinal(ordinal);
        d.setTtype(owner);
        var data = owner.getData();
        if (data == null) {
            data = new HashSet<>();
            owner.setData(data);
        }
        data.add(d);
    }

    @Test
    void closureIncludesRootAndAllTransitiveReferences() {
        Ttype root = type(1, Ttype.Kind.STRUCT, "root", 24);
        field(root, "Entry", 2, 0, 0);
        field(root, "Value", 3, 16, 1);
        Ttype mid = type(2, Ttype.Kind.STRUCT, "mid", 8);
        field(mid, "Deep", 3, 0, 0);
        Ttype leaf = type(3, Ttype.Kind.BASE, "ULONG", 4);
        typeRepository.saveAll(java.util.List.of(root, mid, leaf));

        Map<Integer, Ttype> closure = wiredLoader.loadClosure(os, 1);

        assertThat(closure).containsKeys(1, 2, 3);
    }

    @Test
    void chunkBoundariesResolveWithMoreThanOneChunk() {
        // >CHUNK_SIZE (500) referenced types forces at least two IN queries
        Ttype root = type(0, Ttype.Kind.STRUCT, "root", 8);
        for (int i = 1; i <= 1200; i++) {
            field(root, "f" + i, i, 0, i - 1);
        }
        typeRepository.save(root);
        for (int i = 1; i <= 1200; i++) {
            typeRepository.save(type(i, Ttype.Kind.BASE, "T" + i, 4));
        }

        Map<Integer, Ttype> closure = wiredLoader.loadClosure(os, 0);

        assertThat(closure).hasSize(1201);
        assertThat(closure.get(1200)).isNotNull();
    }

    @Test
    void cyclesTerminate() {
        Ttype a = type(1, Ttype.Kind.STRUCT, "A", 8);
        field(a, "B", 2, 0, 0);
        Ttype b = type(2, Ttype.Kind.STRUCT, "B", 8);
        field(b, "A", 1, 0, 0);
        typeRepository.saveAll(java.util.List.of(a, b));

        Map<Integer, Ttype> closure = wiredLoader.loadClosure(os, 1);

        assertThat(closure).containsKeys(1, 2);
    }

    @Test
    void fieldsOfReturnsDefensiveCopy() {
        Ttype t = type(1, Ttype.Kind.STRUCT, "root", 4);
        field(t, "a", 2, 0, 0);
        var copy = TypeLoader.fieldsOf(t);
        copy.clear();
        assertThat(t.getData()).hasSize(1);
    }

    @Test
    void fieldsSortedByOrdinalOrdersFields() {
        Ttype t = type(1, Ttype.Kind.STRUCT, "root", 4);
        field(t, "second", 2, 0, 1);
        field(t, "first", 2, 0, 0);
        var sorted = TypeLoader.fieldsSortedByOrdinal(t);
        assertThat(sorted.get(0).getName()).isEqualTo("first");
        assertThat(sorted.get(1).getName()).isEqualTo("second");
    }

    @Test
    void loadOneReturnsTypeOrNull() {
        Ttype t = type(9, Ttype.Kind.BASE, "BASE9", 4);
        typeRepository.save(t);

        assertThat(wiredLoader.loadOne(os, 9).getName()).isEqualTo("BASE9");
        assertThat(wiredLoader.loadOne(os, 404)).isNull();
    }
}
