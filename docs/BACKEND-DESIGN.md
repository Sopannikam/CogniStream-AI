# Backend design walkthrough

CogniStream is a Spring Boot repository-analysis service. The React client demonstrates its APIs and contains no indexing or model-provider logic.

Follow `RepoController` through `RepoService` and `IndexingService` to persistence and external adapters. For chat, follow `ChatController`, `ChatService`, `CodeContextRetriever`, `ChatPromptBuilder`, and `ChatStreamHandler`.

## Decisions and trade-offs

| Decision | Reason and trade-off |
|---|---|
| Spring MVC and layered services | Clear HTTP boundaries and testable business rules. Blocking indexing runs on a separate executor. |
| Session-based GitHub OAuth | Spring Security manages login. GitHub tokens remain encrypted on the server. |
| Per-user repository records | A unique user/repository pair prevents duplicate imports within one workspace. |
| Row lock at indexing admission | Competing requests cannot both admit a job for one repository. Slow external calls run after the transaction commits. |
| Bounded executor | Core 2 workers, maximum 4, queue capacity 50. Rejected submissions return 503 and record a retryable failure. |
| Four-chunk embedding batches | Smaller local inference requests limit resource pressure, at the cost of more calls. |
| PostgreSQL and pgvector | Metadata, conversations, and vectors share one database. HNSW and cosine distance support approximate semantic retrieval. |
| Repository filter on vector queries | Only the authorized repository's chunks enter the prompt. |
| POST with SSE | Questions need a body and answers need streaming. The client uses credentialed fetch rather than EventSource. |
| Local models | No paid API key is required. Latency and answer quality depend on hardware and model size. |

## Index lifecycle

PENDING becomes INDEXING, then READY or FAILED. A retry deletes previous vectors and rebuilds the index. Chat is blocked until READY, including during re-indexing. Partial vectors can remain after failure, but are unavailable for chat and deleted on retry.

The executor queue is not durable. A crash can leave INDEXING records without a worker. Restart recovery, cancellation, and resumable jobs are not implemented. A durable queue and persisted job records would be a next step for multi-instance operation.

## Error behavior

Invalid bodies, IDs, and field limits return 400. Missing authentication returns 401. Inaccessible repository/session IDs return 404. A saturated indexing queue returns 503 and records FAILED. Unexpected failures are logged server-side with a generic 500 response. Worker errors are available through the indexing-status endpoint.

Questions are capped at 8,000 characters and titles at 200. Validation limits do not guarantee that every question and retrieved context fits the model window.

## Test boundaries

Tests use H2 and mocked GitHub/model/vector services. They cover token encryption, protected routes, cross-user access, validation, imports, sync persistence, competing index requests, queue rejection, bounded batches, and failure states. Locking is exercised in H2; PostgreSQL concurrency/load tests remain separate work.

## Current limitations

- Local models can invent details. Citations are retrieved source files, not proof of answer correctness.
- Previous chat messages are displayed but are not added to the next prompt.
- Exact line ranges are not tracked in chunk metadata.
- Branches are indexed without pinning the complete job to a commit SHA, so branch changes can produce mixed snapshots.
- Individual file-fetch failures can be skipped; completion does not guarantee every eligible file was fetched.
- There is no automatic resync, durable queue, multi-instance coordination, or conversation deletion API.
- Local-development configuration uses JPA schema updates and disabled CSRF protection. Hosting publicly requires explicit CSRF, TLS/cookie, secret-management, and migration configuration.

## Project explanation

I built a Spring Boot service that imports GitHub repositories, indexes code asynchronously, stores embeddings in PostgreSQL with pgvector, and streams answers grounded in retrieved code. I used transactional admission, ownership checks, validation, and failure-state tracking. A small React client demonstrates the APIs.

Be ready to explain an embedding failure, simultaneous index requests, a cross-user UUID request, and the difference between a streamed answer and a normal JSON response. Use the request examples and tests to demonstrate these behaviors.
