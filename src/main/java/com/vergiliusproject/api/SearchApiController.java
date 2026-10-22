package com.vergiliusproject.api;

import static com.vergiliusproject.api.ApiExceptionHandler.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vergiliusproject.api.ApiDtos.SearchHit;
import com.vergiliusproject.api.ApiDtos.SearchLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cross-build type-name search backed by the dump manifest's reverse index.
 * Read-only and cheap: the manifest is loaded lazily from disk and cached in
 * memory (immutable between regenerations). If the manifest has not been
 * generated yet, searches return empty results rather than erroring.
 */
@RestController
public class SearchApiController {

    private static final int MAX_HITS = 200;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path manifestPath;
    private volatile List<SearchHit> index;
    private volatile long loadedAt;

    public SearchApiController(@Value("${vergilius.dump.dir:api-dump}") String dumpDir) {
        this.manifestPath = Paths.get(dumpDir).toAbsolutePath().resolve("index.json");
    }

    @GetMapping("/api/v1/types/search")
    public ResponseEntity<List<SearchHit>> search(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            throw new BadRequestException("q must not be empty");
        }
        String needle = q.toLowerCase(Locale.US);

        List<SearchHit> hits = index().stream()
                .filter(h -> h.typeName().toLowerCase(Locale.US).contains(needle))
                .limit(MAX_HITS)
                .toList();

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, ApiCache.VOLATILE)
                .eTag(ApiCache.etag("search", needle, loadedAt))
                .body(hits);
    }

    List<SearchHit> index() {
        List<SearchHit> current = index;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (index == null) {
                index = loadIndex();
                loadedAt = System.currentTimeMillis();
            }
            return index;
        }
    }

    private List<SearchHit> loadIndex() {
        if (!Files.exists(manifestPath)) {
            return List.of();
        }
        try {
            JsonNode types = mapper.readTree(Files.readString(manifestPath)).get("types");
            if (types == null) {
                return List.of();
            }
            List<SearchHit> hits = new ArrayList<>();
            types.fieldNames().forEachRemaining(name -> {
                List<SearchLocation> locations = new ArrayList<>();
                for (JsonNode loc : types.get(name)) {
                    locations.add(new SearchLocation(
                            loc.get("arch").asText(),
                            null,
                            loc.get("family").asText(),
                            null,
                            loc.get("os").asText(),
                            loc.get("buildnumber").asText(),
                            loc.get("href").asText()));
                }
                hits.add(new SearchHit(name, null, locations));
            });
            hits.sort((a, b) -> a.typeName().compareTo(b.typeName()));
            return List.copyOf(hits);
        } catch (IOException e) {
            return List.of();
        }
    }

    void invalidateCache() {
        synchronized (this) {
            index = null;
        }
    }
}
