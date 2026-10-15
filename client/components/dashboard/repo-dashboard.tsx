"use client";

import { useMemo, useState } from "react";
import { FolderGit2 } from "lucide-react";

import { DashboardHeader } from "@/components/dashboard/dashboard-header";
import { RepoCard } from "@/components/dashboard/repo-card";
import { Button } from "@/components/ui/button";
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useImportPublicRepo, useRefreshRepos, useRepos } from "@/hooks/use-repos";
import type { IndexStatus } from "@/lib/api";

type FilterStatus = "ALL" | IndexStatus;

export function RepoDashboard() {
  const reposQuery = useRepos();
  const refresh = useRefreshRepos();
  const importRepo = useImportPublicRepo();
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<FilterStatus>("ALL");
  const [visibility, setVisibility] = useState<"all" | "public" | "private">(
    "all"
  );

  const filtered = useMemo(() => {
    const list = reposQuery.data ?? [];
    const q = search.trim().toLowerCase();

    return list.filter((repo) => {
      if (status !== "ALL" && repo.indexStatus !== status) return false;
      if (visibility === "private" && !repo.isPrivate) return false;
      if (visibility === "public" && repo.isPrivate) return false;
      if (!q) return true;
      return (
        repo.fullName.toLowerCase().includes(q) ||
        (repo.description ?? "").toLowerCase().includes(q) ||
        (repo.language ?? "").toLowerCase().includes(q)
      );
    });
  }, [reposQuery.data, search, status, visibility]);

  const readyCount =
    reposQuery.data?.filter((r) => r.indexStatus === "READY").length ?? 0;

  return (
    <div className="flex min-h-full flex-col">
      <DashboardHeader
        search={search}
        onSearchChange={setSearch}
        visibility={visibility}
        onVisibilityChange={setVisibility}
        status={status}
        onStatusChange={setStatus}
        totalCount={reposQuery.data?.length}
        readyCount={readyCount}
        onSync={() => refresh.mutate()}
        isSyncing={refresh.isPending || reposQuery.isFetching}
      />

      <div className="flex flex-1 flex-col gap-4 p-4 md:p-6">
        <form
          className="rounded-xl border bg-card p-4"
          onSubmit={(event) => {
            event.preventDefault();
            importRepo.mutate(repositoryUrl.trim(), {
              onSuccess: () => {
                setRepositoryUrl("");
                setSearch("");
                setStatus("ALL");
                setVisibility("all");
              },
            });
          }}
        >
          <label htmlFor="public-repository-url" className="text-sm font-medium">
            Import a public repository
          </label>
          <p id="public-repository-help" className="mt-1 text-sm text-muted-foreground">
            Paste a GitHub repository URL, then choose Index on its card to prepare it for chat.
          </p>
          <div className="mt-3 flex flex-col gap-2 sm:flex-row">
            <Input
              id="public-repository-url"
              type="url"
              required
              maxLength={500}
              placeholder="https://github.com/owner/repository"
              value={repositoryUrl}
              onChange={(event) => {
                setRepositoryUrl(event.target.value);
                importRepo.reset();
              }}
              aria-describedby="public-repository-help public-repository-result"
              aria-invalid={importRepo.isError}
              disabled={importRepo.isPending}
              className="flex-1"
            />
            <Button type="submit" disabled={importRepo.isPending || !repositoryUrl.trim()}>
              {importRepo.isPending ? "Importing…" : "Import repository"}
            </Button>
          </div>
          <div id="public-repository-result" aria-live="polite">
            {importRepo.isError && (
              <p role="alert" className="mt-2 text-sm text-destructive">{importRepo.error.message}</p>
            )}
            {importRepo.isSuccess && (
              <p className="mt-2 text-sm text-muted-foreground">
                {importRepo.data.fullName} is in your workspace. Choose Index, or Chat if it is already ready.
              </p>
            )}
          </div>
        </form>

        {reposQuery.isLoading && (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-64 rounded-2xl" />
            ))}
          </div>
        )}

        {reposQuery.isError && (
          <Empty className="border border-dashed">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <FolderGit2 />
              </EmptyMedia>
              <EmptyTitle>Couldn’t load repositories</EmptyTitle>
              <EmptyDescription>
                {(reposQuery.error as Error).message}
              </EmptyDescription>
            </EmptyHeader>
            <Button onClick={() => void reposQuery.refetch()}>Try again</Button>
          </Empty>
        )}

        {reposQuery.isSuccess && filtered.length === 0 && (
          <Empty className="border border-dashed">
            <EmptyHeader>
              <EmptyMedia variant="icon">
                <FolderGit2 />
              </EmptyMedia>
              <EmptyTitle>No repositories match</EmptyTitle>
              <EmptyDescription>
                Try clearing filters or syncing again.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}

        {reposQuery.isSuccess && filtered.length > 0 && (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {filtered.map((repo) => (
              <RepoCard key={repo.id} repo={repo} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
