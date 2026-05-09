package com.sopannikam.cognistream.services.indexing;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CodeFileFilterTests {
    private final CodeFileFilter filter = new CodeFileFilter();

    @Test
    void excludesGeneratedFilesSecretsBinariesAndOversizeFiles() {
        assertThat(filter.isEligible("node_modules/lib/index.js", 10, 100)).isFalse();
        assertThat(filter.isEligible(".env", 10, 100)).isFalse();
        assertThat(filter.isEligible("target/classes/App.class", 10, 100)).isFalse();
        assertThat(filter.isEligible("package-lock.json", 10, 100)).isFalse();
        assertThat(filter.isEligible("images/photo.png", 10, 100)).isFalse();
        assertThat(filter.isEligible("src/App.java", 101, 100)).isFalse();
        assertThat(filter.isEligible("src/App.java", 100, 100)).isTrue();
        assertThat(filter.isEligible("Dockerfile", 10, 100)).isTrue();
    }
}
