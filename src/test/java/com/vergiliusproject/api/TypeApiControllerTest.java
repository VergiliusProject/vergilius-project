package com.vergiliusproject.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vergiliusproject.FieldBuilder;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TtypeRepository;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TypeApiControllerTest {

    private static final String LIST_URL = "/api/v1/kernels/x64/fam/os/types";

    private OsRepository osRepo;
    private TtypeRepository typeRepo;
    private MockMvc mvc;
    private Os os;

    @BeforeEach
    void setUp() {
        osRepo = mock(OsRepository.class);
        typeRepo = mock(TtypeRepository.class);
        TypeLoader loader = new TypeLoader(typeRepo);
        TypeApiController controller = new TypeApiController(new ApiOsResolver(osRepo), typeRepo, loader);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        os = new Os();
        os.setFamilyName("Fam");
        os.setOsName("OS");
        os.setFamilySlug("fam");
        os.setOsSlug("os");
        os.setBuildnumber("1.0.0.0");
        os.setArch("x64");
        os.setTimestamp(0L);
        when(osRepo.findByArchAndFamilySlugAndOsSlug("x64", "fam", "os")).thenReturn(os);
    }

    private Ttype struct(String name, int sizeof) {
        Ttype t = new Ttype();
        t.setId(name.hashCode());
        t.setName(name);
        t.setKind(Ttype.Kind.STRUCT);
        t.setSizeof(sizeof);
        t.setOpersys(os);
        return t;
    }

    @Test
    void listPaginatesAndFiltersByKind() throws Exception {
        Ttype a = struct("Alpha", 8);
        Ttype b = struct("Beta", 16);
        Ttype c = new Ttype();
        c.setName("Gamma");
        c.setKind(Ttype.Kind.ENUM);
        c.setSizeof(4);
        c.setOpersys(os);
        when(typeRepo.findStructEnumUnionByOpersys(os)).thenReturn(List.of(a, b, c));

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(LIST_URL).queryParam("kind", "struct").queryParam("page", "0")
                .queryParam("size", "1")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        String body = res.getResponse().getContentAsString();
        assertThat(body).contains("Alpha");
        assertThat(body).doesNotContain("Gamma");
        assertThat(body).contains("\"totalElements\":2");
        assertThat(res.getResponse().getHeader("ETag")).isNotBlank();
        assertThat(res.getResponse().getHeader("Cache-Control")).contains("max-age=86400");
    }

    @Test
    void listSearchesCaseInsensitively() throws Exception {
        Ttype a = struct("Alpha", 8);
        when(typeRepo.findStructEnumUnionByOpersys(os)).thenReturn(List.of(a));

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(LIST_URL).queryParam("search", "ALP")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        assertThat(res.getResponse().getContentAsString()).contains("Alpha");
    }

    @Test
    void listRejectsInvalidKind() throws Exception {
        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(LIST_URL).queryParam("kind", "pointer")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(res.getResponse().getContentAsString()).contains("Invalid kind");
    }

    @Test
    void listRejectsOversizePage() throws Exception {
        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(LIST_URL).queryParam("size", "500")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void detailReturnsTreeAndStrippedCSourceWithEtag() throws Exception {
        Ttype base = new Ttype();
        base.setId(2);
        base.setName("LONG");
        base.setKind(Ttype.Kind.BASE);
        base.setSizeof(4);
        base.setOpersys(os);

        Ttype root = struct("Alpha", 4);
        Tdata d = new Tdata();
        d.setName("a");
        d.setId(2);
        d.setOffset(0);
        d.setOrdinal(0);
        d.setTtype(root);
        root.setData(new HashSet<>(List.of(d)));

        when(typeRepo.findByNameAndOpersys("Alpha", os)).thenReturn(List.of(root));
        when(typeRepo.findByIdInAndOpersys(any(), eq(os))).thenReturn(List.of(root, base));
        when(typeRepo.findByIdAndOpersys(2, os)).thenReturn(base);

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(LIST_URL + "/Alpha")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        String body = res.getResponse().getContentAsString();
        assertThat(body).contains("\"kind\":\"STRUCT\"");
        assertThat(body).contains("\"cSource\"");
        assertThat(body).doesNotContain("str-link");
        assertThat(body).doesNotContain("<a ");
        String etag = res.getResponse().getHeader("ETag");
        assertThat(etag).isNotBlank();

        var notModified = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(LIST_URL + "/Alpha").header("If-None-Match", etag)).andReturn();
        assertThat(notModified.getResponse().getStatus()).isEqualTo(304);
    }

    @Test
    void detailReturnsProblemDetail404ForUnknownType() throws Exception {
        when(typeRepo.findByNameAndOpersys("Nope", os)).thenReturn(List.of());

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(LIST_URL + "/Nope")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(404);
        assertThat(res.getResponse().getContentType()).isEqualTo("application/problem+json");
    }
}
