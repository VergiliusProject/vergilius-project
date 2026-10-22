package com.vergiliusproject.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import com.vergiliusproject.dto.Root;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TtypeRepository;

/**
 * Gate 2: walker/FieldBuilder parity harness. Every corpus golden must be
 * reproducible byte-for-byte from the TypeTreeBuilder tree rendered by
 * CDeclRenderer. The goldens themselves are FieldBuilder.toString() output,
 * so passing this proves the walker decodes identical semantics.
 */
@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WalkerFieldBuilderParityTest {
    private Os os;

    @Autowired
    private OsRepository osRepository;

    @Autowired
    private TtypeRepository typeRepository;

    static Stream<Arguments> inputFiles() throws Exception {
        Path dir = Paths.get(WalkerFieldBuilderParityTest.class.getResource("/FieldBuilderTest/data").toURI());
        return Files.list(dir)
                .filter(Files::isRegularFile)
                .sorted()
                .map(path -> Arguments.of(path, path.getFileName().toString()));
    }

    @BeforeAll
    void setupAll() throws Exception {
        try (var stream = WalkerFieldBuilderParityTest.class.getResourceAsStream("/FieldBuilderTest/data.yml")) {
            Yaml yaml = new Yaml();
            yaml.setBeanAccess(BeanAccess.FIELD);
            Root root = yaml.loadAs(stream, Root.class);

            os = new Os();
            os.setFamilyName("TestFamily");
            os.setOsName("TestOS");
            os.setBuildnumber("0.0.0.0");
            os.setArch("x64");
            os.setTimestamp(root.getTimestamp());

            var types = root.getTypes();
            types.forEach(type -> {
                type.setOpersys(os);
                if (type.getData() != null) {
                    type.getData().forEach(data -> data.setTtype(type));
                }
            });
            os.setTtypes(new HashSet<>(types));
            osRepository.save(os);
        }
    }

    @ParameterizedTest(name = "{index} => {1}")
    @MethodSource("inputFiles")
    void walkerMatchesFieldBuilderGolden(Path inputFile, String fileName) throws Exception {
        List<Ttype> found = typeRepository.findByNameAndOpersys(fileName, os);
        assertTrue(!found.isEmpty(), "type " + fileName + " missing from corpus");
        Ttype type = found.getFirst();

        TypeLoader loader = new TypeLoader(typeRepository);
        Map<Integer, Ttype> closure = loader.loadClosure(os, type.getId());
        var tree = new TypeTreeBuilder(closure, "/api/v1/kernels/x64/f/o/types/").buildRoot(type);
        String rendered = CDeclRenderer.render(tree);

        // Goldens embed FieldBuilder's HTML anchors; the walker never sees
        // them. Column alignment inside goldens was computed on anchor-stripped
        // lengths (FieldBuilder strips with this same regex), so comparing
        // against the stripped golden is byte-exact.
        String expected = Files.readString(inputFile).replaceAll("(<.+?>)", "");
        assertEquals(expected, rendered, () -> diffHint(expected, rendered));
    }

    private static String diffHint(String expected, String actual) {
        String[] e = expected.split("\n");
        String[] a = actual.split("\n");
        for (int i = 0; i < Math.min(e.length, a.length); i++) {
            if (!e[i].equals(a[i])) {
                return "first differing line " + i + ":\n  expected: " + e[i] + "\n  actual:   " + a[i];
            }
        }
        return "length differs: expected " + e.length + " lines, actual " + a.length;
    }
}
