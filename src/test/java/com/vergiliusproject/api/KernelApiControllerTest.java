package com.vergiliusproject.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vergiliusproject.entities.Os;
import com.vergiliusproject.repos.OsRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class KernelApiControllerTest {

    private OsRepository osRepo;
    private MockMvc mvc;

    private Os os(String family, String osName, String oldFamily, String oldOs) {
        Os o = new Os();
        o.setFamilyName(family);
        o.setOsName(osName);
        o.setOldFamilyName(oldFamily);
        o.setOldOsName(oldOs);
        o.setBuildnumber("10.0.1.0");
        o.setArch("x64");
        o.setTimestamp(0L);
        return o;
    }

    @BeforeEach
    void setUp() {
        osRepo = mock(OsRepository.class);
        KernelApiController controller = new KernelApiController(new ApiOsResolver(osRepo));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void familiesReturns200WithGroupedFamilies() throws Exception {
        Os a = os("Windows 10", "1607", null, null);
        Os b = os("Windows 10", "1703", null, null);
        Os c = os("Windows 11", "21H2", null, null);
        when(osRepo.findByArch("x64")).thenReturn(List.of(a, b, c));

        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/kernels/x64")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString())
                .contains("\"family\":\"Windows 10\"")
                .contains("\"buildCount\":2")
                .contains("Windows 11");
    }

    @Test
    void familiesReturnsProblemDetail404ForUnknownArch() throws Exception {
        when(osRepo.findByArch("arm")).thenReturn(List.of());

        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/kernels/arm")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentType()).isEqualTo("application/problem+json");
        assertThat(result.getResponse().getContentAsString()).contains("Not found");
    }

    @Test
    void buildsReturnsSortedSummaries() throws Exception {
        Os v2 = os("Windows 10", "1703", null, null);
        v2.setBuildnumber("10.0.2.0");
        Os v1 = os("Windows 10", "1607", null, null);
        when(osRepo.findByArchAndFamilySlug(eq("x64"), any())).thenReturn(List.of(v2, v1));

        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/kernels/x64/windows-10")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        String body = result.getResponse().getContentAsString();
        assertThat(body.indexOf("1607")).isLessThan(body.indexOf("1703"));
    }

    @Test
    void buildReturns301ForOldNames() throws Exception {
        Os renamed = os("Windows 10", "1607", "Windows 10 (old)", "1607 (old)");
        when(osRepo.findByArchAndFamilySlugAndOsSlug("x64", "Windows 10 (old)", "1607 (old)"))
                .thenReturn(null);
        when(osRepo.findByArchAndOldFamilyName("x64", "Windows 10 (old)"))
                .thenReturn(List.of(renamed));

        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/kernels/x64/Windows 10 (old)/1607 (old)")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.MOVED_PERMANENTLY.value());
        assertThat(result.getResponse().getHeader("Location"))
                .startsWith("/api/v1/kernels/x64/");
    }

    @Test
    void buildReturns200WithEtagForCanonicalSlug() throws Exception {
        Os canonical = os("Windows 10", "1607", null, null);
        when(osRepo.findByArchAndFamilySlugAndOsSlug("x64", "windows-10", "1607"))
                .thenReturn(canonical);

        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/kernels/x64/windows-10/1607")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("ETag")).isNotBlank();
        assertThat(result.getResponse().getContentAsString()).contains("\"os\":\"1607\"");
    }
}
