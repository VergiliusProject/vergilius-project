package com.vergiliusproject.api.dump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Batch dump trigger: activates when the app is started with
 * --dump-regenerate, regenerates every build document and the manifest, then
 * optionally exits (--dump-regenerate-and-exit for CI use).
 */
@Component
public class DumpRegenerateRunner implements ApplicationRunner {

    private final DumpGenerator generator;
    private final Path dumpDir;

    public DumpRegenerateRunner(DumpGenerator generator,
            @Value("${vergilius.dump.dir:api-dump}") String dumpDir) {
        this.generator = generator;
        this.dumpDir = Paths.get(dumpDir).toAbsolutePath();
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!args.containsOption("dump-regenerate")) {
            return;
        }
        Files.createDirectories(dumpDir);
        generator.generateAll(dumpDir);
        System.out.println("Dump regenerated at " + dumpDir);
        if (args.containsOption("dump-regenerate-and-exit")) {
            System.exit(0);
        }
    }
}
