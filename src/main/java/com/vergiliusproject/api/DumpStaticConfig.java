package com.vergiliusproject.api;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves generated dump documents as static files under /api/ (index.json and
 * os/{arch}/{osSlug}.json) so bulk consumers can fetch complete per-build
 * documents from the CDN/mirror without hitting live endpoints. The single
 * /api/** pattern is safe: controller mappings outrank resource handlers, so
 * /api/v1/** still routes to the API controllers.
 */
@Configuration
public class DumpStaticConfig implements WebMvcConfigurer {

    private final String dumpDir;

    public DumpStaticConfig(@Value("${vergilius.dump.dir:api-dump}") String dumpDir) {
        this.dumpDir = dumpDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(dumpDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/api/**")
                .addResourceLocations(location);
    }
}
