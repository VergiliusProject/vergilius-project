package com.vergiliusproject.api.diff;

import static com.vergiliusproject.api.ApiExceptionHandler.BadRequestException;
import static com.vergiliusproject.api.ApiExceptionHandler.NotFoundException;

import com.vergiliusproject.entities.Os;
import com.vergiliusproject.repos.OsRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Resolves an ordered range of builds for timeline diffs: every build of the
 * reference architecture whose build number falls between the `from` and `to`
 * builds (inclusive), sorted ascending. `from`/`to` are arch/familySlug/osSlug
 * refs; old-name aliases resolve to canonical builds. An explicit comma
 * separated builds= list bypasses range resolution.
 */
@Component
public class BuildRangeResolver {

    private final OsRepository osRepo;

    public BuildRangeResolver(OsRepository osRepo) {
        this.osRepo = osRepo;
    }

    public List<Os> resolveRange(String fromRef, String toRef) {
        String[] fromParts = splitRef(fromRef, "from");
        String[] toParts = splitRef(toRef, "to");

        if (!fromParts[0].equals(toParts[0])) {
            throw new BadRequestException(
                    "'from' and 'to' must target the same architecture");
        }
        String arch = fromParts[0];

        List<Os> builds = new ArrayList<>(osRepo.findByArch(arch));
        if (builds.isEmpty()) {
            throw new NotFoundException("No kernels found for arch '" + arch + "'");
        }
        builds.sort(Comparator.naturalOrder());

        int fromIdx = indexOf(builds, fromParts);
        int toIdx = indexOf(builds, toParts);
        if (fromIdx > toIdx) {
            int tmp = fromIdx;
            fromIdx = toIdx;
            toIdx = tmp;
        }
        return builds.subList(fromIdx, toIdx + 1);
    }

    public List<Os> resolveExplicit(List<String> refs) {
        List<Os> result = new ArrayList<>();
        for (String ref : refs) {
            String[] parts = splitRef(ref, "builds");
            Os os = osRepo.findByArchAndFamilySlugAndOsSlug(parts[0], parts[1], parts[2]);
            if (os == null) {
                os = osRepo.findByArchAndOldFamilyName(parts[0], parts[1]).stream()
                        .filter(o -> parts[2].equals(o.getOldOsName()))
                        .findFirst().orElse(null);
            }
            if (os == null) {
                throw new NotFoundException("No kernel found for '" + ref + "'");
            }
            result.add(os);
        }
        if (result.size() < 2) {
            throw new BadRequestException("builds= needs at least 2 entries");
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private static String[] splitRef(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("'" + label + "' must be arch/family/os");
        }
        String[] parts = value.split("/");
        if (parts.length != 3) {
            throw new BadRequestException(
                    "'" + label + "' must have exactly 3 segments: arch/family/os");
        }
        return parts;
    }

    private int indexOf(List<Os> builds, String[] parts) {
        for (int i = 0; i < builds.size(); i++) {
            Os o = builds.get(i);
            boolean canonical = parts[1].equals(o.getFamilySlug()) && parts[2].equals(o.getOsSlug());
            boolean alias = parts[1].equals(o.getOldFamilyName()) && parts[2].equals(o.getOldOsName());
            if (canonical || alias) {
                return i;
            }
        }
        throw new NotFoundException("No kernel found for '"
                + parts[0] + "/" + parts[1] + "/" + parts[2] + "'");
    }
}
