package com.vergiliusproject.api.dump;

import com.vergiliusproject.entities.Os;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Regenerates dump documents after admin uploads. Async and failure-isolated:
 * a dump problem must never break an upload.
 */
@Service
public class DumpRefreshService {

    private static final Logger log = LoggerFactory.getLogger(DumpRefreshService.class);

    private final DumpGenerator generator;
    private final Path dumpDir;

    public DumpRefreshService(DumpGenerator generator,
            @Value("${vergilius.dump.dir:api-dump}") String dumpDir) {
        this.generator = generator;
        this.dumpDir = Paths.get(dumpDir).toAbsolutePath();
    }

    @Async
    public void refreshAfterUpload(Os os) {
        try {
            // Full regeneration: the manifest is derived entirely from the
            // DB, so rebuilding everything after any upload stays consistent.
            generator.generateAll(dumpDir);
        } catch (IOException | RuntimeException e) {
            log.error("Dump regeneration failed after upload of {} {}: {}",
                    os.getArch(), os.getOsName(), e.getMessage(), e);
        }
    }
}
