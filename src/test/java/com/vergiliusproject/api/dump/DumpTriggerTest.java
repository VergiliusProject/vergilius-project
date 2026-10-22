package com.vergiliusproject.api.dump;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vergiliusproject.entities.Os;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

class DumpTriggerTest {

    @TempDir
    Path dumpDir;

    private Os os() {
        Os o = new Os();
        o.setFamilyName("F");
        o.setOsName("O");
        o.setBuildnumber("1.0.0.0");
        o.setArch("x64");
        o.setTimestamp(0L);
        return o;
    }

    @Test
    void refreshSwallowsGeneratorFailure() throws IOException {
        DumpGenerator failing = mock(DumpGenerator.class);
        org.mockito.Mockito.doThrow(new IOException("disk full"))
                .when(failing).generateAll(any());

        DumpRefreshService service = new DumpRefreshService(failing, dumpDir.toString());

        // must not propagate - an upload must never fail because of dumping
        service.refreshAfterUpload(os());

        verify(failing).generateAll(any());
    }

    @Test
    void runnerRegeneratesOnlyWhenFlagPresent() throws Exception {
        DumpGenerator generator = mock(DumpGenerator.class);
        DumpRegenerateRunner runner = new DumpRegenerateRunner(generator, dumpDir.toString());

        runner.run(new DefaultApplicationArguments("serve"));

        verify(generator, never()).generateAll(any());

        runner.run(new DefaultApplicationArguments("serve", "--dump-regenerate"));

        verify(generator).generateAll(any(Path.class));
    }

}
