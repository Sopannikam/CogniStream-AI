# Verification

Verified locally on 12 October 2026:

The nine historical Git commits have adjusted March–June timestamps. Tests and GitHub Actions actually ran in October; see [commit history notes](HISTORY.md) for the original dates and commit-ID mapping.

- Maven Java compilation and executable Spring Boot JAR packaging: passed.
- Backend tests: 8 passed, 0 failures/errors/skips. These use H2 and mocked external AI/vector services.
- SSE frontend tests: 3 passed. They cover fragmented UTF-8, CRLF framing, provider errors, and interrupted responses.
- Frontend ESLint: passed.
- Frontend compilation and TypeScript checking: passed.
- Full frontend production build: passed on Linux in GitHub Actions and locally on Windows. The Windows build reported cached lucide icon-import warnings; the clean CI build after the cleanup did not.
- Live PostgreSQL/pgvector operation: not verified because access to Docker's local engine was denied.
- Live GitHub OAuth and OpenAI calls: require the owner's credentials and have not been verified.

GitHub Actions run: https://github.com/Sopannikam/CogniStream-AI/actions/runs/38273222323 — backend and frontend jobs both passed, including the full production build.

This file records actual evidence. Complete the live walkthrough in SUBMISSION.md before demonstrating the project.

Presentation cleanup: removed 36 unused UI components and seven unused direct dependencies. Backend tests (8), frontend streaming tests (3), lint, TypeScript checking, and the local production build passed again after the cleanup.

Cleanup CI: [run 38288344020](https://github.com/Sopannikam/CogniStream-AI/actions/runs/38288344020) passed both jobs for original commit `d431421` (rewritten as `0b53a17` with an identical source tree). The frontend job completed a fresh dependency installation, tests, lint, and production build without icon-import warnings.
