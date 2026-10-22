package com.vergiliusproject.api.diff;

import static com.vergiliusproject.api.ApiExceptionHandler.BadRequestException;

import com.vergiliusproject.api.ApiCache;
import com.vergiliusproject.api.ApiOsResolver;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.api.TypeLoader;
import com.vergiliusproject.api.TypeTreeBuilder;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.TtypeRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cross-build structure comparison: GET /api/v1/types/{name}/diff?from=...
 * &to=... where each side is arch/familySlug/osSlug. Returns the structural
 * change list plus generated pseudocode describing build-aware access.
 */
@RestController
@RequestMapping("/api/v1/types")
public class TypeDiffController {

    private final ApiOsResolver resolver;
    private final TtypeRepository typeRepo;
    private final TypeLoader typeLoader;

    public TypeDiffController(ApiOsResolver resolver, TtypeRepository typeRepo,
            TypeLoader typeLoader) {
        this.resolver = resolver;
        this.typeRepo = typeRepo;
        this.typeLoader = typeLoader;
    }

    @GetMapping("/{name}/diff")
    public ResponseEntity<Map<String, Object>> diff(
            @PathVariable String name,
            @RequestParam String from,
            @RequestParam String to) {

        BuildRef fromRef = parseRef(from, "from");
        BuildRef toRef = parseRef(to, "to");

        Os fromOs = resolveBuild(fromRef);
        Os toOs = resolveBuild(toRef);

        Loaded fromLoaded = loadType(fromOs, name);
        Loaded toLoaded = loadType(toOs, name);

        List<TypeDiffer.Change> changes = TypeDiffer.diff(fromLoaded.tree(), toLoaded.tree());

        DiffPseudocode.Context ctx = new DiffPseudocode.Context(
                buildLabel(fromOs), buildLabel(toOs), name,
                fromLoaded.size(), toLoaded.size());
        String pseudocode = DiffPseudocode.generate(ctx, changes);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", name);
        body.put("from", Map.of(
                "arch", fromOs.getArch(), "family", fromOs.getFamilySlug(),
                "os", fromOs.getOsSlug(), "buildnumber", fromOs.getBuildnumber(),
                "present", fromLoaded.tree() != null));
        body.put("to", Map.of(
                "arch", toOs.getArch(), "family", toOs.getFamilySlug(),
                "os", toOs.getOsSlug(), "buildnumber", toOs.getBuildnumber(),
                "present", toLoaded.tree() != null));
        body.put("sizeDelta", delta(fromLoaded.size(), toLoaded.size()));
        body.put("changes", changes);
        body.put("pseudocode", pseudocode);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, ApiCache.BUILD_SCOPED)
                .eTag(ApiCache.etag(fromOs.getId(), toOs.getId(), name))
                .body(body);
    }

    private record BuildRef(String arch, String family, String os) {}

    private static BuildRef parseRef(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("'" + label + "' must be arch/family/os");
        }
        String[] parts = value.split("/");
        if (parts.length != 3) {
            throw new BadRequestException(
                    "'" + label + "' must have exactly 3 segments: arch/family/os");
        }
        return new BuildRef(parts[0], parts[1], parts[2]);
    }

    private Os resolveBuild(BuildRef ref) {
        ApiOsResolver.Resolved resolved = resolver.byOsSlug(ref.arch(), ref.family(), ref.os());
        if (resolved.redirected()) {
            // old-name alias: retry with canonical slugs
            return resolver.byOsSlug(resolved.os().getArch(),
                    resolved.os().getFamilySlug(), resolved.os().getOsSlug()).os();
        }
        return resolved.os();
    }

    private record Loaded(TypeNode tree, Integer size) {}

    private Loaded loadType(Os os, String name) {
        List<Ttype> matches = typeRepo.findByNameAndOpersys(name, os);
        if (matches.isEmpty()) {
            return new Loaded(null, null);
        }
        Ttype type = matches.getFirst();
        String hrefBase = "/api/v1/kernels/" + os.getArch() + "/" + os.getFamilySlug()
                + "/" + os.getOsSlug() + "/types/";
        var closure = typeLoader.loadClosure(os, type.getId());
        TypeNode tree = new TypeTreeBuilder(closure, hrefBase).buildRoot(type);
        return new Loaded(tree, type.getSizeof());
    }

    private static Integer delta(Integer from, Integer to) {
        if (from == null || to == null) {
            return null;
        }
        return to - from;
    }

    private static String buildLabel(Os os) {
        return os.getArch() + "/" + os.getFamilySlug() + "/" + os.getOsSlug()
                + " (" + os.getBuildnumber() + ")";
    }
}
