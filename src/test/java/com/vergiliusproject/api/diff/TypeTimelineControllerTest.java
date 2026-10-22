package com.vergiliusproject.api.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vergiliusproject.api.ApiExceptionHandler;
import com.vergiliusproject.api.ApiOsResolver;
import com.vergiliusproject.api.TypeLoader;
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

class TypeTimelineControllerTest {

    private OsRepository osRepo;
    private TtypeRepository typeRepo;
    private MockMvc mvc;
    private Os b1;
    private Os b2;
    private Os b3;

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

        b1 = os("1507", "1507", "10.0.10240.0");
        b2 = os("1607", "1607", "10.0.14393.0");
        b3 = os("21h2", "21h2", "10.0.22000.0");
    }

    private Os os(String osName, String osSlug, String build) {
        Os o = new Os();
        o.setFamilyName("Windows 10");
        o.setOsName(osName);
        o.setFamilySlug("windows-10");
        o.setOsSlug(osSlug);
        o.setBuildnumber(build);
        o.setArch("x64");
        o.setTimestamp(0L);
        return o;
    }

    private Ttype structType(Os os, String name, int sizeof, int aOffset) {
        Ttype t = new Ttype();
        t.setId(name.hashCode());
        t.setName(name);
        t.setKind(Ttype.Kind.STRUCT);
        t.setSizeof(sizeof);
        t.setOpersys(os);
        Tdata d = new Tdata();
        d.setName("A");
        d.setId("ULONG".hashCode() * 31);
        d.setOffset(aOffset);
        d.setOrdinal(0);
        d.setTtype(t);
        t.setData(new HashSet<>(List.of(d)));
        return t;
    }

    private Ttype baseType(Os os) {
        Ttype t = new Ttype();
        t.setId("ULONG".hashCode() * 31);
        t.setName("ULONG");
        t.setKind(Ttype.Kind.BASE);
        t.setSizeof(4);
        t.setOpersys(os);
        return t;
    }

    @Test
    void timelineAcrossRangeProducesTransitionsAndPseudocode() throws Exception {
        when(osRepo.findByArch("x64")).thenReturn(List.of(b1, b2, b3));

        Ttype t1 = structType(b1, "MYSTRUCT", 8, 0);
        Ttype t2 = structType(b2, "MYSTRUCT", 8, 4);  // A moved
        Ttype t3 = structType(b3, "MYSTRUCT", 8, 4);

        when(typeRepo.findByNameAndOpersys("MYSTRUCT", b1)).thenReturn(List.of(t1));
        when(typeRepo.findByNameAndOpersys("MYSTRUCT", b2)).thenReturn(List.of(t2));
        when(typeRepo.findByNameAndOpersys("MYSTRUCT", b3)).thenReturn(List.of(t3));
        when(typeRepo.findByIdInAndOpersys(any(), eq(b1))).thenReturn(List.of(t1, baseType(b1)));
        when(typeRepo.findByIdInAndOpersys(any(), eq(b2))).thenReturn(List.of(t2, baseType(b2)));
        when(typeRepo.findByIdInAndOpersys(any(), eq(b3))).thenReturn(List.of(t3, baseType(b3)));

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/types/MYSTRUCT/timeline")
                .queryParam("from", "x64/windows-10/1507")
                .queryParam("to", "x64/windows-10/21h2")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        String body = res.getResponse().getContentAsString();
        assertThat(body).contains("\"buildnumber\":\"10.0.14393.0\"");
        assertThat(body).contains("\"kind\":\"MOVED\"");
        assertThat(body).contains("evolution across 3 builds");
        assertThat(body).contains("// In 1607:");
        assertThat(body).contains("~ A moved +4 (now @0x4)");
        assertThat(body).contains("a = *(typeof(A)*)(base + 0x4);");
        assertThat(body).contains("a = *(typeof(A)*)(base + 0x0);");
        assertThat(res.getResponse().getHeader("ETag")).isNotBlank();
    }

    @Test
    void timelineWithExplicitBuildsList() throws Exception {
        when(osRepo.findByArchAndFamilySlugAndOsSlug("x64", "windows-10", "1507")).thenReturn(b1);
        when(osRepo.findByArchAndFamilySlugAndOsSlug("x64", "windows-10", "21h2")).thenReturn(b3);

        when(typeRepo.findByNameAndOpersys("MYSTRUCT", b1))
                .thenReturn(List.of(structType(b1, "MYSTRUCT", 8, 0)));
        when(typeRepo.findByNameAndOpersys("MYSTRUCT", b3))
                .thenReturn(List.of(structType(b3, "MYSTRUCT", 8, 0)));
        when(typeRepo.findByIdInAndOpersys(any(), eq(b1))).thenReturn(List.of());
        when(typeRepo.findByIdInAndOpersys(any(), eq(b3))).thenReturn(List.of());

        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/types/MYSTRUCT/timeline")
                .queryParam("builds", "x64/windows-10/1507,x64/windows-10/21h2")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        assertThat(res.getResponse().getContentAsString()).contains("No differences across the entire range");
    }

    @Test
    void timelineRejectsMixedArches() throws Exception {
        var res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/types/MYSTRUCT/timeline")
                .queryParam("from", "x64/windows-10/1507")
                .queryParam("to", "x86/windows-10/21h2")).andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(res.getResponse().getContentAsString()).contains("same architecture");
    }
}
