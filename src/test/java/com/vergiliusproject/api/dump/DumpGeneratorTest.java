package com.vergiliusproject.api.dump;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vergiliusproject.api.TypeLoader;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TtypeRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class DumpGeneratorTest {

    @Autowired
    private OsRepository osRepository;

    @Autowired
    private TtypeRepository typeRepository;

    private DumpGenerator generator;
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path outDir;

    private Os os;

    @BeforeEach
    void setUp() {
        generator = new DumpGenerator(osRepository, typeRepository, new TypeLoader(typeRepository));

        os = new Os();
        os.setFamilyName("Windows 10");
        os.setOsName("1607");
        os.setBuildnumber("10.0.14393.0");
        os.setArch("x64");
        os.setTimestamp(1468632695L);
        osRepository.save(os);
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
        if (owner.getData() == null) {
            owner.setData(new HashSet<>());
        }
        owner.getData().add(d);
    }

    private void seedBuild() {
        Ttype base = type(1, Ttype.Kind.BASE, "ULONG", 4);
        Ttype listEntry = type(2, Ttype.Kind.STRUCT, "LIST_ENTRY", 16);
        field(listEntry, "Flink", 3, 0, 0);
        field(listEntry, "Blink", 3, 8, 1);
        Ttype ptr = type(3, Ttype.Kind.POINTER, null, 8);
        field(ptr, "ptr", 2, 0, 0);
        Ttype holder = type(4, Ttype.Kind.STRUCT, "HOLDER", 16);
        field(holder, "Link", 2, 0, 0);
        Ttype pool = type(5, Ttype.Kind.ENUM, "POOL_TYPE", 4);
        field(pool, "Paged", 1, 0, 0);
        Ttype poolUser = type(6, Ttype.Kind.STRUCT, "POOL_USER", 4);
        field(poolUser, "Pool", 5, 0, 0);
        typeRepository.saveAll(List.of(base, listEntry, ptr, holder, pool, poolUser));
    }

    @Test
    void buildDocumentContainsTypesTreeCSourceAndUsedIn() throws Exception {
        seedBuild();

        generator.generateAll(outDir);

        Path doc = outDir.resolve("os/x64/1607.json");
        assertThat(doc).exists();
        JsonNode root = mapper.readTree(Files.readString(doc));

        assertThat(root.get("format").asInt()).isEqualTo(1);
        assertThat(root.get("arch").asText()).isEqualTo("x64");
        assertThat(root.get("buildnumber").asText()).isEqualTo("10.0.14393.0");

        JsonNode types = root.get("types");
        assertThat(types).hasSize(4);
        assertThat(types.get(0).get("name").asText()).isEqualTo("HOLDER");

        JsonNode holder = types.get(0);
        assertThat(holder.get("tree").get("fields").get(0).get("type").get("kind").asText())
                .isEqualTo("REF");
        assertThat(holder.get("cSource").asText()).contains("struct LIST_ENTRY");

        JsonNode usedIn = root.get("usedIn");
        assertThat(usedIn.get("LIST_ENTRY").toString()).contains("HOLDER");
    }

    @Test
    void manifestHasReverseIndexAndBuildEntries() throws Exception {
        seedBuild();

        generator.generateAll(outDir);

        JsonNode manifest = mapper.readTree(
                Files.readString(outDir.resolve("index.json")));
        assertThat(manifest.get("format").asInt()).isEqualTo(1);
        // the H2 context is shared with other corpus harnesses; find OUR build
        JsonNode ourBuild = null;
        for (JsonNode b : manifest.get("builds")) {
            if ("1607".equals(b.get("os").asText())) {
                ourBuild = b;
                break;
            }
        }
        assertThat(ourBuild).isNotNull();
        assertThat(ourBuild.get("file").asText()).isEqualTo("/api/os/x64/1607.json");

        JsonNode index = manifest.get("types");
        JsonNode listEntryLocs = null;
        for (JsonNode loc : index.get("LIST_ENTRY")) {
            if ("1607".equals(loc.get("os").asText())) {
                listEntryLocs = loc;
                break;
            }
        }
        assertThat(listEntryLocs).isNotNull();
        assertThat(listEntryLocs.get("arch").asText()).isEqualTo("x64");
        JsonNode poolHref = null;
        for (JsonNode loc : index.get("POOL_TYPE")) {
            if ("1607".equals(loc.get("os").asText())) {
                poolHref = loc;
                break;
            }
        }
        assertThat(poolHref.get("href").asText())
                .isEqualTo("/api/v1/kernels/x64/windows-10/1607/types/POOL_TYPE");
    }

    @Test
    void outputIsByteStableAcrossRuns() throws Exception {
        seedBuild();

        Path first = outDir.resolve("run1");
        Path second = outDir.resolve("run2");
        generator.generateAll(first);
        generator.generateAll(second);

        assertThat(Files.readString(first.resolve("index.json")))
                .isEqualTo(Files.readString(second.resolve("index.json")));
        assertThat(Files.readString(first.resolve("os/x64/1607.json")))
                .isEqualTo(Files.readString(second.resolve("os/x64/1607.json")));
    }
}
