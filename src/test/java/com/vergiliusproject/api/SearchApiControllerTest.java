package com.vergiliusproject.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SearchApiControllerTest {

    @TempDir
    Path dumpDir;

    private MockMvc mvcWith(SearchApiController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private void writeManifest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode manifest = mapper.createObjectNode();
        manifest.put("format", 1);
        ObjectNode types = manifest.putObject("types");

        ArrayNode locs = types.putArray("LIST_ENTRY");
        ObjectNode loc = locs.addObject();
        loc.put("arch", "x64");
        loc.put("family", "windows-10");
        loc.put("os", "1607");
        loc.put("buildnumber", "10.0.14393.0");
        loc.put("file", "/api/os/x64/1607.json");
        loc.put("href", "/api/v1/kernels/x64/windows-10/1607/types/LIST_ENTRY");

        ArrayNode pebLocs = types.putArray("_PEB");
        ObjectNode pebLoc = pebLocs.addObject();
        pebLoc.put("arch", "x64");
        pebLoc.put("family", "windows-10");
        pebLoc.put("os", "1607");
        pebLoc.put("buildnumber", "10.0.14393.0");
        pebLoc.put("file", "/api/os/x64/1607.json");
        pebLoc.put("href", "/api/v1/kernels/x64/windows-10/1607/types/_PEB");

        Files.writeString(dumpDir.resolve("index.json"), mapper.writeValueAsString(manifest));
    }

    @Test
    void searchReturnsHitsFromManifest() throws Exception {
        writeManifest();
        SearchApiController controller = new SearchApiController(dumpDir.toString());
        MockMvc mvc = mvcWith(controller);

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/types/search").queryParam("q", "peb")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        String body = res.getResponse().getContentAsString();
        assertThat(body).contains("_PEB");
        assertThat(body).doesNotContain("LIST_ENTRY");
    }

    @Test
    void searchIsCaseInsensitiveAndCapped() throws Exception {
        writeManifest();
        SearchApiController controller = new SearchApiController(dumpDir.toString());
        MockMvc mvc = mvcWith(controller);

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/types/search").queryParam("q", "list_entry")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        assertThat(res.getResponse().getContentAsString()).contains("LIST_ENTRY");
    }

    @Test
    void emptyQueryReturns400ProblemDetail() throws Exception {
        SearchApiController controller = new SearchApiController(dumpDir.toString());
        MockMvc mvc = mvcWith(controller);

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/types/search").queryParam("q", " ")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(res.getResponse().getContentType()).isEqualTo("application/problem+json");
    }

    @Test
    void missingManifestReturnsEmptyListNotError() throws Exception {
        SearchApiController controller = new SearchApiController(dumpDir.toString());
        MockMvc mvc = mvcWith(controller);

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/types/search").queryParam("q", "anything")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        assertThat(res.getResponse().getContentAsString()).isEqualTo("[]");
    }
}
