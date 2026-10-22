package com.vergiliusproject.api.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.vergiliusproject.api.ApiExceptionHandler;
import com.vergiliusproject.api.ApiOsResolver;
import com.vergiliusproject.api.TypeLoader;

class TypeDiffControllerTest {

    private static final String URL = "/api/v1/types/MYSTRUCT/diff";

    private OsRepository osRepo;
    private TtypeRepository typeRepo;
    private MockMvc mvc;
    private Os osA;
    private Os osB;

    @BeforeEach
    void setUp() {
        osRepo = mock(OsRepository.class);
        typeRepo = mock(TtypeRepository.class);
        TypeDiffController controller = new TypeDiffController(
                new ApiOsResolver(osRepo), new BuildRangeResolver(osRepo),
                typeRepo, new TypeLoader(typeRepo));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        osA = os("Windows 10", "1607", "windows-10", "1607", "10.0.14393.0");
        osB = os("Windows 11", "21H2", "windows-11", "21h2", "10.0.22000.0");
    }

    private Os os(String family, String osName, String familySlug, String osSlug, String build) {
        Os o = new Os();
        o.setFamilyName(family);
        o.setOsName(osName);
        o.setFamilySlug(familySlug);
        o.setOsSlug(osSlug);
        o.setBuildnumber(build);
        o.setArch("x64");
        o.setTimestamp(0L);
        return o;
    }

    private Ttype structType(Os os, String name, int sizeof, int fieldRefId) {
        Ttype t = new Ttype();
        t.setId(name.hashCode());
        t.setName(name);
        t.setKind(Ttype.Kind.STRUCT);
        t.setSizeof(sizeof);
        t.setOpersys(os);
        Tdata d = new Tdata();
        d.setName("A");
        d.setId(fieldRefId);
        d.setOffset(0);
        d.setOrdinal(0);
        d.setTtype(t);
        t.setData(new HashSet<>(List.of(d)));
        return t;
    }

    private Ttype baseType(Os os, String name, int sizeof) {
        Ttype t = new Ttype();
        t.setId(name.hashCode() * 31);
        t.setName(name);
        t.setKind(Ttype.Kind.BASE);
        t.setSizeof(sizeof);
        t.setOpersys(os);
        return t;
    }

    @Test
    void diffReturnsChangesAndPseudocode() throws Exception {
        when(osRepo.findByArchAndFamilySlugAndOsSlug("x64", "windows-10", "1607")).thenReturn(osA);
        when(osRepo.findByArchAndFamilySlugAndOsSlug("x64", "windows-11", "21h2")).thenReturn(osB);

        Ttype fromStruct = structType(osA, "MYSTRUCT", 8, "ULONG".hashCode() * 31);
        Ttype fromBase = baseType(osA, "ULONG", 4);
        // version B: field A moved to 0x8 (struct grew)
        Ttype toStruct = structType(osB, "MYSTRUCT", 16, "ULONG".hashCode() * 31);
        Tdata moved = toStruct.getData().iterator().next();
        moved.setOffset(8);
        Ttype toBase = baseType(osB, "ULONG", 4);

        when(typeRepo.findByNameAndOpersys("MYSTRUCT", osA)).thenReturn(List.of(fromStruct));
        when(typeRepo.findByNameAndOpersys("MYSTRUCT", osB)).thenReturn(List.of(toStruct));
        when(typeRepo.findByIdInAndOpersys(any(), eq(osA)))
                .thenReturn(List.of(fromStruct, fromBase));
        when(typeRepo.findByIdInAndOpersys(any(), eq(osB)))
                .thenReturn(List.of(toStruct, toBase));

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(URL)
                .queryParam("from", "x64/windows-10/1607")
                .queryParam("to", "x64/windows-11/21h2")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        String body = res.getResponse().getContentAsString();
        assertThat(body).contains("\"type\":\"MYSTRUCT\"");
        assertThat(body).contains("\"kind\":\"MOVED\"");
        assertThat(body).contains("\"offsetDelta\":8");
        assertThat(body).contains("pseudocode");
        assertThat(body).contains("if (build == x64/windows-11/21h2");
        assertThat(body).contains("*(typeof(A)*)(base + 0x8)");
        assertThat(res.getResponse().getHeader("ETag")).isNotBlank();
    }

    @Test
    void diffSurfaces404ForUnknownBuild() throws Exception {
        when(osRepo.findByArchAndFamilySlugAndOsSlug("x64", "nope", "nope")).thenReturn(null);

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(URL)
                .queryParam("from", "x64/nope/nope")
                .queryParam("to", "x64/windows-11/21h2")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(404);
        assertThat(res.getResponse().getContentType()).isEqualTo("application/problem+json");
    }

    @Test
    void diffRejectsMalformedRef() throws Exception {
        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(URL)
                .queryParam("from", "x64/windows-10")
                .queryParam("to", "x64/windows-11/21h2")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(res.getResponse().getContentAsString()).contains("3 segments");
    }
}
