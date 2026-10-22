package com.vergiliusproject.api;

import static com.vergiliusproject.api.ApiExceptionHandler.BadRequestException;
import static com.vergiliusproject.api.ApiExceptionHandler.NotFoundException;

import com.vergiliusproject.entities.Os;
import com.vergiliusproject.repos.OsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Resolves Os entities from canonical slugs, falling back to old names for redirects. */
@Component
public class ApiOsResolver {

    public record Resolved(Os os, boolean redirected) {}

    private final OsRepository osRepo;

    public ApiOsResolver(OsRepository osRepo) {
        this.osRepo = osRepo;
    }

    public List<Os> byArch(String arch) {
        List<Os> builds = osRepo.findByArch(arch);
        if (builds.isEmpty()) {
            throw new NotFoundException("No kernels found for arch '" + arch + "'");
        }
        return builds;
    }

    public List<Os> byFamilySlug(String arch, String familySlug) {
        List<Os> builds = osRepo.findByArchAndFamilySlug(arch, familySlug);
        if (builds.isEmpty()) {
            throw new NotFoundException(
                    "No kernels found for arch '" + arch + "' and family '" + familySlug + "'");
        }
        return builds;
    }

    /**
     * Resolves one build by slugs; if that fails, tries the old family/os names
     * and reports a redirect to the caller so it can issue a 301.
     */
    public Resolved byOsSlug(String arch, String familySlug, String osSlug) {
        Os os = osRepo.findByArchAndFamilySlugAndOsSlug(arch, familySlug, osSlug);
        if (os != null) {
            return new Resolved(os, false);
        }

        List<Os> oldMatches = osRepo.findByArchAndOldFamilyName(arch, familySlug).stream()
                .filter(o -> osSlug.equals(o.getOldOsName()))
                .toList();
        if (oldMatches.size() == 1) {
            return new Resolved(oldMatches.getFirst(), true);
        }
        throw new NotFoundException("No kernel found for '" + arch + "/" + familySlug + "/" + osSlug + "'");
    }

    public static String canonicalPath(Os os) {
        return "/api/v1/kernels/" + os.getArch() + "/" + os.getFamilySlug() + "/" + os.getOsSlug();
    }

    public static List<ApiDtos.FamilySummary> familiesOf(List<Os> builds) {
        Map<String, List<Os>> byFamily = builds.stream()
                .collect(Collectors.groupingBy(
                        Os::getFamilyName, LinkedHashMap::new, Collectors.toList()));
        return byFamily.entrySet().stream()
                .map(e -> new ApiDtos.FamilySummary(
                        e.getKey(), e.getValue().getFirst().getFamilySlug(), e.getValue().size()))
                .toList();
    }

    public static void requireValidPaging(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        if (size < 1 || size > 200) {
            throw new BadRequestException("size must be between 1 and 200");
        }
    }
}
