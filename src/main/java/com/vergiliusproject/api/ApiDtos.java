package com.vergiliusproject.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Ttype;
import java.util.List;
import java.util.Map;

/**
 * DTO records for the /api/v1 read-only API. Entities are never serialized
 * directly: Ttype.data is a lazy Set (N+1 hazard), Os leaks internals.
 */
public final class ApiDtos {
    private ApiDtos() {}

    public record KernelSummary(
            String arch,
            String family,
            String familySlug,
            String os,
            String osSlug,
            String buildnumber,
            String buildDate,
            String href) {
        public static KernelSummary of(Os os) {
            String base = "/api/v1/kernels/" + os.getArch() + "/" + os.getFamilySlug() + "/" + os.getOsSlug();
            return new KernelSummary(os.getArch(), os.getFamilyName(), os.getFamilySlug(),
                    os.getOsName(), os.getOsSlug(), os.getBuildnumber(),
                    os.convertTimestampToDate(os.getTimestamp()), base);
        }
    }

    public record FamilySummary(String family, String familySlug, int buildCount) {}

    /** One type node in a resolved type tree. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TypeNode(
            String kind,            // BASE|POINTER|ARRAY|FUNCTION|STRUCT|UNION|ENUM|REF
            String name,            // null for anonymous nodes
            Integer size,           // sizeof in bytes where meaningful
            Boolean isConst,
            Boolean isVolatile,
            String anonId,          // "anon:N" for anonymous nodes in dump documents
            String href,            // REF nodes only
            Boolean truncated,      // true only when the node budget backstop fired
            // kind-specific payload
            Integer offset,         // field offset in bytes (fields only)
            Integer bitWidth,       // bitfield width
            String base,            // BASE: base type name
            TypeNode target,        // POINTER
            Integer count,          // ARRAY: element count
            TypeNode element,       // ARRAY
            TypeNode returnType,    // FUNCTION
            List<TypeNode> parameters, // FUNCTION
            List<FieldNode> fields, // STRUCT/UNION
            List<EnumConstant> constants, // ENUM
            String typeName) {      // REF: referenced type name (== name)

        public static TypeNode ref(String name, String href) {
            return new TypeNode("REF", name, null, null, null, null, href, null,
                    null, null, null, null, null, null, null, null, null, null, name);
        }
    }

    public record FieldNode(String name, Integer offset, Integer bitWidth, TypeNode type) {}

    public record EnumConstant(String name, long value) {}

    public record TypeListItem(
            String name,
            String kind,
            Integer size,
            String href) {
        public static TypeListItem of(Os os, Ttype t) {
            String href = "/api/v1/kernels/" + os.getArch() + "/" + os.getFamilySlug() + "/"
                    + os.getOsSlug() + "/types/" + t.getName();
            return new TypeListItem(t.getName(), t.getKind().toString(), t.getSizeof(), href);
        }
    }

    public record TypeDetail(
            String name,
            String kind,
            Integer size,
            boolean isConst,
            boolean isVolatile,
            TypeNode tree,
            String cSource,
            Map<String, String> links) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {}

    public record SearchHit(String typeName, String kind, List<SearchLocation> locations) {}

    public record SearchLocation(String arch, String family, String familySlug, String os,
            String osSlug, String buildnumber, String href) {}
}
