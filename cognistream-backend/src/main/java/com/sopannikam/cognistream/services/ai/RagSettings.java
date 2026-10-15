package com.sopannikam.cognistream.services.ai;

public final class RagSettings {
     /** How many code chunks to fetch from the vector database per question. */
    public static final int TOP_K_CHUNKS = 8;

    /** Max time (ms) to keep an SSE stream open while the model is responding. */
    public static final long STREAM_TIMEOUT_MS = 300_000L;

    /** Metadata key stored on each embedded document (must match {@link com.sopannikam.cognistream.services.indexing.CodeChunker}). */
    public static final String METADATA_REPO_ID = "repoId";

    private RagSettings() {
    }
}
