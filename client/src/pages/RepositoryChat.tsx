import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { Link, useParams } from "react-router-dom";
import Markdown from "react-markdown";
import {
  api,
  type ChatMessage,
  type ChatSession,
  type Repository,
} from "../lib/api";
import { streamChatMessage } from "../lib/stream-chat";

export function RepositoryChat() {
  const { repoId = "" } = useParams();
  const [repo, setRepo] = useState<Repository | null>(null);
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [sessionId, setSessionId] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState("");
  const [draft, setDraft] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [streaming, setStreaming] = useState(false);
  const [busy, setBusy] = useState(false);
  const abort = useRef<AbortController | null>(null);
  const end = useRef<HTMLDivElement>(null);
  const refreshRepo = useCallback(async () => {
    const row = await api.getRepo(repoId);
    setRepo(row);
    return row;
  }, [repoId]);
  useEffect(() => {
    let active = true;
    api
      .getRepo(repoId)
      .then((row) => {
        if (active) setRepo(row);
      })
      .catch((err: Error) => {
        if (active) setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
      abort.current?.abort();
    };
  }, [repoId]);
  useEffect(() => {
    if (repo?.indexStatus !== "INDEXING") return;
    const timer = window.setInterval(
      () => void refreshRepo().catch((err: Error) => setError(err.message)),
      2500,
    );
    return () => window.clearInterval(timer);
  }, [repo?.indexStatus, refreshRepo]);
  useEffect(() => {
    if (repo?.indexStatus !== "READY") return;
    let active = true;
    api
      .listSessions(repoId)
      .then((rows) => {
        if (active) {
          setSessions(rows);
          setSessionId(rows[0]?.id ?? "");
        }
      })
      .catch((err: Error) => {
        if (active) setError(err.message);
      });
    return () => {
      active = false;
    };
  }, [repo?.indexStatus, repoId]);
  useEffect(() => {
    if (!sessionId) {
      setMessages([]);
      return;
    }
    let active = true;
    setBusy(true);
    setMessages([]);
    api
      .getMessages(sessionId)
      .then((rows) => {
        if (active) setMessages(rows);
      })
      .catch((err: Error) => {
        if (active) setError(err.message);
      })
      .finally(() => {
        if (active) setBusy(false);
      });
    return () => {
      active = false;
    };
  }, [sessionId]);
  useEffect(() => {
    end.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, draft]);
  async function createSession() {
    setBusy(true);
    setError("");
    try {
      const session = await api.createSession(repoId);
      setSessions((rows) => [session, ...rows]);
      setSessionId(session.id);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function index() {
    setBusy(true);
    setError("");
    try {
      setRepo(await api.startIndex(repoId));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function send(event: FormEvent) {
    event.preventDefault();
    if (!sessionId || streaming || !question.trim()) return;
    const text = question.trim();
    const controller = new AbortController();
    abort.current = controller;
    setStreaming(true);
    setDraft("");
    setQuestion("");
    setError("");
    try {
      await streamChatMessage(sessionId, text, {
        signal: controller.signal,
        onUserMessage: (message) => setMessages((rows) => [...rows, message]),
        onToken: (token) => setDraft((value) => value + token),
        onAssistantMessage: (message) => {
          setMessages((rows) => [...rows, message]);
          setDraft("");
        },
      });
    } catch (err) {
      if ((err as Error).name !== "AbortError")
        setError((err as Error).message);
    } finally {
      setStreaming(false);
      setDraft("");
      try {
        setMessages(await api.getMessages(sessionId));
      } catch {
        /* Keep the visible messages if refresh fails. */
      }
    }
  }
  return (
    <main className="workspace">
      <Link className="back-link" to="/repositories">
        ← Repositories
      </Link>
      <div className="page-heading">
        <div>
          <p className="eyebrow">REPOSITORY CHAT</p>
          <h1>{repo?.fullName ?? "Repository"}</h1>
          <p className="muted">
            Answers grounded in indexed files, with source links.
          </p>
        </div>
        {repo && (
          <span className={`badge ${repo.indexStatus.toLowerCase()}`}>
            {repo.indexStatus.toLowerCase()}
          </span>
        )}
      </div>
      {error && (
        <p role="alert" className="notice error">
          {error}
        </p>
      )}
      {loading ? (
        <p role="status">Loading repository…</p>
      ) : repo && repo.indexStatus !== "READY" ? (
        <section className="panel status-panel">
          <h2>
            {repo.indexStatus === "INDEXING"
              ? "Indexing repository"
              : repo.indexStatus === "FAILED"
                ? "Indexing failed"
                : "Index this repository"}
          </h2>
          {repo.indexStatus === "INDEXING" ? (
            <>
              <p role="status">
                {repo.filesProcessed} of {repo.filesTotal || "?"} files
                processed · {repo.chunkCount} chunks
              </p>
              <progress
                aria-label="Indexing progress"
                value={repo.filesProcessed}
                max={repo.filesTotal || 1}
              />
            </>
          ) : (
            <>
              <p>
                {repo.errorMessage ??
                  "Prepare its source files before starting a chat."}
              </p>
              <button
                className="primary"
                disabled={busy}
                onClick={() => void index()}
              >
                {busy ? "Starting…" : "Start indexing"}
              </button>
            </>
          )}
        </section>
      ) : (
        repo && (
          <div className="chat-layout">
            <aside className="panel sessions">
              <div className="section-heading">
                <h2>Conversations</h2>
                <button
                  disabled={streaming || busy}
                  onClick={() => void createSession()}
                >
                  New chat
                </button>
              </div>
              <nav aria-label="Chat sessions">
                {sessions.map((session) => (
                  <button
                    key={session.id}
                    disabled={streaming || busy}
                    aria-current={session.id === sessionId ? "page" : undefined}
                    onClick={() => {
                      setSessionId(session.id);
                      setError("");
                    }}
                  >
                    {session.title}
                    <small>
                      {new Date(session.createdAt).toLocaleDateString()}
                    </small>
                  </button>
                ))}
              </nav>
              <div className="index-summary">
                <p>
                  {repo.filesProcessed} files · {repo.chunkCount} chunks
                </p>
                <button
                  className="quiet"
                  disabled={streaming || busy}
                  onClick={() => void index()}
                >
                  Re-index repository
                </button>
                <p className="hint">Re-index after source files change.</p>
              </div>
            </aside>
            <section className="panel conversation">
              <div className="messages" aria-busy={streaming}>
                {!sessionId && (
                  <div className="empty">
                    <h2>Ask about this codebase</h2>
                    <p>Create a chat to explore the indexed source.</p>
                    <button
                      className="primary"
                      disabled={busy}
                      onClick={() => void createSession()}
                    >
                      Start a chat
                    </button>
                  </div>
                )}
                {sessionId && !messages.length && !streaming && (
                  <p className="empty">
                    Try “Where is authentication handled?” or “Explain the
                    indexing flow.”
                  </p>
                )}
                {messages.map((message) => (
                  <article
                    className={`message ${message.role.toLowerCase()}`}
                    key={message.id}
                  >
                    <span className="message-role">
                      {message.role === "USER" ? "You" : "CogniStream"}
                    </span>
                    <Markdown>{message.content}</Markdown>
                    {message.citations.length > 0 && (
                      <div className="sources">
                        <span>Sources</span>
                        {Array.from(
                          new Set(
                            message.citations.map(
                              (citation) => citation.filePath,
                            ),
                          ),
                        ).map((path) => (
                          <a
                            key={path}
                            href={`https://github.com/${repo.fullName}/blob/${encodeURIComponent(repo.defaultBranch)}/${path.split("/").map(encodeURIComponent).join("/")}`}
                            target="_blank"
                            rel="noreferrer"
                          >
                            {path}
                          </a>
                        ))}
                      </div>
                    )}
                  </article>
                ))}
                {streaming && (
                  <article className="message assistant">
                    <span className="message-role">CogniStream</span>
                    {draft ? (
                      <Markdown>{draft}</Markdown>
                    ) : (
                      <p role="status">
                        Reading the code and preparing an answer…
                      </p>
                    )}
                  </article>
                )}
                <div ref={end} />
              </div>
              <form className="composer" onSubmit={(event) => void send(event)}>
                <label htmlFor="question">Ask a question</label>
                <textarea
                  id="question"
                  rows={3}
                  maxLength={8000}
                  value={question}
                  onChange={(event) => setQuestion(event.target.value)}
                  disabled={!sessionId || busy || streaming}
                  placeholder="Ask about the indexed code…"
                />
                <div className="composer-actions">
                  <span className="hint">
                    Local AI can be slow and may make mistakes. Check source
                    files.
                  </span>
                  {streaming ? (
                    <button
                      type="button"
                      onClick={() => abort.current?.abort()}
                    >
                      Stop
                    </button>
                  ) : (
                    <button
                      className="primary"
                      disabled={!sessionId || busy || !question.trim()}
                    >
                      Send message
                    </button>
                  )}
                </div>
              </form>
            </section>
          </div>
        )
      )}
    </main>
  );
}
