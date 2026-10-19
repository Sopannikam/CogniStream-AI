# Backend API

All `/api/repos/**` and `/api/chat/**` routes require the GitHub-authenticated session cookie and enforce repository/session ownership.

| Method | Route | Purpose |
|---|---|---|
| GET | `/oauth2/authorization/github` | Begin GitHub login |
| GET | `/api/auth/login-url` | Login endpoint information |
| GET | `/api/auth/me` | Signed-in profile; no access token |
| POST | `/api/auth/logout` | End session, 204 |
| GET | `/api/repos?refresh=true` | Sync GitHub repository metadata |
| GET | `/api/repos?refresh=false` | List stored repository metadata |
| POST | `/api/repos/import` | Import a public GitHub repository URL |
| GET | `/api/repos/{id}` | Repository details |
| POST | `/api/repos/{id}/index` | Start asynchronous indexing, 202 |
| GET | `/api/repos/{id}/status` | Progress, chunk count, and error |
| POST | `/api/chat/sessions` | Create a session for a READY repository |
| GET | `/api/chat/sessions?repositoryId={id}` | List owned sessions |
| GET | `/api/chat/sessions/{id}` | Session messages and citations |
| POST | `/api/chat/sessions/{id}/messages` | Send question and stream response |
| GET | `/actuator/health` | Basic health status |

Import body: `{"url":"https://github.com/owner/repository"}`. Import requires a public repository and preserves an existing index when the same repository is added again. Use the normal index endpoint afterward. Imported repositories stay in the stored list after Sync.

Create-session body: `{"repositoryId":"uuid","title":"Optional title"}`.
Send-message body: `{"content":"Where is authentication implemented?"}`.

The streaming endpoint emits `user_message`, JSON-string `token`, `assistant_message` with citations, `done`, or `error`. The frontend uses credentialed `fetch` with a readable stream because the endpoint is POST. It does not use EventSource.

## Contract and request examples

Import [openapi.json](openapi.json) into Postman or an OpenAPI viewer. [requests.http](requests.http) contains examples for authentication lookup, imports, indexing, progress, and streaming chat. Sign in through GitHub first and supply your own `COGNISTREAM_SESSION` cookie locally. The browser and API client do not share a cookie jar.

For command-line streaming, use a client with output buffering disabled, such as `curl -N`. The question request must include the backend session cookie and a JSON body with `content`. No model credentials belong in client requests.

## Validation and errors

Import URLs are required and limited to 500 characters. Questions are nonblank and limited to 8,000 characters. Optional chat titles are limited to 200 characters. Invalid JSON and malformed UUIDs return 400. An already-indexing repository returns 400. A saturated indexing executor marks the attempted job FAILED and returns 503 so it can be retried.

Application JSON errors contain `status`, `error`, `message`, and `timestamp`. The authentication entry point may return an empty 401 body. Unexpected exceptions are logged server-side and return a generic 500 message. Worker failures are read through the job status endpoint: 202 acknowledges submission, not completed indexing.
