package com.vergiliusproject.api.dump;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vergiliusproject.api.ApiCache;
import com.vergiliusproject.api.CDeclRenderer;
import com.vergiliusproject.api.TypeLoader;
import com.vergiliusproject.api.TypeTreeBuilder;
import com.vergiliusproject.api.ApiDtos.TypeNode;
import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Ttype;
import com.vergiliusproject.repos.OsRepository;
import com.vergiliusproject.repos.TtypeRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Generates the static API dump: one self-contained JSON document per OS
 * build (complete type set with resolved nesting and a usedIn reverse index)
 * plus an index.json manifest with a per-type-name reverse index across
 * builds. Output is deterministic (sorted everywhere) so two runs are
 * byte-identical. Anon ids are scoped per type entry - REF nodes only point
 * at named types, so per-type scoping is self-consistent.
 */
@Component
public class DumpGenerator {

    static final int FORMAT = 1;

    private final OsRepository osRepo;
    private final TtypeRepository typeRepo;
    private final TypeLoader typeLoader;
    // self-contained mapper: deterministic output independent of web config
    private final ObjectMapper mapper = new ObjectMapper();

    public DumpGenerator(OsRepository osRepo, TtypeRepository typeRepo,
            TypeLoader typeLoader) {
        this.osRepo = osRepo;
        this.typeRepo = typeRepo;
        this.typeLoader = typeLoader;
    }

    public Path generateAll(Path outDir) throws IOException {
        Files.createDirectories(outDir.resolve("os"));

        List<Os> builds = new ArrayList<>();
        osRepo.findAll().forEach(builds::add);
        builds.sort(Comparator.comparing(Os::getArch).thenComparing(Comparator.naturalOrder()));

        Map<String, Object> typeIndex = new TreeMap<>();
        List<Map<String, Object>> manifestBuilds = new ArrayList<>();

        for (Os os : builds) {
            manifestBuilds.add(generateBuild(os, outDir, typeIndex));
        }

        Map<String, Object> manifest = new TreeMap<>();
        manifest.put("format", FORMAT);
        manifest.put("builds", manifestBuilds);
        manifest.put("types", typeIndex);
        writeJson(outDir.resolve("index.json"), manifest);

        return outDir;
    }

    public Map<String, Object> generateBuild(Os os, Path outDir, Map<String, Object> typeIndex) throws IOException {
        List<Ttype> types = typeRepo.findStructEnumUnionByOpersys(os).stream()
                .sorted(Comparator.comparing(Ttype::getName))
                .toList();

        String hrefBase = "/api/v1/kernels/" + os.getArch() + "/" + os.getFamilySlug()
                + "/" + os.getOsSlug() + "/types/";

        List<Map<String, Object>> typeEntries = new ArrayList<>();
        Map<String, List<String>> usedIn = new TreeMap<>();

        for (Ttype type : types) {
            var closure = typeLoader.loadClosure(os, type.getId());
            TypeNode tree = new TypeTreeBuilder(closure, hrefBase).buildRoot(type);
            String cSource = CDeclRenderer.render(tree);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", type.getName());
            entry.put("kind", type.getKind().toString());
            entry.put("size", type.getSizeof());
            entry.put("const", type.isIsConst());
            entry.put("volatile", type.isIsVolatile());
            entry.put("tree", tree);
            entry.put("cSource", cSource);
            typeEntries.add(entry);

            for (String refName : collectRefs(tree, new TreeSet<>())) {
                usedIn.computeIfAbsent(refName, k -> new ArrayList<>()).add(type.getName());
            }
        }

        Map<String, Object> doc = new TreeMap<>();
        doc.put("format", FORMAT);
        doc.put("arch", os.getArch());
        doc.put("family", os.getFamilyName());
        doc.put("os", os.getOsName());
        doc.put("buildnumber", os.getBuildnumber());
        doc.put("timestamp", os.getTimestamp());
        doc.put("buildDate", os.convertTimestampToDate(os.getTimestamp()));
        doc.put("slugs", Map.of("family", os.getFamilySlug(), "os", os.getOsSlug()));
        doc.put("types", typeEntries);
        doc.put("usedIn", usedIn);

        String relative = "os/" + os.getArch() + "/" + os.getOsSlug() + ".json";
        writeJson(outDir.resolve(relative), doc);

        Map<String, Object> manifestEntry = new TreeMap<>();
        manifestEntry.put("arch", os.getArch());
        manifestEntry.put("family", os.getFamilyName());
        manifestEntry.put("os", os.getOsName());
        manifestEntry.put("buildnumber", os.getBuildnumber());
        manifestEntry.put("timestamp", os.getTimestamp());
        manifestEntry.put("slugs", Map.of("family", os.getFamilySlug(), "os", os.getOsSlug()));
        manifestEntry.put("file", "/api/" + relative);
        manifestEntry.put("href", "/api/v1/kernels/" + os.getArch() + "/" + os.getFamilySlug()
                + "/" + os.getOsSlug());

        for (Ttype type : types) {
            indexEntriesFor(typeIndex, type.getName()).add(Map.of(
                            "arch", os.getArch(),
                            "family", os.getFamilySlug(),
                            "os", os.getOsSlug(),
                            "buildnumber", os.getBuildnumber(),
                            "file", "/api/" + relative,
                            "href", hrefBase + type.getName()));
        }

        return manifestEntry;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> indexEntriesFor(
            Map<String, Object> typeIndex, String name) {
        return (List<Map<String, Object>>) typeIndex
                .computeIfAbsent(name, k -> new ArrayList<Map<String, Object>>());
    }

    static TreeSet<String> collectRefs(TypeNode node, TreeSet<String> acc) {
        if (node == null) {
            return acc;
        }
        if ("REF".equals(node.kind()) && node.name() != null) {
            acc.add(node.name());
        }
        if (node.target() != null) {
            collectRefs(node.target(), acc);
        }
        if (node.element() != null) {
            collectRefs(node.element(), acc);
        }
        if (node.returnType() != null) {
            collectRefs(node.returnType(), acc);
        }
        if (node.parameters() != null) {
            node.parameters().forEach(p -> collectRefs(p, acc));
        }
        if (node.fields() != null) {
            node.fields().forEach(f -> collectRefs(f.type(), acc));
        }
        return acc;
    }

    private void writeJson(Path target, Object value) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, mapper.writeValueAsString(value));
    }
}
