import { useEffect, useState } from "react";
import { Link, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { api, getGithubLoginUrl, ApiError, type User } from "./lib/api";
import { Workspace } from "./pages/Workspace";
import { RepositoryChat } from "./pages/RepositoryChat";

export function App() {
  const location = useLocation();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    api
      .me()
      .then((profile) => {
        if (active) setUser(profile);
      })
      .catch((err: Error) => {
        if (active && !(err instanceof ApiError && err.status === 401))
          setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);
  async function logout() {
    try {
      await api.logout();
      setUser(null);
    } catch (err) {
      setError((err as Error).message);
    }
  }
  return (
    <>
      <header className="topbar">
        <Link to="/repositories" className="brand">
          <span className="brand-mark">C</span>CogniStream
        </Link>
        <div className="account">
          {user && (
            <>
              <span>{user.githubUsername}</span>
              <button className="quiet" onClick={() => void logout()}>
                Sign out
              </button>
            </>
          )}
        </div>
      </header>
      {error && (
        <div role="alert" className="notice error">
          {error}{" "}
          <button onClick={() => window.location.reload()}>
            Retry connection
          </button>
        </div>
      )}
      {loading ? (
        <main className="login">
          <p role="status">Connecting to your workspace…</p>
        </main>
      ) : !user ? (
        <main className="login">
          <section className="panel login-panel">
            <p className="eyebrow">REPOSITORY WORKSPACE</p>
            <h1>Understand your code.</h1>
            <p>
              Connect GitHub, index a repository, and ask questions with links
              to the source.
            </p>
            <a className="button primary" href={getGithubLoginUrl()}>
              Continue with GitHub
            </a>
            <p className="hint">
              Indexing and answers use local AI on your computer.
            </p>
          </section>
        </main>
      ) : (
        <Routes>
          <Route path="/repositories" element={<Workspace />} />
          <Route
            path="/repositories/:repoId"
            element={<RepositoryChat key={location.pathname} />}
          />
          <Route
            path="/chat/:repoId"
            element={<RepositoryChat key={location.pathname} />}
          />
          <Route path="*" element={<Navigate to="/repositories" replace />} />
        </Routes>
      )}
    </>
  );
}
