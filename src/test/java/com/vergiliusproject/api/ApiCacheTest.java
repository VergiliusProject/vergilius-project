package com.vergiliusproject.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiCacheTest {

    @Test
    void sameInputsGiveSameEtag() {
        String a = ApiCache.etag("uuid-1", 42, "page=0");
        String b = ApiCache.etag("uuid-1", 42, "page=0");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentInputsGiveDifferentEtags() {
        assertThat(ApiCache.etag("uuid-1", 1, "page=0"))
                .isNotEqualTo(ApiCache.etag("uuid-1", 2, "page=0"));
        assertThat(ApiCache.etag("uuid-1", 1, "page=0"))
                .isNotEqualTo(ApiCache.etag("uuid-1", 1, "page=1"));
    }

    @Test
    void matchesHandlesIfNoneMatchHeaderSemantics() {
        String etag = ApiCache.etag("uuid-1", 1, "page=0");
        assertThat(ApiCache.matches(null, etag)).isFalse();
        assertThat(ApiCache.matches("", etag)).isFalse();
        assertThat(ApiCache.matches(etag, etag)).isTrue();
        assertThat(ApiCache.matches("\"other\", " + etag, etag)).isTrue();
        assertThat(ApiCache.matches("*", etag)).isTrue();
        assertThat(ApiCache.matches("\"other\"", etag)).isFalse();
    }
}
