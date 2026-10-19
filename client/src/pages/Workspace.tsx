import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, type Repository } from "../lib/api";

export function Workspace() {
  const [repos, setRepos] = useState<Repository[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [url, setUrl] = useState("");
  const [search, setSearch] = useState("");
  const navigate = useNavigate();
  const load = useCallback(async (sync = false) => {
    const rows = await api.listRepos(sync);
    setRepos(rows);
    return rows;
  }, []);
  useEffect(() => {
    let active = true;
    api
      .listRepos(false)
      .then(async (rows) => (rows.length ? rows : api.listRepos(true)))
      .then((rows) => {
        if (active) setRepos(rows);
      })
      .catch((err: Error) => {
        if (active) setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  const indexing = repos.some((repo) => repo.indexStatus === "INDEXING");
  useEffect(() => {
    if (!indexing) return;
    const timer = window.setInterval(
      () => void load().catch((err: Error) => setError(err.message)),
      2500,
    );
    return () => window.clearInterval(timer);
  }, [indexing, load]);
  async function run(action: string, operation: () => Promise<void>) {
    setBusy(action);
    setError("");
    setNotice("");
    try {
      await operation();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy("");
    }
  }
  function importRepo(event: FormEvent) {
    event.preventDefault();
    void run("import", async () => {
      const repo = await api.importPublicRepo(url.trim());
      await load();
      setUrl("");
      setSearch("");
      setNotice(
        `${repo.fullName} is in your workspace.`,
      );
    });
  }
  const visible = repos.filter((repo) =>
    repo.fullName.toLowerCase().includes(search.toLowerCase()),
  );
  return (
    <main className="workspace">
      <div className="page-heading">
        <div>
          <p className="eyebrow">WORKSPACE</p>
          <h1>Repositories</h1>
          <p className="muted">
            Import code. Follow indexing progress. Ask focused questions.
          </p>
        </div>
        <button
          disabled={!!busy || loading}
          onClick={() =>
            void run("sync", async () => {
              await load(true);
              setNotice("Repositories synced.");
            })
          }
        >
          {busy === "sync" ? "Syncing…" : "Sync GitHub"}
        </button>
      </div>
      <section className="panel import-panel">
        <form onSubmit={importRepo}>
          <label htmlFor="repo-url">Import a public repository</label>
          <div className="inline-form">
            <input
              id="repo-url"
              type="url"
              required
              maxLength={500}
              value={url}
              onChange={(event) => setUrl(event.target.value)}
              placeholder="https://github.com/owner/repository"
              disabled={!!busy}
            />
            <button className="primary" disabled={!!busy || !url.trim()}>
              {busy === "import" ? "Importing…" : "Import repository"}
            </button>
          </div>
          <p className="hint">
            No fork required. Your imported repositories stay here after
            syncing.
          </p>
        </form>
      </section>
      {error && (
        <p role="alert" className="notice error">
          {error}
        </p>
      )}
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      <section className="panel repository-panel">
        <div className="list-heading">
          <span>
            {repos.length} repositories ·{" "}
            {repos.filter((r) => r.indexStatus === "READY").length} ready
          </span>
          <input
            aria-label="Search repositories"
            placeholder="Search repositories…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        {loading ? (
          <p role="status" className="empty">
            Loading repositories…
          </p>
        ) : visible.length === 0 ? (
          <p className="empty">
            No repositories found. Import a public URL or sync GitHub.
          </p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Repository</th>
                  <th>Status</th>
                  <th>Indexed code</th>
                  <th>
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {visible.map((repo) => (
                  <tr key={repo.id}>
                    <td>
                      <a
                        className="repo-name"
                        href={
                          repo.htmlUrl ?? `https://github.com/${repo.fullName}`
                        }
                        target="_blank"
                        rel="noreferrer"
                      >
                        {repo.fullName}
                      </a>
                      <p className="hint">
                        {repo.defaultBranch} ·{" "}
                        {repo.isPrivate ? "Private" : "Public"}
                        {repo.language ? ` · ${repo.language}` : ""}
                      </p>
                      {repo.errorMessage && (
                        <p className="row-error">{repo.errorMessage}</p>
                      )}
                    </td>
                    <td>
                      <span
                        className={`badge ${repo.indexStatus.toLowerCase()}`}
                      >
                        {repo.indexStatus === "PENDING"
                          ? "Not indexed"
                          : repo.indexStatus.toLowerCase()}
                      </span>
                    </td>
                    <td>
                      {repo.indexStatus === "INDEXING" ? (
                        <>
                          <span>
                            {repo.filesProcessed}/{repo.filesTotal || "?"} files
                          </span>
                          <progress
                            aria-label={`Indexing ${repo.fullName}`}
                            value={repo.filesProcessed}
                            max={repo.filesTotal || 1}
                          />
                        </>
                      ) : (
                        <span>
                          {repo.filesProcessed} files · {repo.chunkCount} chunks
                        </span>
                      )}
                    </td>
                    <td>
                      <div className="row-actions">
                        {repo.indexStatus === "READY" ||
                        repo.indexStatus === "INDEXING" ? (
                          <Link
                            className="button"
                            to={`/repositories/${repo.id}`}
                          >
                            {repo.indexStatus === "READY"
                              ? "Chat"
                              : "View progress"}
                          </Link>
                        ) : (
                          <button
                            disabled={!!busy}
                            onClick={() =>
                              void run(repo.id, async () => {
                                await api.startIndex(repo.id);
                                navigate(`/repositories/${repo.id}`);
                              })
                            }
                          >
                            {busy === repo.id
                              ? "Starting…"
                              : repo.indexStatus === "FAILED"
                                ? "Retry indexing"
                                : "Index"}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}
