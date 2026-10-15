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
