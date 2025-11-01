package com.vergiliusproject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
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
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TtypeRepository;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FieldBuilderTest {
    private Os os;

    @Autowired
    private OsRepository osRepository;

    @Autowired
    private TtypeRepository typeRepository;

    // MethodSource that yields every file under src/test/resources/FieldBuilderTest/data
    static Stream<Arguments> inputFiles() throws Exception {
        Path dir = Paths.get(FieldBuilderTest.class.getResource("/FieldBuilderTest/data").toURI());

        return Files.list(dir)
                .filter(Files::isRegularFile)
                .sorted()
                .map(path -> Arguments.of(path, path.getFileName().toString()));
    }

    @BeforeAll
    void setupAll() throws Exception {
        try (var stream = FieldBuilderTest.class.getResourceAsStream("/FieldBuilderTest/data.yml")) 
        {
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
            
            types.stream().forEach(type -> { 
                type.setOpersys(os); 
                var datas = type.getData();
                
                if (datas != null) {
                    datas.forEach(data -> data.setTtype(type));
                }
            });
            
            os.setTtypes(new HashSet<>(types));

            osRepository.save(os);
        }
    }

    // One ParameterizedTest invocation per file
    @ParameterizedTest(name = "{index} => {1}")
    @MethodSource("inputFiles")
    void testEachFile(Path inputFile, String fileName) throws Exception {
        var type = typeRepository.findByNameAndOpersys(fileName, os);
        var typeDefinition = FieldBuilder.recursionProcessing(typeRepository, type.getFirst(), 0, 0, null, os).toString();

        assertEquals(Files.readString(inputFile), typeDefinition);
    }
}
