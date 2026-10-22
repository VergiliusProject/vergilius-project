package com.vergiliusproject.api;

import static com.vergiliusproject.api.ApiExceptionHandler.BadRequestException;
import static com.vergiliusproject.api.ApiExceptionHandler.NotFoundException;

import com.vergiliusproject.FieldBuilder;
import com.vergiliusproject.api.ApiDtos.KernelSummary;
import com.vergiliusproject.api.ApiDtos.PageResponse;
import com.vergiliusproject.api.ApiDtos.TypeDetail;
import com.vergiliusproject.api.ApiDtos.TypeListItem;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.TtypeRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kernels/{arch}/{familySlug}/{osSlug}/types")
public class TypeApiController {

    private static final Set<String> VALID_KINDS = Set.of("struct", "union", "enum");

    private final ApiOsResolver resolver;
    private final TtypeRepository typeRepo;
    private final TypeLoader typeLoader;

    public TypeApiController(ApiOsResolver resolver, TtypeRepository typeRepo, TypeLoader typeLoader) {
        this.resolver = resolver;
        this.typeRepo = typeRepo;
        this.typeLoader = typeLoader;
    }

    /**
     * Paginated type list. Slices the second-level-cached per-build list in
     * memory (the same list every website page load uses). Bridge: replace
     * with a paged COUNT/LIMIT query if list items ever need field-level data
     * or p95 latency at cache hit exceeds 100ms.
     */
    @GetMapping
    public ResponseEntity<PageResponse<TypeListItem>> list(
            @PathVariable String arch,
            @PathVariable String familySlug,
            @PathVariable String osSlug,
            @RequestParam(required = false) List<String> kind,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        ApiOsResolver.Resolved resolved = resolver.byOsSlug(arch, familySlug, osSlug);
        if (resolved.redirected()) {
            return ResponseEntity.status(301)
                    .location(java.net.URI.create(ApiOsResolver.canonicalPath(resolved.os())
                            + "/types"))
                    .build();
        }
        Os os = resolved.os();
        ApiOsResolver.requireValidPaging(page, size);

        List<String> kinds = validateKinds(kind);

        List<TypeListItem> all = typeRepo.findStructEnumUnionByOpersys(os).stream()
                .filter(t -> kinds.isEmpty() || kinds.contains(t.getKind().toString().toLowerCase(Locale.US)))
                .filter(t -> search == null || search.isBlank()
                        || t.getName().toLowerCase(Locale.US).contains(search.toLowerCase(Locale.US)))
                .map(t -> TypeListItem.of(os, t))
                .toList();

        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        PageResponse<TypeListItem> body = new PageResponse<>(
                all.subList(from, to), page, size, all.size(),
                size == 0 ? 0 : (all.size() + size - 1) / size);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, ApiCache.BUILD_SCOPED)
                .eTag(ApiCache.etag(os.getId(), "types", kinds, search, page, size))
                .body(body);
    }

    @GetMapping("/{name}")
    public ResponseEntity<TypeDetail> detail(
            @PathVariable String arch,
            @PathVariable String familySlug,
            @PathVariable String osSlug,
            @PathVariable String name,
            @org.springframework.web.bind.annotation.RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        ApiOsResolver.Resolved resolved = resolver.byOsSlug(arch, familySlug, osSlug);
        if (resolved.redirected()) {
            return ResponseEntity.status(301)
                    .location(java.net.URI.create(ApiOsResolver.canonicalPath(resolved.os())
                            + "/types/" + name))
                    .build();
        }
        Os os = resolved.os();

        List<Ttype> matches = typeRepo.findByNameAndOpersys(name, os);
        if (matches.isEmpty()) {
            throw new NotFoundException(
                    "No type named '" + name + "' in kernel '" + os.getOsName() + "'");
        }
        Ttype type = matches.getFirst();

        String etag = ApiCache.etag(os.getId(), type.getIdtype());
        if (ApiCache.matches(ifNoneMatch, etag)) {
            return ResponseEntity.status(304)
                    .eTag(etag)
                    .header(HttpHeaders.CACHE_CONTROL, ApiCache.BUILD_SCOPED)
                    .build();
        }

        String hrefBase = "/api/v1/kernels/" + arch + "/" + familySlug + "/" + osSlug + "/types/";
        var closure = typeLoader.loadClosure(os, type.getId());
        var tree = new TypeTreeBuilder(closure, hrefBase).buildRoot(type);

        String cSource = renderCSource(type, os);

        KernelSummary summary = KernelSummary.of(os);
        Map<String, String> links = new java.util.LinkedHashMap<>();
        links.put("kernel", summary.href());
        links.put("self", hrefBase + type.getName());

        TypeDetail body = new TypeDetail(
                type.getName(), type.getKind().toString(), type.getSizeof(),
                type.isIsConst(), type.isIsVolatile(), tree, cSource, links);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, ApiCache.BUILD_SCOPED)
                .eTag(etag)
                .body(body);
    }

    private String renderCSource(Ttype type, Os os) {
        String rendered = FieldBuilder.recursionProcessing(typeRepo, type, 0, 0, "/", os).toString();
        // FieldBuilder embeds HTML anchors for named-type references; strip
        // them with the same regex the renderer itself uses for column math.
        return rendered.replaceAll("(<.+?>)", "");
    }

    private static List<String> validateKinds(List<String> kind) {
        if (kind == null) {
            return List.of();
        }
        for (String k : kind) {
            if (!VALID_KINDS.contains(k.toLowerCase(Locale.US))) {
                throw new BadRequestException(
                        "Invalid kind '" + k + "'; valid values: struct, union, enum");
            }
        }
        return kind.stream().map(k -> k.toLowerCase(Locale.US)).toList();
    }
}
