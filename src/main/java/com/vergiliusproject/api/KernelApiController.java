package com.vergiliusproject.api;

import com.vergiliusproject.entities.Os;
import com.vergiliusproject.repos.OsRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kernels")
public class KernelApiController {

    private final ApiOsResolver resolver;

    public KernelApiController(ApiOsResolver resolver) {
        this.resolver = resolver;
    }

    @GetMapping("/{arch}")
    public ResponseEntity<List<ApiDtos.FamilySummary>> families(@PathVariable String arch) {
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, ApiCache.VOLATILE)
                .body(ApiOsResolver.familiesOf(resolver.byArch(arch)));
    }

    @GetMapping("/{arch}/{familySlug}")
    public ResponseEntity<List<ApiDtos.KernelSummary>> builds(
            @PathVariable String arch,
            @PathVariable String familySlug) {
        List<ApiDtos.KernelSummary> body = resolver.byFamilySlug(arch, familySlug).stream()
                .sorted(Comparator.naturalOrder())
                .map(ApiDtos.KernelSummary::of)
                .toList();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, ApiCache.VOLATILE)
                .body(body);
    }

    @GetMapping("/{arch}/{familySlug}/{osSlug}")
    public ResponseEntity<?> build(
            @PathVariable String arch,
            @PathVariable String familySlug,
            @PathVariable String osSlug) {
        ApiOsResolver.Resolved resolved = resolver.byOsSlug(arch, familySlug, osSlug);
        Os os = resolved.os();
        if (resolved.redirected()) {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .location(java.net.URI.create(ApiOsResolver.canonicalPath(os)))
                    .build();
        }
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, ApiCache.BUILD_SCOPED)
                .eTag(ApiCache.etag(os.getId(), "build"))
                .body(ApiDtos.KernelSummary.of(os));
    }
}
