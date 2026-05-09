# Submission walkthrough

## Before demonstrating

Fill the local `.env` credentials, start the healthy pgvector database, backend, and frontend. Sign in using GitHub. Use a small repository you own or have access to; indexing larger repositories takes longer and consumes more embedding requests.

## Five-minute flow

1. Show the landing page and explain that the project helps developers understand GitHub codebases.
2. Sign in with GitHub and show the repository dashboard. Explain that OAuth creates the session and the backend stores an encrypted access token.
3. Sync repositories and use the search/filter controls.
4. Index a small repository. Show progress changing from INDEXING to READY, with files and chunks.
5. Open chat and ask: "What does this project do?", "Where is the main entry point?", or "Where is authentication implemented?" Use questions supported by that repository's code.
6. Show incremental Markdown output and open a citation on GitHub.
7. Create a second chat session and show the saved first session.
8. Explain the architecture using `docs/IMPLEMENTATION.md` and show the test results.

## Explain honestly

This is a tutorial-based implementation adapted from the instructor's source, with working configuration and tested fixes. Tests that use mocked AI/vector services validate application behavior, not live model quality or pgvector integration. Actual OAuth and AI operation require your credentials and a completed live walkthrough. Do not present mocked test answers as real OpenAI output.

Attach the repository link and, if required, the source archive and a screen recording made after the live walkthrough. Keep `.env`, tokens, and local database credentials out of the submitted archive and GitHub.
