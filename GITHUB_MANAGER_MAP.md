# GithubManager.kt Structural Map

Generated from `app/src/main/java/com/glassfiles/data/github/GitHubManager.kt`. This is a read-only structural map; no source changes are implied here.

## 1. File overview

- Total line count: **5294**
- `object GitHubManager` range: **lines 16-4536** (data classes continue after it through line 5294).
- Total public methods on `GitHubManager`: **288** (overloads counted separately).
- Total private methods/helpers on `GitHubManager`: **65**.
- Approximate properties / fields: **5** private constants: `TAG`, `API`, `PREFS`, `KEY_TOKEN`, `KEY_USER`.
- Major dependencies imported: Android `Context`/`Log`; Kotlin coroutines `Dispatchers` and `withContext`; `org.json.JSONArray` / `JSONObject`; Java `HttpURLConnection`, `URL`, `URLEncoder`, `BufferedReader`, `InputStreamReader`, `OutputStreamWriter`; Java/Kotlin file and zip APIs via fully-qualified names in method bodies.
- Not imported/used here: OkHttp, Moshi, kotlinx.serialization, Retrofit.

## 2. Method groups by domain

### Authentication / Session (lines 1-114)
- Public methods: **6**
- Summary: Token persistence, login state, current user fetch/cache, and the core REST/GraphQL transport helpers.
- `saveToken(context: Context, token: String)` - Stores the GitHub token in `github_prefs`.
- `getToken(context: Context): String` - Reads the saved GitHub token from `github_prefs`.
- `isLoggedIn(context: Context): Boolean` - Returns whether a nonblank token is stored.
- `logout(context: Context)` - Clears the GitHub shared preferences.
- `getUser(context: Context): GHUser?` - Fetches `/user` and parses the response.
- `getCachedUser(context: Context): GHUser?` - Reads cached user JSON from `github_prefs` and maps it to `GHUser`.

### Repositories / Contents / Branches (lines 115-190)
- Public methods: **7**
- Summary: Repository listing/search/creation plus contents, decoded file content, and commit listing.
- `getRepos(context: Context, page: Int = 1, perPage: Int = 30): List<GHRepo>` - Fetches `/user/repos?sort=updated&per_page=$perPage&page=$page&type=all` and parses the response.
- `searchRepos(context: Context, query: String): List<GHRepo>` - Searches via `/search/repositories?q=$query&sort=stars&per_page=20` and parses the response.
- `createRepo(context: Context, name: String, description: String, isPrivate: Boolean): Boolean` - Creates/adds via `/user/repos` and returns success/result state.
- `deleteRepo(context: Context, owner: String, repo: String): Boolean` - Deletes/removes via `/repos/$owner/$repo` and returns success/result state.
- `getRepoContents(context: Context, owner: String, repo: String, path: String = "", branch: String? = null): List<GHContent>` - Fetches `/repos/$owner/$repo/contents/$path${refQuery(branch)}` and parses the response.
- `getFileContent(context: Context, owner: String, repo: String, path: String, branch: String? = null): String` - Fetches `/repos/$owner/$repo/contents/$path${refQuery(branch)}` and parses the response.
- `getCommits(context: Context, owner: String, repo: String, page: Int = 1): List<GHCommit>` - Fetches `/repos/$owner/$repo/commits?per_page=30&page=$page` and parses the response.

### Issues / Basic Issue List (lines 191-230)
- Public methods: **3**
- Summary: Issue listing/creation and branch listing used by repository screens.
- `getIssues(context: Context, owner: String, repo: String, state: String = "open", page: Int = 1): List<GHIssue>` - Fetches `/repos/$owner/$repo/issues?state=$state&per_page=30&page=$page` and parses the response.
- `createIssue(context: Context, owner: String, repo: String, title: String, body: String): Boolean` - Creates/adds via `/repos/$owner/$repo/issues` and returns success/result state.
- `getBranches(context: Context, owner: String, repo: String): List<String>` - Fetches `/repos/$owner/$repo/branches?per_page=100&page=$page` and parses the response.

### Repository Files / Uploads / Branch Mutation (lines 231-415)
- Public methods: **9**
- Summary: Clone, upload, bulk upload, delete, download, and branch create/delete operations.
- `cloneRepo(context: Context, owner: String, repo: String, destDir: java.io.File, onProgress: (String) -> Unit): Boolean` - Uses a direct `HttpURLConnection` flow for file/asset/log transfer where the generic request helper is not used.
- `uploadFile( context: Context, owner: String, repo: String, path: String, content: ByteArray, message: String, branch: String? = null, sha: String? = null ): Boolean` - Calls `/repos/$owner/$repo/contents/$path` and returns success/result state.
- `uploadFileWithResult( context: Context, owner: String, repo: String, path: String, content: ByteArray, message: String, branch: String? = null, sha: String? = null ): GHFileSaveResult` - Calls `/repos/$owner/$repo/contents/$path` and returns success/result state.
- `uploadFileFromPath( context: Context, owner: String, repo: String, repoPath: String, localPath: String, message: String, branch: String? = null ): Boolean` - Composes existing helpers: `uploadFile()`.
- `uploadMultipleFiles( context: Context, owner: String, repo: String, branch: String, files: List<Pair<String, ByteArray>>, message: String, onProgress: (Int, Int) -> Unit = { _, _ -> } ): Boolean` - Calls `/repos/$owner/$repo/git/ref/heads/$branch`, `/repos/$owner/$repo/git/commits/$latestSha`, `/repos/$owner/$repo/git/blobs` and returns success/result state.
- `deleteFile( context: Context, owner: String, repo: String, path: String, sha: String, message: String, branch: String? = null ): Boolean` - Deletes/removes via `/repos/$owner/$repo/contents/$path` and returns success/result state.
- `downloadFile(context: Context, owner: String, repo: String, path: String, destFile: java.io.File, branch: String? = null): Boolean` - Downloads via `/repos/$owner/$repo/contents/$path${refQuery(branch)}` and returns success/result state.
- `createBranch(context: Context, owner: String, repo: String, branchName: String, fromBranch: String): Boolean` - Creates/adds via `/repos/$owner/$repo/git/ref/heads/$fromBranch`, `/repos/$owner/$repo/git/refs` and returns success/result state.
- `deleteBranch(context: Context, owner: String, repo: String, branch: String): Boolean` - Deletes/removes via `/repos/$owner/$repo/git/refs/heads/$branch` and returns success/result state.

### Pull Requests / Reviews (lines 416-559)
- Public methods: **11**
- Summary: Pull request list/detail/mutation, review retrieval/update/delete, and reviewer request mutation.
- `getPullRequests(context: Context, owner: String, repo: String, state: String = "open", page: Int = 1): List<GHPullRequest>` - Fetches `/repos/$owner/$repo/pulls?state=$state&per_page=30&page=$page` and parses the response.
- `getPullRequestDetail(context: Context, owner: String, repo: String, number: Int): GHPullRequest?` - Fetches `/repos/$owner/$repo/pulls/$number` and parses the response.
- `createPullRequest( context: Context, owner: String, repo: String, title: String, body: String, head: String, base: String ): Boolean` - Creates/adds via `/repos/$owner/$repo/pulls` and returns success/result state.
- `updatePullRequest(context: Context, owner: String, repo: String, number: Int, title: String? = null, body: String? = null, base: String? = null, state: String? = null): Boolean` - Updates via `/repos/$owner/$repo/pulls/$number` and returns success/result state.
- `mergePullRequest(context: Context, owner: String, repo: String, number: Int, message: String = "", method: String = "merge", title: String = ""): Boolean` - Calls `/repos/$owner/$repo/pulls/$number/merge` and returns success/result state.
- `getPullRequestReviews(context: Context, owner: String, repo: String, number: Int): List<GHPullReview>` - Fetches `/repos/$owner/$repo/pulls/$number/reviews?per_page=100` and parses the response.
- `getPullRequestReview(context: Context, owner: String, repo: String, number: Int, reviewId: Long): GHPullReview?` - Fetches `/repos/$owner/$repo/pulls/$number/reviews/$reviewId` and parses the response.
- `updatePullRequestReview(context: Context, owner: String, repo: String, number: Int, reviewId: Long, body: String): GHPullReview?` - Updates via `/repos/$owner/$repo/pulls/$number/reviews/$reviewId` and returns success/result state.
- `deletePullRequestReview(context: Context, owner: String, repo: String, number: Int, reviewId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/pulls/$number/reviews/$reviewId` and returns success/result state.
- `requestPullRequestReviewers(context: Context, owner: String, repo: String, number: Int, reviewers: List<String>): Boolean` - Calls `/repos/$owner/$repo/pulls/$number/requested_reviewers` and returns success/result state.
- `removePullRequestReviewers(context: Context, owner: String, repo: String, number: Int, reviewers: List<String>): Boolean` - Deletes/removes via `/repos/$owner/$repo/pulls/$number/requested_reviewers` and returns success/result state.

### Issues Extended Metadata / Reactions (lines 560-632)
- Public methods: **9**
- Summary: Issue comments, issue open/close/lock state, and issue detail parsing.
- `getIssueComments(context: Context, owner: String, repo: String, number: Int): List<GHComment>` - Fetches `/repos/$owner/$repo/issues/$number/comments?per_page=50` and parses the response.
- `addComment(context: Context, owner: String, repo: String, number: Int, body: String): Boolean` - Creates/adds via `/repos/$owner/$repo/issues/$number/comments` and returns success/result state.
- `updateIssueComment(context: Context, owner: String, repo: String, commentId: Long, body: String): Boolean` - Updates via `/repos/$owner/$repo/issues/comments/$commentId` and returns success/result state.
- `deleteIssueComment(context: Context, owner: String, repo: String, commentId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/issues/comments/$commentId` and returns success/result state.
- `closeIssue(context: Context, owner: String, repo: String, number: Int): Boolean` - Calls `/repos/$owner/$repo/issues/$number` and returns success/result state.
- `reopenIssue(context: Context, owner: String, repo: String, number: Int): Boolean` - Calls `/repos/$owner/$repo/issues/$number` and returns success/result state.
- `lockIssue(context: Context, owner: String, repo: String, number: Int, reason: String = ""): Boolean` - Calls `/repos/$owner/$repo/issues/$number/lock` and returns success/result state.
- `unlockIssue(context: Context, owner: String, repo: String, number: Int): Boolean` - Deletes/removes via `/repos/$owner/$repo/issues/$number/lock` and returns success/result state.
- `getIssueDetail(context: Context, owner: String, repo: String, number: Int): GHIssueDetail?` - Fetches `/repos/$owner/$repo/issues/$number` and parses the response.

### Repository Social / README Metadata (lines 633-677)
- Public methods: **7**
- Summary: Star/watch-adjacent repository actions, README content, languages, contributors, and forking.
- `isStarred(context: Context, owner: String, repo: String): Boolean` - Fetches `/user/starred/$owner/$repo` and parses the response.
- `starRepo(context: Context, owner: String, repo: String): Boolean` - Calls `/user/starred/$owner/$repo` and returns success/result state.
- `unstarRepo(context: Context, owner: String, repo: String): Boolean` - Deletes/removes via `/user/starred/$owner/$repo` and returns success/result state.
- `forkRepo(context: Context, owner: String, repo: String): Boolean` - Calls `/repos/$owner/$repo/forks` and returns success/result state.
- `getReadme(context: Context, owner: String, repo: String): String` - Fetches `/repos/$owner/$repo/readme` and parses the response.
- `getLanguages(context: Context, owner: String, repo: String): Map<String, Long>` - Fetches `/repos/$owner/$repo/languages` and parses the response.
- `getContributors(context: Context, owner: String, repo: String): List<GHContributor>` - Fetches `/repos/$owner/$repo/contributors?per_page=30` and parses the response.

### Releases / Release Assets (lines 678-918)
- Public methods: **12**
- Summary: Release CRUD, detailed release publishing, release asset download/upload/delete, and release parsers.
- `getReleases(context: Context, owner: String, repo: String): List<GHRelease>` - Fetches `/repos/$owner/$repo/releases?per_page=20` and parses the response.
- `createRelease(context: Context, owner: String, repo: String, tag: String, name: String, body: String, prerelease: Boolean = false): Boolean` - Creates/adds via `/repos/$owner/$repo/releases` and returns success/result state.
- `createReleaseDetailed( context: Context, owner: String, repo: String, tag: String, name: String, body: String, prerelease: Boolean = false, draft: Boolean = false, targetCommitish: String = "" ): GHRelease?` - Creates/adds via `/repos/$owner/$repo/releases` and returns success/result state.
- `getReleaseByTag(context: Context, owner: String, repo: String, tag: String): GHRelease?` - Fetches `/repos/$owner/$repo/releases/tags/$encodedTag` and parses the response.
- `updateRelease(context: Context, owner: String, repo: String, tag: String, name: String, body: String, prerelease: Boolean): Boolean` - Composes existing helpers: `updateReleaseDetailed()`.
- `updateReleaseDetailed( context: Context, owner: String, repo: String, tag: String, name: String, body: String, prerelease: Boolean, draft: Boolean? = null, releaseId: Long = 0L ): GHRelease?` - Updates via `/repos/$owner/$repo/releases/tags/$encodedTag`, `/repos/$owner/$repo/releases/$resolvedReleaseId` and returns success/result state.
- `publishRelease(context: Context, owner: String, repo: String, release: GHRelease): GHRelease?` - Composes existing helpers: `updateReleaseDetailed()`.
- `deleteRelease(context: Context, owner: String, repo: String, tag: String): Boolean` - Deletes/removes via `/repos/$owner/$repo/releases/tags/$encodedTag`, `/repos/$owner/$repo/releases/$releaseId` and returns success/result state.
- `deleteReleaseAsset(context: Context, owner: String, repo: String, assetId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/releases/assets/$assetId` and returns success/result state.
- `downloadReleaseAsset(context: Context, asset: GHAsset, destFile: java.io.File): Boolean` - Uses a direct `HttpURLConnection` flow for file/asset/log transfer where the generic request helper is not used.
- `uploadReleaseAsset(context: Context, owner: String, repo: String, releaseId: Long, file: java.io.File, label: String = ""): Boolean` - Uses a direct `HttpURLConnection` flow for file/asset/log transfer where the generic request helper is not used.
- `uploadReleaseAssetDetailed(context: Context, owner: String, repo: String, releaseId: Long, file: java.io.File, label: String = ""): GHAsset?` - Uses a direct `HttpURLConnection` flow for file/asset/log transfer where the generic request helper is not used.

### Gists (lines 919-962)
- Public methods: **4**
- Summary: Authenticated gist list/create/read/delete operations.
- `getGists(context: Context): List<GHGist>` - Fetches `/gists?per_page=30` and parses the response.
- `createGist(context: Context, description: String, isPublic: Boolean, files: Map<String, String>): Boolean` - Creates/adds via `/gists` and returns success/result state.
- `getGistContent(context: Context, gistId: String): Map<String, String>` - Fetches `/gists/$gistId` and parses the response.
- `deleteGist(context: Context, gistId: String): Boolean` - Deletes/removes via `/gists/$gistId` and returns success/result state.

### Search / Commit Diff (lines 963-1000)
- Public methods: **2**
- Summary: User search and commit diff detail retrieval.
- `searchUsers(context: Context, query: String): List<GHUser>` - Searches via `/search/users?q=$q&per_page=30` and parses the response.
- `getCommitDiff(context: Context, owner: String, repo: String, sha: String): GHCommitDetail?` - Fetches `/repos/$owner/$repo/commits/$sha` and parses the response.

### Actions / Workflows (lines 1001-1766)
- Public methods: **57**
- Summary: Workflows, runs, jobs, logs, dispatch, artifacts, checks, pending deployments, caches, secrets, variables, runners, permissions, and retention.
- `getWorkflows(context: Context, owner: String, repo: String): List<GHWorkflow>` - Fetches `/repos/$owner/$repo/actions/workflows?per_page=100&page=$page` and parses the response.
- `getWorkflowRuns( context: Context, owner: String, repo: String, workflowId: Long? = null, perPage: Int = 20, page: Int = 1, branch: String? = null, event: String? = null, status: String? = null ): List<GHWorkflowRun>` - Composes existing helpers: `parseWorkflowRun()`, `request()`.
- `getWorkflowRun(context: Context, owner: String, repo: String, runId: Long): GHWorkflowRun?` - Fetches `/repos/$owner/$repo/actions/runs/$runId` and parses the response.
- `getWorkflowRunJobs(context: Context, owner: String, repo: String, runId: Long): List<GHJob>` - Fetches `/repos/$owner/$repo/actions/runs/$runId/jobs?filter=all&per_page=100` and parses the response.
- `getWorkflowRunLogs(context: Context, owner: String, repo: String, runId: Long): String` - Uses a direct `HttpURLConnection` flow for file/asset/log transfer where the generic request helper is not used.
- `getJobLogs(context: Context, owner: String, repo: String, jobId: Long): String` - Uses a direct `HttpURLConnection` flow for file/asset/log transfer where the generic request helper is not used.
- `rerunWorkflow(context: Context, owner: String, repo: String, runId: Long): Boolean` - Calls `/repos/$owner/$repo/actions/runs/$runId/rerun` and returns success/result state.
- `rerunFailedJobs(context: Context, owner: String, repo: String, runId: Long): Boolean` - Calls `/repos/$owner/$repo/actions/runs/$runId/rerun-failed-jobs` and returns success/result state.
- `cancelWorkflowRun(context: Context, owner: String, repo: String, runId: Long): Boolean` - Calls `/repos/$owner/$repo/actions/runs/$runId/cancel` and returns success/result state.
- `enableWorkflow(context: Context, owner: String, repo: String, workflowId: Long): Boolean` - Calls `/repos/$owner/$repo/actions/workflows/$workflowId/enable` and returns success/result state.
- `disableWorkflow(context: Context, owner: String, repo: String, workflowId: Long): Boolean` - Calls `/repos/$owner/$repo/actions/workflows/$workflowId/disable` and returns success/result state.
- `dispatchWorkflow(context: Context, owner: String, repo: String, workflowId: Long, branch: String, inputs: Map<String, String> = emptyMap()): Boolean` - Calls `/repos/$owner/$repo/actions/workflows/$workflowId/dispatches` and returns success/result state.
- `dispatchWorkflowDetailed(context: Context, owner: String, repo: String, workflowId: Long, branch: String, inputs: Map<String, String> = emptyMap()): GHActionResult` - Calls `/repos/$owner/$repo/actions/workflows/$workflowId/dispatches` and returns success/result state.
- `dispatchWorkflow(context: Context, owner: String, repo: String, workflowId: String, ref: String, inputs: Map<String, String> = emptyMap()): Boolean` - Calls `/repos/$owner/$repo/actions/workflows/$encodedId/dispatches` and returns success/result state.
- `getWorkflowDispatchSchema(context: Context, owner: String, repo: String, workflowPath: String, branch: String? = null): GHWorkflowDispatchSchema?` - Composes existing helpers: `getFileContent()`, `parseWorkflowDispatchSchema()`.
- `getWorkflowDispatchSchemas(context: Context, owner: String, repo: String, workflows: List<GHWorkflow>, branch: String? = null): List<GHWorkflowDispatchSchema>` - Composes existing helpers: `getWorkflowDispatchSchema()`.
- `getRunArtifacts(context: Context, owner: String, repo: String, runId: Long): List<GHArtifact>` - Fetches `/repos/$owner/$repo/actions/runs/$runId/artifacts?per_page=100` and parses the response.
- `downloadArtifact(context: Context, owner: String, repo: String, artifactId: Long, destFile: java.io.File): Boolean` - Uses a direct `HttpURLConnection` flow for file/asset/log transfer where the generic request helper is not used.
- `deleteArtifact(context: Context, owner: String, repo: String, artifactId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/actions/artifacts/$artifactId` and returns success/result state.
- `getRepositoryArtifacts(context: Context, owner: String, repo: String, page: Int = 1, name: String? = null): List<GHArtifact>` - Fetches repository Actions artifacts with page/name query parameters and parses the response.
- `getArtifact(context: Context, owner: String, repo: String, artifactId: Long): GHArtifact?` - Fetches `/repos/$owner/$repo/actions/artifacts/$artifactId` and parses the response.
- `getWorkflowRunAttempt(context: Context, owner: String, repo: String, runId: Long, attempt: Int): GHWorkflowRun?` - Fetches `/repos/$owner/$repo/actions/runs/$runId/attempts/$attempt` and parses the response.
- `getWorkflowRunAttemptJobs(context: Context, owner: String, repo: String, runId: Long, attempt: Int): List<GHJob>` - Fetches `/repos/$owner/$repo/actions/runs/$runId/attempts/$attempt/jobs?per_page=100` and parses the response.
- `getWorkflowRunAttemptLogs(context: Context, owner: String, repo: String, runId: Long, attempt: Int): String` - Composes existing helpers: `getRedirectLocationOrText()`.
- `rerunJob(context: Context, owner: String, repo: String, jobId: Long): Boolean` - Calls `/repos/$owner/$repo/actions/jobs/$jobId/rerun` and returns success/result state.
- `deleteWorkflowRun(context: Context, owner: String, repo: String, runId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/actions/runs/$runId` and returns success/result state.
- `deleteWorkflowRunLogs(context: Context, owner: String, repo: String, runId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/actions/runs/$runId/logs` and returns success/result state.
- `forceCancelWorkflowRun(context: Context, owner: String, repo: String, runId: Long): Boolean` - Calls `/repos/$owner/$repo/actions/runs/$runId/force-cancel` and returns success/result state.
- `getWorkflowUsage(context: Context, owner: String, repo: String, workflowId: Long): GHActionsUsage?` - Fetches `/repos/$owner/$repo/actions/workflows/$workflowId/timing` and parses the response.
- `getWorkflowRunUsage(context: Context, owner: String, repo: String, runId: Long): GHActionsUsage?` - Fetches `/repos/$owner/$repo/actions/runs/$runId/timing` and parses the response.
- `getCheckRunsForRef(context: Context, owner: String, repo: String, ref: String): List<GHCheckRun>` - Fetches `/repos/$owner/$repo/commits/$encodedRef/check-runs?per_page=100` and parses the response.
- `getCheckRunAnnotations(context: Context, owner: String, repo: String, checkRunId: Long): List<GHCheckAnnotation>` - Fetches `/repos/$owner/$repo/check-runs/$checkRunId/annotations?per_page=100` and parses the response.
- `getPendingDeployments(context: Context, owner: String, repo: String, runId: Long): List<GHPendingDeployment>` - Fetches `/repos/$owner/$repo/actions/runs/$runId/pending_deployments` and parses the response.
- `reviewPendingDeployments(context: Context, owner: String, repo: String, runId: Long, environmentIds: List<Long>, approve: Boolean, comment: String): Boolean` - Calls `/repos/$owner/$repo/actions/runs/$runId/pending_deployments` and returns success/result state.
- `getWorkflowRunReviewHistory(context: Context, owner: String, repo: String, runId: Long): List<GHWorkflowRunReview>` - Fetches `/repos/$owner/$repo/actions/runs/$runId/approvals` and parses the response.
- `approveWorkflowRunForFork(context: Context, owner: String, repo: String, runId: Long): Boolean` - Calls `/repos/$owner/$repo/actions/runs/$runId/approve` and returns success/result state.
- `getActionsCacheUsage(context: Context, owner: String, repo: String): GHActionsCacheUsage?` - Fetches `/repos/$owner/$repo/actions/cache/usage` and parses the response.
- `getActionsCaches(context: Context, owner: String, repo: String, page: Int = 1, key: String? = null, ref: String? = null): List<GHActionsCacheEntry>` - Fetches repository Actions caches with page/key/ref query parameters and parses the response.
- `deleteActionsCache(context: Context, owner: String, repo: String, cacheId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/actions/caches/$cacheId` and returns success/result state.
- `getRepoActionsSecrets(context: Context, owner: String, repo: String): List<GHActionSecret>` - Fetches `/repos/$owner/$repo/actions/secrets?per_page=100` and parses the response.
- `getRepoActionsPublicKey(context: Context, owner: String, repo: String): GHActionPublicKey?` - Fetches `/repos/$owner/$repo/actions/secrets/public-key` and parses the response.
- `createOrUpdateRepoActionsSecret(context: Context, owner: String, repo: String, name: String, value: String): Boolean` - Creates/adds via `/repos/$owner/$repo/actions/secrets/$encodedName` and returns success/result state.
- `deleteRepoActionsSecret(context: Context, owner: String, repo: String, name: String): Boolean` - Deletes/removes via `/repos/$owner/$repo/actions/secrets/$encodedName` and returns success/result state.
- `getRepoActionsVariables(context: Context, owner: String, repo: String): List<GHActionVariable>` - Fetches `/repos/$owner/$repo/actions/variables?per_page=100` and parses the response.
- `createRepoActionsVariable(context: Context, owner: String, repo: String, name: String, value: String): Boolean` - Creates/adds via `/repos/$owner/$repo/actions/variables` and returns success/result state.
- `updateRepoActionsVariable(context: Context, owner: String, repo: String, name: String, value: String): Boolean` - Updates via `/repos/$owner/$repo/actions/variables/$encodedName` and returns success/result state.
- `deleteRepoActionsVariable(context: Context, owner: String, repo: String, name: String): Boolean` - Deletes/removes via `/repos/$owner/$repo/actions/variables/$encodedName` and returns success/result state.
- `getRepoSelfHostedRunners(context: Context, owner: String, repo: String): List<GHActionRunner>` - Fetches `/repos/$owner/$repo/actions/runners?per_page=100` and parses the response.
- `deleteRepoSelfHostedRunner(context: Context, owner: String, repo: String, runnerId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/actions/runners/$runnerId` and returns success/result state.
- `createRepoRunnerRegistrationToken(context: Context, owner: String, repo: String): GHRunnerToken?` - Creates/adds via `/repos/$owner/$repo/actions/runners/registration-token` and returns success/result state.
- `createRepoRunnerRemoveToken(context: Context, owner: String, repo: String): GHRunnerToken?` - Creates/adds via `/repos/$owner/$repo/actions/runners/remove-token` and returns success/result state.
- `getRepoActionsPermissions(context: Context, owner: String, repo: String): GHActionsPermissions?` - Fetches `/repos/$owner/$repo/actions/permissions` and parses the response.
- `setRepoActionsPermissions(context: Context, owner: String, repo: String, enabled: Boolean, allowedActions: String): Boolean` - Updates via `/repos/$owner/$repo/actions/permissions` and returns success/result state.
- `getRepoActionsWorkflowPermissions(context: Context, owner: String, repo: String): GHWorkflowPermissions?` - Fetches `/repos/$owner/$repo/actions/permissions/workflow` and parses the response.
- `setRepoActionsWorkflowPermissions(context: Context, owner: String, repo: String, defaultWorkflowPermissions: String, canApprovePullRequestReviews: Boolean): Boolean` - Updates via `/repos/$owner/$repo/actions/permissions/workflow` and returns success/result state.
- `getRepoActionsRetention(context: Context, owner: String, repo: String): GHActionsRetention?` - Fetches `/repos/$owner/$repo/actions/permissions/artifact-and-log-retention` and parses the response.
- `setRepoActionsRetention(context: Context, owner: String, repo: String, days: Int): Boolean` - Updates via `/repos/$owner/$repo/actions/permissions/artifact-and-log-retention` and returns success/result state.

### Notifications / Watching (lines 1767-1832)
- Public methods: **9**
- Summary: Notification threads, thread subscriptions, and repository watch/unwatch helpers.
- `getNotifications(context: Context, all: Boolean = false): List<GHNotification>` - Fetches `/notifications?all=$all&per_page=50` and parses the response.
- `markNotificationRead(context: Context, threadId: String): Boolean` - Marks via `/notifications/threads/$threadId` and returns success/result state.
- `markAllNotificationsRead(context: Context): Boolean` - Marks via `/notifications` and returns success/result state.
- `getThreadSubscription(context: Context, threadId: String): GHThreadSubscription` - Fetches `/notifications/threads/$threadId/subscription` and parses the response.
- `setThreadSubscription(context: Context, threadId: String, subscribed: Boolean, ignored: Boolean): Boolean` - Updates via `/notifications/threads/$threadId/subscription` and returns success/result state.
- `deleteThreadSubscription(context: Context, threadId: String): Boolean` - Deletes/removes via `/notifications/threads/$threadId/subscription` and returns success/result state.
- `isWatching(context: Context, owner: String, repo: String): Boolean` - Fetches `/repos/$owner/$repo/subscription` and parses the response.
- `watchRepo(context: Context, owner: String, repo: String): Boolean` - Calls `/repos/$owner/$repo/subscription` and returns success/result state.
- `unwatchRepo(context: Context, owner: String, repo: String): Boolean` - Deletes/removes via `/repos/$owner/$repo/subscription` and returns success/result state.

### Advanced Search (lines 1833-1980)
- Public methods: **5**
- Summary: Search API helpers for code, issues, commits, topics, labels, repository-id lookup, and search parsers.
- `searchCode(context: Context, query: String, owner: String, repo: String): List<GHCodeResult>` - Searches via `/search/code?q=$q&per_page=20` and parses the response.
- `searchIssuesAdvanced(context: Context, query: String, page: Int = 1): List<GHSearchIssueResult>` - Searches via `/search/issues?q=$q&sort=updated&order=desc&per_page=30&page=$page` and parses the response.
- `searchCommitsAdvanced(context: Context, query: String, page: Int = 1): List<GHSearchCommitResult>` - Searches via `/search/commits?q=$q&sort=author-date&order=desc&per_page=30&page=$page` and parses the response.
- `searchTopics(context: Context, query: String, page: Int = 1): List<GHTopicSearchResult>` - Searches via `/search/topics?q=$q&per_page=30&page=$page` and parses the response.
- `searchLabels(context: Context, repositoryFullName: String, query: String, page: Int = 1): List<GHLabelSearchResult>` - Searches via `/search/labels?q=$q&repository_id=$repositoryId&per_page=30&page=$page` and parses the response.

### Users / Organizations (lines 1991-2055)
- Public methods: **8**
- Summary: User profile, user repos, follow state/mutations, starred repositories, organizations, and organization repos.
- `getUserProfile(context: Context, username: String): GHUserProfile?` - Fetches `/users/$username` and parses the response.
- `getUserRepos(context: Context, username: String): List<GHRepo>` - Fetches `/users/$username/repos?sort=updated&per_page=30` and parses the response.
- `isFollowing(context: Context, username: String): Boolean` - Fetches `/user/following/$username` and parses the response.
- `followUser(context: Context, username: String): Boolean` - Calls `/user/following/$username` and returns success/result state.
- `unfollowUser(context: Context, username: String): Boolean` - Deletes/removes via `/user/following/$username` and returns success/result state.
- `getStarredRepos(context: Context, page: Int = 1): List<GHRepo>` - Fetches `/user/starred?sort=created&per_page=30&page=$page` and parses the response.
- `getOrganizations(context: Context): List<GHOrg>` - Fetches `/user/orgs?per_page=30` and parses the response.
- `getOrgRepos(context: Context, org: String): List<GHRepo>` - Fetches `/orgs/$org/repos?sort=updated&per_page=30` and parses the response.

### Issue Metadata / Labels / Milestones (lines 2056-2142)
- Public methods: **10**
- Summary: Labels, milestones, assignees, issue metadata update, PR review submission, and PR files.
- `getLabels(context: Context, owner: String, repo: String): List<GHLabel>` - Fetches `/repos/$owner/$repo/labels?per_page=50` and parses the response.
- `createLabel(context: Context, owner: String, repo: String, name: String, color: String, description: String = ""): Boolean` - Creates/adds via `/repos/$owner/$repo/labels` and returns success/result state.
- `deleteLabel(context: Context, owner: String, repo: String, name: String): Boolean` - Deletes an encoded label name from the repository and returns success state.
- `addLabelsToIssue(context: Context, owner: String, repo: String, issueNumber: Int, labels: List<String>): Boolean` - Creates/adds via `/repos/$owner/$repo/issues/$issueNumber/labels` and returns success/result state.
- `getMilestones(context: Context, owner: String, repo: String): List<GHMilestone>` - Fetches `/repos/$owner/$repo/milestones?per_page=30` and parses the response.
- `createMilestone(context: Context, owner: String, repo: String, title: String, description: String = "", dueOn: String? = null): Boolean` - Creates/adds via `/repos/$owner/$repo/milestones` and returns success/result state.
- `getAssignees(context: Context, owner: String, repo: String): List<GHUserLite>` - Fetches `/repos/$owner/$repo/assignees` and parses the response.
- `updateIssueMeta(context: Context, owner: String, repo: String, issueNumber: Int, labels: List<String>, assignees: List<String>, milestoneNumber: Int?, clearMilestone: Boolean = false): Boolean` - Updates via `/repos/$owner/$repo/issues/$issueNumber` and returns success/result state.
- `submitPullRequestReview(context: Context, owner: String, repo: String, number: Int, event: String, body: String = ""): Boolean` - Calls `/repos/$owner/$repo/pulls/$number/reviews` and returns success/result state.
- `getPullRequestFiles(context: Context, owner: String, repo: String, number: Int): List<GHPullFile>` - Fetches `/repos/$owner/$repo/pulls/$number/files?per_page=100` and parses the response.

### Directory Upload / Account Settings (lines 2143-2381)
- Public methods: **28**
- Summary: Directory upload and native account settings endpoints: profile, emails, keys, social accounts, follows, blocking, interaction limits, and rate limits.
- `uploadDirectory( context: Context, owner: String, repo: String, branch: String, localDir: java.io.File, repoBasePath: String = "", message: String, onProgress: (Int, Int) -> Unit = { _, _ -> } ): Boolean` - Composes existing helpers: `collectFiles()`, `uploadMultipleFiles()`.
- `getCurrentUserProfile(context: Context): GHUserProfile?` - Composes existing helpers: `getCachedUser()`, `getUser()`, `getUserProfile()`.
- `updateCurrentUserProfile( context: Context, name: String, bio: String, company: String, location: String, blog: String ): Boolean` - Updates via `/user` and returns success/result state.
- `getEmailEntries(context: Context): List<GHEmailEntry>` - Fetches `/user/emails` and parses the response.
- `addEmailAddress(context: Context, email: String): Boolean` - Creates/adds via `/user/emails` and returns success/result state.
- `deleteEmailAddress(context: Context, email: String): Boolean` - Deletes/removes via `/user/emails` and returns success/result state.
- `setEmailVisibility(context: Context, visibility: String): Boolean` - Updates via `/user/email/visibility` and returns success/result state.
- `getSshKeysNative(context: Context): List<GHUserKeyEntry>` - Fetches `/user/keys` and parses the response.
- `getSshSigningKeysNative(context: Context): List<GHUserKeyEntry>` - Fetches `/user/ssh_signing_keys` and parses the response.
- `getGpgKeysNative(context: Context): List<GHUserKeyEntry>` - Fetches `/user/gpg_keys` and parses the response.
- `addSshKeyNative(context: Context, title: String, key: String): Boolean` - Creates/adds via `/user/keys` and returns success/result state.
- `addSshSigningKeyNative(context: Context, title: String, key: String): Boolean` - Creates/adds via `/user/ssh_signing_keys` and returns success/result state.
- `addGpgKeyNative(context: Context, armoredKey: String): Boolean` - Creates/adds via `/user/gpg_keys` and returns success/result state.
- `deleteSshKeyNative(context: Context, id: Long): Boolean` - Deletes/removes via `/user/keys/$id` and returns success/result state.
- `deleteSshSigningKeyNative(context: Context, id: Long): Boolean` - Deletes/removes via `/user/ssh_signing_keys/$id` and returns success/result state.
- `deleteGpgKeyNative(context: Context, id: Long): Boolean` - Deletes/removes via `/user/gpg_keys/$id` and returns success/result state.
- `getSocialAccountsNative(context: Context): List<GHSocialAccountEntry>` - Fetches `/user/social_accounts` and parses the response.
- `addSocialAccountNative(context: Context, url: String): Boolean` - Creates/adds via `/user/social_accounts` and returns success/result state.
- `deleteSocialAccountNative(context: Context, url: String): Boolean` - Deletes/removes via `/user/social_accounts` and returns success/result state.
- `getFollowersNative(context: Context): List<GHFollowerEntry>` - Fetches `/user/followers?per_page=100` and parses the response.
- `getFollowingNative(context: Context): List<GHFollowerEntry>` - Fetches `/user/following?per_page=100` and parses the response.
- `getBlockedUsersNative(context: Context): List<GHBlockedEntry>` - Fetches `/user/blocks?per_page=100` and parses the response.
- `blockUserNative(context: Context, username: String): Boolean` - Blocks an encoded username through the authenticated user block endpoint and returns success state.
- `unblockUserNative(context: Context, username: String): Boolean` - Unblocks an encoded username through the authenticated user block endpoint and returns success state.
- `getInteractionLimitNative(context: Context): GHInteractionLimitEntry?` - Fetches `/user/interaction-limits` and parses the response.
- `setInteractionLimitNative(context: Context, limit: String, expiry: String): Boolean` - Updates via `/user/interaction-limits` and returns success/result state.
- `removeInteractionLimitNative(context: Context): Boolean` - Deletes/removes via `/user/interaction-limits` and returns success/result state.
- `getRateLimitSummaryNative(context: Context): String` - Fetches `/rate_limit` and parses the response.

### Repository Administration (lines 2382-2721)
- Public methods: **18**
- Summary: Repository settings, topics, tags, transfer, branch protection, collaborators, teams, and related parsers.
- `getRepoSettings(context: Context, owner: String, repo: String): GHRepoSettings?` - Fetches `/repos/$owner/$repo` and parses the response.
- `updateRepoSettings( context: Context, owner: String, repo: String, name: String? = null, description: String? = null, homepage: String? = null, isPrivate: Boolean? = null, hasIssues: Boolean? = null, hasProjects: Boolean? = null, hasWiki: Boolean? = null, hasDiscussions: Boolean? = null, allowForking: Boolean? = null, isTemplate: Boolean? = null, archived: Boolean? = null, topics: List<String>? = null, allowMergeCommit: Boolean? = null, allowSquashMerge: Boolean? = null, allowRebaseMerge: Boolean? = null, deleteBranchOnMerge: Boolean? = null ): Boolean` - Updates via `/repos/$owner/$repo` and returns success/result state.
- `getRepoTopics(context: Context, owner: String, repo: String): List<String>` - Fetches `/repos/$owner/$repo/topics` and parses the response.
- `getRepoTags(context: Context, owner: String, repo: String, page: Int = 1): List<GHTag>` - Fetches `/repos/$owner/$repo/tags?per_page=50&page=$page` and parses the response.
- `replaceRepoTopics(context: Context, owner: String, repo: String, topics: List<String>): Boolean` - Updates via `/repos/$owner/$repo/topics` and returns success/result state.
- `transferRepo(context: Context, owner: String, repo: String, newOwner: String, newName: String? = null): Boolean` - Calls `/repos/$owner/$repo/transfer` and returns success/result state.
- `getBranchProtection(context: Context, owner: String, repo: String, branch: String): GHBranchProtection?` - Fetches `/repos/$owner/$repo/branches/$encodedBranch/protection` and parses the response.
- `updateBranchProtection( context: Context, owner: String, repo: String, branch: String, requiredStatusChecks: GHRequiredStatusChecks? = null, requiredPRReviews: GHRequiredPRReviews? = null, restrictions: GHBranchRestrictions? = null, allowForcePushes: Boolean? = null, allowDeletions: Boolean? = null, requiredConversationResolution: Boolean? = null, enforceAdmins: Boolean? = null ): Boolean` - Updates via `/repos/$owner/$repo/branches/$encodedBranch/protection` and returns success/result state.
- `deleteBranchProtection(context: Context, owner: String, repo: String, branch: String): Boolean` - Deletes/removes via `/repos/$owner/$repo/branches/$encodedBranch/protection` and returns success/result state.
- `getCollaborators(context: Context, owner: String, repo: String): List<GHCollaborator>` - Fetches `/repos/$owner/$repo/collaborators?per_page=100` and parses the response.
- `addCollaborator(context: Context, owner: String, repo: String, username: String, permission: String = "push"): Boolean` - Adds an encoded collaborator username with a permission payload and returns success state.
- `removeCollaborator(context: Context, owner: String, repo: String, username: String): Boolean` - Removes an encoded collaborator username from the repository and returns success state.
- `updateCollaboratorPermission(context: Context, owner: String, repo: String, username: String, permission: String): Boolean` - Updates an encoded collaborator username permission and returns success state.
- `getRepoTeams(context: Context, owner: String, repo: String): List<GHRepoTeam>` - Fetches `/repos/$owner/$repo/teams?per_page=100` and parses the response.
- `getOrgTeams(context: Context, org: String): List<GHOrgTeam>` - Fetches teams for an encoded organization and parses the response.
- `addRepoTeam(context: Context, org: String, teamSlug: String, owner: String, repo: String, permission: String): Boolean` - Adds a repository to an encoded organization/team slug with normalized permission and returns success state.
- `updateRepoTeamPermission(context: Context, org: String, teamSlug: String, owner: String, repo: String, permission: String): Boolean` - Composes existing helpers: `addRepoTeam()`.
- `removeRepoTeam(context: Context, org: String, teamSlug: String, owner: String, repo: String): Boolean` - Removes a repository from an encoded organization/team slug and returns success state.

### PR Review Comments / Compare / Timeline (lines 2722-2929)
- Public methods: **13**
- Summary: PR review comments, PR check runs, compare API, user cache clearing, reactions, and issue timeline.
- `getPullRequestReviewComments(context: Context, owner: String, repo: String, pullNumber: Int): List<GHReviewComment>` - Fetches `/repos/$owner/$repo/pulls/$pullNumber/comments?per_page=100` and parses the response.
- `createPullRequestReviewComment( context: Context, owner: String, repo: String, pullNumber: Int, body: String, path: String, line: Int, side: String = "RIGHT", inReplyToId: Long? = null ): Boolean` - Creates/adds via `/repos/$owner/$repo/pulls/$pullNumber/comments` and returns success/result state.
- `updatePullRequestReviewComment(context: Context, owner: String, repo: String, commentId: Long, body: String): Boolean` - Updates via `/repos/$owner/$repo/pulls/comments/$commentId` and returns success/result state.
- `deletePullRequestReviewComment(context: Context, owner: String, repo: String, commentId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/pulls/comments/$commentId` and returns success/result state.
- `getPullRequestCheckRuns(context: Context, owner: String, repo: String, ref: String): List<GHCheckRun>` - Fetches `/repos/$owner/$repo/commits/$encodedRef/check-runs?per_page=100` and parses the response.
- `compareCommits(context: Context, owner: String, repo: String, base: String, head: String): GHCompareResult?` - Calls `/repos/$owner/$repo/compare/$encodedBase...$encodedHead` and returns success/result state.
- `clearGitHubUserCache(context: Context)` - Removes cached token/user profile GitHub preferences.
- `getIssueReactions(context: Context, owner: String, repo: String, issueNumber: Int): List<GHReaction>` - Fetches `/repos/$owner/$repo/issues/$issueNumber/reactions?per_page=100` and parses the response.
- `addIssueReaction(context: Context, owner: String, repo: String, issueNumber: Int, content: String): Boolean` - Creates/adds via `/repos/$owner/$repo/issues/$issueNumber/reactions` and returns success/result state.
- `deleteIssueReaction(context: Context, owner: String, repo: String, reactionId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/reactions/$reactionId` and returns success/result state.
- `getIssueCommentReactions(context: Context, owner: String, repo: String, commentId: Long): List<GHReaction>` - Fetches `/repos/$owner/$repo/issues/comments/$commentId/reactions?per_page=100` and parses the response.
- `addIssueCommentReaction(context: Context, owner: String, repo: String, commentId: Long, content: String): Boolean` - Creates/adds via `/repos/$owner/$repo/issues/comments/$commentId/reactions` and returns success/result state.
- `getIssueTimeline(context: Context, owner: String, repo: String, issueNumber: Int): List<GHTimelineEvent>` - Fetches `/repos/$owner/$repo/issues/$issueNumber/timeline?per_page=100` and parses the response.

### Webhooks (lines 2930-3074)
- Public methods: **12**
- Summary: Webhook CRUD, config, deliveries, ping/test/redelivery, and webhook parsers.
- `getWebhooks(context: Context, owner: String, repo: String): List<GHWebhook>` - Fetches `/repos/$owner/$repo/hooks?per_page=100` and parses the response.
- `getWebhook(context: Context, owner: String, repo: String, hookId: Long): GHWebhook?` - Fetches `/repos/$owner/$repo/hooks/$hookId` and parses the response.
- `createWebhook(context: Context, owner: String, repo: String, config: Map<String, String>, events: List<String>, active: Boolean = true): Boolean` - Creates/adds via `/repos/$owner/$repo/hooks` and returns success/result state.
- `updateWebhook(context: Context, owner: String, repo: String, hookId: Long, config: Map<String, String>, events: List<String>, active: Boolean = true): Boolean` - Updates via `/repos/$owner/$repo/hooks/$hookId` and returns success/result state.
- `pingWebhook(context: Context, owner: String, repo: String, hookId: Long): Boolean` - Calls `/repos/$owner/$repo/hooks/$hookId/pings` and returns success/result state.
- `testWebhook(context: Context, owner: String, repo: String, hookId: Long): Boolean` - Calls `/repos/$owner/$repo/hooks/$hookId/tests` and returns success/result state.
- `getWebhookConfig(context: Context, owner: String, repo: String, hookId: Long): GHWebhookConfig?` - Fetches `/repos/$owner/$repo/hooks/$hookId/config` and parses the response.
- `updateWebhookConfig(context: Context, owner: String, repo: String, hookId: Long, config: Map<String, String>): Boolean` - Updates via `/repos/$owner/$repo/hooks/$hookId/config` and returns success/result state.
- `getWebhookDeliveries(context: Context, owner: String, repo: String, hookId: Long): List<GHWebhookDelivery>` - Fetches `/repos/$owner/$repo/hooks/$hookId/deliveries?per_page=100` and parses the response.
- `getWebhookDelivery(context: Context, owner: String, repo: String, hookId: Long, deliveryId: Long): GHWebhookDelivery?` - Fetches `/repos/$owner/$repo/hooks/$hookId/deliveries/$deliveryId` and parses the response.
- `redeliverWebhookDelivery(context: Context, owner: String, repo: String, hookId: Long, deliveryId: Long): Boolean` - Calls `/repos/$owner/$repo/hooks/$hookId/deliveries/$deliveryId/attempts` and returns success/result state.
- `deleteWebhook(context: Context, owner: String, repo: String, hookId: Long): Boolean` - Deletes/removes via `/repos/$owner/$repo/hooks/$hookId` and returns success/result state.

### Discussions (lines 3075-3325)
- Public methods: **8**
- Summary: GraphQL discussion list/categories/detail/create/update/delete/comments, plus repository node-id lookup and parsers.
- `getDiscussions(context: Context, owner: String, repo: String): List<GHDiscussion>` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `getDiscussionCategories(context: Context, owner: String, repo: String): List<GHDiscussionCategory>` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `getDiscussionDetail(context: Context, owner: String, repo: String, discussionNumber: Int): GHDiscussion?` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `createDiscussion(context: Context, owner: String, repo: String, title: String, body: String, categoryId: String): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `updateDiscussion(context: Context, discussionId: String, title: String, body: String, categoryId: String? = null): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `deleteDiscussion(context: Context, discussionId: String): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `getDiscussionComments(context: Context, owner: String, repo: String, discussionNumber: Int): List<GHComment>` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `addDiscussionComment(context: Context, discussionId: String, body: String): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.

### Projects (lines 3326-3962)
- Public methods: **24**
- Summary: Classic Projects REST methods and Project V2 GraphQL detail/mutation/item/field operations.
- `getRepoProjects(context: Context, owner: String, repo: String, state: String = "all"): List<GHProject>` - Fetches `/repos/$owner/$repo/projects?state=$state&per_page=100` and parses the response.
- `getProject(context: Context, projectId: Long): GHProject?` - Fetches `/projects/$projectId` and parses the response.
- `createRepoProject(context: Context, owner: String, repo: String, name: String, body: String): GHProject?` - Creates/adds via `/repos/$owner/$repo/projects` and returns success/result state.
- `updateProject(context: Context, projectId: Long, name: String, body: String, state: String): Boolean` - Updates via `/projects/$projectId` and returns success/result state.
- `deleteProject(context: Context, projectId: Long): Boolean` - Deletes/removes via `/projects/$projectId` and returns success/result state.
- `getProjectColumns(context: Context, projectId: Long): List<GHProjectColumn>` - Fetches `/projects/$projectId/columns?per_page=100` and parses the response.
- `createProjectColumn(context: Context, projectId: Long, name: String): GHProjectColumn?` - Creates/adds via `/projects/$projectId/columns` and returns success/result state.
- `getProjectCards(context: Context, columnId: Long): List<GHProjectCard>` - Fetches `/projects/columns/$columnId/cards?archived_state=all&per_page=100` and parses the response.
- `createProjectCard(context: Context, columnId: Long, note: String): GHProjectCard?` - Creates/adds via `/projects/columns/$columnId/cards` and returns success/result state.
- `moveProjectCard(context: Context, cardId: Long, position: String, columnId: Long? = null): Boolean` - Calls `/projects/columns/cards/$cardId/moves` and returns success/result state.
- `deleteProjectCard(context: Context, cardId: Long): Boolean` - Deletes/removes via `/projects/columns/cards/$cardId` and returns success/result state.
- `getRepoProjectsV2(context: Context, owner: String, repo: String): List<GHProjectV2>` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `getProjectV2Detail(context: Context, projectId: String): GHProjectV2Detail?` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `updateProjectV2(context: Context, projectId: String, title: String, shortDescription: String, readme: String, closed: Boolean, isPublic: Boolean): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `createProjectV2Field(context: Context, projectId: String, name: String, dataType: String, options: List<String> = emptyList()): GHProjectV2Field?` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `updateProjectV2Field(context: Context, field: GHProjectV2Field, name: String, options: List<String> = emptyList()): GHProjectV2Field?` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `deleteProjectV2Field(context: Context, fieldId: String): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `addProjectV2DraftIssue(context: Context, projectId: String, title: String, body: String): GHProjectV2Item?` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `updateProjectV2DraftIssue(context: Context, draftIssueId: String, title: String, body: String): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `deleteProjectV2Item(context: Context, projectId: String, itemId: String): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `archiveProjectV2Item(context: Context, projectId: String, itemId: String, archived: Boolean): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `updateProjectV2ItemFieldValue(context: Context, projectId: String, itemId: String, field: GHProjectV2Field, value: String): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `clearProjectV2ItemFieldValue(context: Context, projectId: String, itemId: String, fieldId: String): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.
- `moveProjectV2Item(context: Context, projectId: String, itemId: String, afterId: String?): Boolean` - Runs a GitHub GraphQL query or mutation and maps the returned JSON into the method result.

### Packages (lines 3963-4042)
- Public methods: **6**
- Summary: User/org package listing, package detail/delete, version listing/delete, and package parsers.
- `getUserPackages(context: Context, username: String, packageType: String = "all"): List<GHPackage>` - Fetches packages for an encoded username and parses the response.
- `getOrgPackages(context: Context, org: String, packageType: String = "all"): List<GHPackage>` - Fetches packages for an encoded organization and parses the response.
- `getPackage(context: Context, ownerType: String, owner: String, packageType: String, packageName: String): GHPackage?` - Fetches one package through `packageOwnerPath()` with encoded package type/name and parses the response.
- `deletePackage(context: Context, ownerType: String, owner: String, packageType: String, packageName: String): Boolean` - Deletes one package through `packageOwnerPath()` with encoded package type/name and returns success state.
- `getPackageVersions(context: Context, ownerType: String, owner: String, packageType: String, packageName: String): List<GHPackageVersion>` - Fetches package versions through `packageOwnerPath()` with encoded package type/name and parses the response.
- `deletePackageVersion(context: Context, ownerType: String, owner: String, packageType: String, packageName: String, versionId: Long): Boolean` - Deletes one package version through `packageOwnerPath()` with encoded package type/name and returns success state.

### Rulesets (lines 4065-4246)
- Public methods: **7**
- Summary: Repository rulesets, rule suites, create/update/delete payloads, and ruleset parsers.
- `getRulesets(context: Context, owner: String, repo: String): List<GHRuleset>` - Fetches `/repos/$owner/$repo/rulesets?per_page=100` and parses the response.
- `getRulesetDetail(context: Context, owner: String, repo: String, rulesetId: Int): GHRulesetDetail?` - Fetches `/repos/$owner/$repo/rulesets/$rulesetId` and parses the response.
- `getRuleSuites(context: Context, owner: String, repo: String): List<GHRuleSuite>` - Fetches `/repos/$owner/$repo/rule-suites?per_page=100` and parses the response.
- `getRuleSuite(context: Context, owner: String, repo: String, suiteId: Long): GHRuleSuite?` - Fetches `/repos/$owner/$repo/rule-suites/$suiteId` and parses the response.
- `createRuleset( context: Context, owner: String, repo: String, name: String, target: String, enforcement: String, includeRefs: List<String>, excludeRefs: List<String>, rulesJson: String ): GHRulesetDetail?` - Creates/adds via `/repos/$owner/$repo/rulesets` and returns success/result state.
- `updateRuleset( context: Context, owner: String, repo: String, rulesetId: Int, name: String, target: String, enforcement: String, includeRefs: List<String>, excludeRefs: List<String>, rulesJson: String ): GHRulesetDetail?` - Updates via `/repos/$owner/$repo/rulesets/$rulesetId` and returns success/result state.
- `deleteRuleset(context: Context, owner: String, repo: String, rulesetId: Int): Boolean` - Deletes/removes via `/repos/$owner/$repo/rulesets/$rulesetId` and returns success/result state.

### Security / Community (lines 4255-4510)
- Public methods: **13**
- Summary: Dependabot/code scanning/secret scanning/security advisories/community profile/security settings plus parsers and final shared repo/error helpers.
- `getDependabotAlerts(context: Context, owner: String, repo: String): List<GHDependabotAlert>` - Fetches `/repos/$owner/$repo/dependabot/alerts?per_page=100` and parses the response.
- `getDependabotAlert(context: Context, owner: String, repo: String, number: Int): GHDependabotAlert?` - Fetches `/repos/$owner/$repo/dependabot/alerts/$number` and parses the response.
- `getCodeScanningAlerts(context: Context, owner: String, repo: String): List<GHCodeScanningAlert>` - Fetches `/repos/$owner/$repo/code-scanning/alerts?per_page=100` and parses the response.
- `getCodeScanningAlert(context: Context, owner: String, repo: String, number: Int): GHCodeScanningAlert?` - Fetches `/repos/$owner/$repo/code-scanning/alerts/$number` and parses the response.
- `getSecretScanningAlerts(context: Context, owner: String, repo: String): List<GHSecretScanningAlert>` - Fetches `/repos/$owner/$repo/secret-scanning/alerts?per_page=100` and parses the response.
- `getSecretScanningAlert(context: Context, owner: String, repo: String, number: Int): GHSecretScanningAlert?` - Fetches `/repos/$owner/$repo/secret-scanning/alerts/$number` and parses the response.
- `getRepositorySecurityAdvisories(context: Context, owner: String, repo: String): List<GHRepositorySecurityAdvisory>` - Fetches `/repos/$owner/$repo/security-advisories?per_page=100&sort=updated&direction=desc` and parses the response.
- `getRepositorySecurityAdvisory(context: Context, owner: String, repo: String, ghsaId: String): GHRepositorySecurityAdvisory?` - Fetches `/repos/$owner/$repo/security-advisories/$encoded` and parses the response.
- `getCommunityProfile(context: Context, owner: String, repo: String): GHCommunityProfile?` - Fetches `/repos/$owner/$repo/community/profile` and parses the response.
- `getRepositorySecuritySettings(context: Context, owner: String, repo: String): GHRepositorySecuritySettings` - Fetches `/repos/$owner/$repo/automated-security-fixes`, `/repos/$owner/$repo/vulnerability-alerts`, `/repos/$owner/$repo/private-vulnerability-reporting` and parses the response.
- `setAutomatedSecurityFixes(context: Context, owner: String, repo: String, enabled: Boolean): Boolean` - Updates via `/repos/$owner/$repo/automated-security-fixes` and returns success/result state.
- `setVulnerabilityAlerts(context: Context, owner: String, repo: String, enabled: Boolean): Boolean` - Updates via `/repos/$owner/$repo/vulnerability-alerts` and returns success/result state.
- `setPrivateVulnerabilityReporting(context: Context, owner: String, repo: String, enabled: Boolean): Boolean` - Updates via `/repos/$owner/$repo/private-vulnerability-reporting` and returns success/result state.

## 3. Shared infrastructure

- **Stored configuration and auth state** (lines 18-22, 24-36, 2845-2852): Private constants define API/prefs keys; token/user JSON are stored in `SharedPreferences` and read by public auth/profile helpers.
- **Generic REST transport** (`request()` lines 37-69): Runs on `Dispatchers.IO`, builds `HttpURLConnection`, injects `Accept`, `User-Agent`, optional extra headers, optional bearer token, optional JSON body, 15s connect/read timeouts, and returns `ApiResult(success, body, code)`. It is called by most REST methods.
- **GraphQL transport** (`graphql()` lines 70-84): Builds a JSON body with `query` and `variables`, calls `request(context, "/graphql", "POST", ...)`, returns `data` or `null` if request/errors/parsing fails. Used by Discussions and Project V2 domains.
- **Auth header injection** (`request()` lines 40-48 and direct transfer methods such as `cloneRepo`, `downloadReleaseAsset`, `uploadReleaseAsset`, `getWorkflowRunLogs`, `getJobLogs`, `getRedirectLocationOrText`): Generic REST calls inject `Authorization: Bearer <token>` when a token exists; several direct `HttpURLConnection` file/log paths repeat this manually.
- **Error mapping** (`ApiResult` line 4512 and `apiErrorMessage()` lines 4514-4535): `request()` returns raw success/body/code; most callers map failures to `false`, `null`, or empty collections. `apiErrorMessage()` is used by detailed workflow dispatch to expose GitHub error text.
- **JSON parsing** (Inline `JSONObject`/`JSONArray` parsing plus private parsers throughout lines 542-4510): Many domains parse inline; repeated private parse helpers map Pull Review, Release, Actions, Search, Webhook, Discussion, Project, Package, Ruleset, Security, Community, and Repo payloads into data classes.
- **URL and query helpers** (`refQuery()` lines 143-147, `packageOwnerPath()` lines 4010-4016, `getContentType()` lines 905-918, `URLEncoder.encode(...)` inline): Branch refs, package owner paths, content types, search queries, tags, topics, and asset names are encoded close to the method that needs them.
- **Pagination** (implemented inline in methods including `getBranches()`, `getWorkflows()`, `getRepoProjectsV2()`, and page parameters across list methods): There is no shared pagination helper; list methods either accept `page`/`perPage` or manually loop until fewer results are returned.
- **Caching** (`getUser()` lines 85-104, `getCachedUser()` lines 105-114, `clearGitHubUserCache()` lines 2845-2852): Only authenticated user JSON is cached in SharedPreferences. Repository/API response caching is not present in this file.
- **Logging** (`request()` line 64 and `getUser()` catch logging): Logging is minimal and mostly catches transport or user parse errors under tag `GH`.
- **Rate limits** (`getRateLimitSummaryNative()` lines 2366-2381): The file has a rate-limit summary endpoint reader, but `request()` does not inspect `X-RateLimit-*` headers or retry automatically.

## 4. Dependency graph (from outside)

| Caller (file) | Methods called from GithubManager |
|---------------|-----------------------------------|
| `app/src/main/java/com/glassfiles/data/github/GitHubRepoSettingsManager.kt` | `getToken` |
| `app/src/main/java/com/glassfiles/ui/GitHubUploadDialogs.kt` | `getBranches`, `getRepos`, `uploadFileFromPath`, `uploadMultipleFiles` |
| `app/src/main/java/com/glassfiles/ui/GlassFilesApp.kt` | `getCachedUser` |
| `app/src/main/java/com/glassfiles/ui/components/FileContextMenu.kt` | `isLoggedIn` |
| `app/src/main/java/com/glassfiles/ui/screens/BrowseScreen.kt` | `getCachedUser` |
| `app/src/main/java/com/glassfiles/ui/screens/BuildsScreen.kt` | `dispatchWorkflow`, `getWorkflowDispatchSchema`, `getWorkflowRuns` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubActionsModule.kt` | `cancelWorkflowRun`, `createOrUpdateRepoActionsSecret`, `createReleaseDetailed`, `createRepoActionsVariable`, `createRepoRunnerRegistrationToken`, `createRepoRunnerRemoveToken`, `deleteActionsCache`, `deleteArtifact`, `deleteRepoActionsSecret`, `deleteRepoActionsVariable`, `deleteRepoSelfHostedRunner`, `deleteWorkflowRun`, `deleteWorkflowRunLogs`, `disableWorkflow`, `dispatchWorkflowDetailed`, `downloadArtifact`, `enableWorkflow`, `forceCancelWorkflowRun`, `getActionsCaches`, `getActionsCacheUsage`, `getBranches`, `getCachedUser`, `getCheckRunAnnotations`, `getCheckRunsForRef`, `getJobLogs`, `getPendingDeployments`, `getReleaseByTag`, `getRepoActionsPermissions`, `getRepoActionsRetention`, `getRepoActionsSecrets`, `getRepoActionsVariables`, `getRepoActionsWorkflowPermissions`, `getRepoSelfHostedRunners`, `getRepositoryArtifacts`, `getRunArtifacts`, `getWorkflowDispatchSchema`, `getWorkflowRun`, `getWorkflowRunAttempt`, `getWorkflowRunAttemptJobs`, `getWorkflowRunJobs`, `getWorkflowRunReviewHistory`, `getWorkflowRuns`, `getWorkflowRunUsage`, `getWorkflows`, `rerunFailedJobs`, `rerunJob`, `rerunWorkflow`, `reviewPendingDeployments`, `updateRepoActionsVariable`, `uploadReleaseAssetDetailed` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubAdvancedSearchModule.kt` | `searchCommitsAdvanced`, `searchIssuesAdvanced`, `searchLabels`, `searchRepos`, `searchTopics`, `searchUsers` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubBranchProtectionModule.kt` | `deleteBranchProtection`, `getBranchProtection`, `updateBranchProtection` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubCheckRunsModule.kt` | `getPullRequestCheckRuns` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubCodeEditorModule.kt` | `uploadFileWithResult` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubCollaboratorsModule.kt` | `addCollaborator`, `getCollaborators`, `removeCollaborator`, `updateCollaboratorPermission` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubCompareModule.kt` | `compareCommits`, `createPullRequest`, `getBranches` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubDiffModule.kt` | `createPullRequestReviewComment`, `deletePullRequestReviewComment`, `getCommitDiff`, `getPullRequestFiles`, `getPullRequestReviewComments`, `getPullRequests`, `updatePullRequestReviewComment` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubDiscussionsModule.kt` | `addDiscussionComment`, `createDiscussion`, `deleteDiscussion`, `getDiscussionCategories`, `getDiscussionComments`, `getDiscussionDetail`, `getDiscussions`, `updateDiscussion` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubExploreModule.kt` | `getOrganizations`, `getOrgRepos`, `getStarredRepos`, `searchCode` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubGistsAndDialogsModule.kt` | `createBranch`, `createGist`, `createIssue`, `createPullRequest`, `deleteFile`, `deleteGist`, `dispatchWorkflow`, `getGistContent`, `getGists`, `getWorkflowDispatchSchema`, `uploadFile` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubHomeModule.kt` | `createRepo`, `getRepos`, `getUser`, `logout`, `saveToken`, `searchRepos` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubNotificationsModule.kt` | `deleteThreadSubscription`, `getNotifications`, `getThreadSubscription`, `markAllNotificationsRead`, `markNotificationRead`, `setThreadSubscription` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubPackagesModule.kt` | `deletePackage`, `deletePackageVersion`, `getOrganizations`, `getOrgPackages`, `getPackage`, `getPackageVersions`, `getUserPackages` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubProfileModule.kt` | `followUser`, `getCachedUser`, `getUserProfile`, `getUserRepos`, `isFollowing`, `unfollowUser` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubProjectsModule.kt` | `addProjectV2DraftIssue`, `archiveProjectV2Item`, `createProjectCard`, `createProjectColumn`, `createProjectV2Field`, `createRepoProject`, `deleteProject`, `deleteProjectCard`, `deleteProjectV2Field`, `deleteProjectV2Item`, `getProject`, `getProjectCards`, `getProjectColumns`, `getProjectV2Detail`, `getRepoProjects`, `getRepoProjectsV2`, `moveProjectCard`, `moveProjectV2Item`, `updateProject`, `updateProjectV2`, `updateProjectV2DraftIssue`, `updateProjectV2Field`, `updateProjectV2ItemFieldValue` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubReleasesModule.kt` | `createReleaseDetailed`, `deleteRelease`, `deleteReleaseAsset`, `downloadReleaseAsset`, `getCommits`, `getReleases`, `publishRelease`, `updateReleaseDetailed`, `uploadReleaseAssetDetailed` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubRepoModule.kt` | `addComment`, `addIssueCommentReaction`, `addIssueReaction`, `cloneRepo`, `closeIssue`, `deleteIssueComment`, `deletePullRequestReview`, `downloadFile`, `forkRepo`, `getAssignees`, `getBranches`, `getCommitDiff`, `getCommits`, `getContributors`, `getFileContent`, `getIssueCommentReactions`, `getIssueComments`, `getIssueDetail`, `getIssueReactions`, `getIssues`, `getIssueTimeline`, `getLabels`, `getLanguages`, `getMilestones`, `getPullRequestCheckRuns`, `getPullRequestDetail`, `getPullRequestFiles`, `getPullRequestReview`, `getPullRequestReviewComments`, `getPullRequestReviews`, `getPullRequests`, `getReadme`, `getReleases`, `getRepoContents`, `getWorkflowRuns`, `getWorkflows`, `isStarred`, `isWatching`, `lockIssue`, `mergePullRequest`, `removePullRequestReviewers`, `reopenIssue`, `requestPullRequestReviewers`, `starRepo`, `submitPullRequestReview`, `unlockIssue`, `unstarRepo`, `unwatchRepo`, `updateIssueComment`, `updateIssueMeta`, `updatePullRequest`, `updatePullRequestReview`, `watchRepo` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubRepoSettingsModule.kt` | `getRepoSettings`, `getRepoTags`, `replaceRepoTopics`, `updateRepoSettings` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubScreen.kt` | `getCachedUser`, `getUser`, `isLoggedIn`, `logout`, `saveToken` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubSecurityModule.kt` | `createRuleset`, `deleteRuleset`, `getCodeScanningAlert`, `getCodeScanningAlerts`, `getCommunityProfile`, `getDependabotAlert`, `getDependabotAlerts`, `getRepositorySecurityAdvisories`, `getRepositorySecurityAdvisory`, `getRepositorySecuritySettings`, `getRulesetDetail`, `getRulesets`, `getRuleSuite`, `getRuleSuites`, `getSecretScanningAlert`, `getSecretScanningAlerts`, `setAutomatedSecurityFixes`, `setPrivateVulnerabilityReporting`, `setVulnerabilityAlerts`, `updateRuleset` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubSettingsModule.kt` | `addEmailAddress`, `addGpgKeyNative`, `addSocialAccountNative`, `addSshKeyNative`, `addSshSigningKeyNative`, `blockUserNative`, `clearGitHubUserCache`, `deleteEmailAddress`, `deleteGpgKeyNative`, `deleteSocialAccountNative`, `deleteSshKeyNative`, `deleteSshSigningKeyNative`, `followUser`, `getBlockedUsersNative`, `getCachedUser`, `getCurrentUserProfile`, `getEmailEntries`, `getFollowersNative`, `getFollowingNative`, `getGpgKeysNative`, `getInteractionLimitNative`, `getNotifications`, `getOrganizations`, `getRateLimitSummaryNative`, `getSocialAccountsNative`, `getSshKeysNative`, `getSshSigningKeysNative`, `getStarredRepos`, `getToken`, `getUser`, `logout`, `markAllNotificationsRead`, `markNotificationRead`, `removeInteractionLimitNative`, `saveToken`, `setEmailVisibility`, `setInteractionLimitNative`, `unblockUserNative`, `unfollowUser`, `unstarRepo`, `updateCurrentUserProfile` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubTeamsModule.kt` | `addRepoTeam`, `getOrgTeams`, `getRepoTeams`, `removeRepoTeam`, `updateRepoTeamPermission` |
| `app/src/main/java/com/glassfiles/ui/screens/GitHubWebhooksModule.kt` | `createWebhook`, `deleteWebhook`, `getWebhook`, `getWebhookConfig`, `getWebhookDeliveries`, `getWebhookDelivery`, `getWebhooks`, `pingWebhook`, `redeliverWebhookDelivery`, `testWebhook`, `updateWebhook`, `updateWebhookConfig` |
| `app/src/main/java/com/glassfiles/ui/screens/SharedAndFolderScreens.kt` | `isLoggedIn` |

## 5. Coupling observations

- `request()` is the central REST dependency for almost every public REST method; changing its headers, timeout, error behavior, or auth handling affects nearly the whole manager.
- `graphql()` is the central dependency for Discussions and Project V2; those domains share GraphQL null-on-error behavior rather than the REST `ApiResult` surface.
- `getWorkflowDispatchSchema()` calls `getFileContent()` and `parseWorkflowDispatchSchema()`, while `getWorkflowDispatchSchemas()` loops over `getWorkflowDispatchSchema()` for each workflow.
- `dispatchWorkflowDetailed()` is coupled to `apiErrorMessage()` for user-visible dispatch failure text; the simpler dispatch overloads return only `Boolean`.
- Release helpers are layered: `updateRelease()` delegates to `updateReleaseDetailed()`, `publishRelease()` delegates to `updateReleaseDetailed()`, and detailed upload/download paths use direct connections plus release parsers.
- Actions secret mutation is coupled: `createOrUpdateRepoActionsSecret()` first calls `getRepoActionsPublicKey()` before sending the encrypted-value payload placeholder.
- Project V2 field value mutation is coupled: `updateProjectV2ItemFieldValue()` may call `clearProjectV2ItemFieldValue()` when the incoming value is blank.
- Repository team permission update is coupled: `updateRepoTeamPermission()` delegates to `addRepoTeam()`.
- Upload helpers are layered: `uploadFileFromPath()` calls `uploadFile()`, `uploadDirectory()` calls `collectFiles()` and `uploadMultipleFiles()`.
- Discussion creation is coupled to `getRepositoryNodeId()` before running the GraphQL mutation.
- Parsing is domain-local rather than centralized: many public fetch methods depend on private `parse*` helpers adjacent to their domain.

## 6. Hotspots

- `getProjectV2Detail` (lines 3449-3573) - over 100 lines; GraphQL query/mapping; manual JSON assembly/parsing.
- `parseWorkflowDispatchSchema` (lines 1177-1272) - 96 lines; iteration-heavy.
- `uploadMultipleFiles` (lines 325-372) - 48 lines; iteration-heavy; manual JSON assembly/parsing.
- `cloneRepo` (lines 231-276) - 46 lines; iteration-heavy.
- `compareCommits` (lines 2799-2844) - 46 lines; iteration-heavy; manual JSON assembly/parsing.
- `updateBranchProtection` (lines 2540-2584) - 45 lines; manual JSON assembly/parsing.
- `getRepoProjectsV2` (lines 3406-3448) - 43 lines; GraphQL query/mapping; manual JSON assembly/parsing.
- `getBranchProtection` (lines 2498-2539) - 42 lines; iteration-heavy; manual JSON assembly/parsing.
- `getDiscussionComments` (lines 3224-3265) - 42 lines; GraphQL query/mapping; manual JSON assembly/parsing.

## 7. Quick stats summary

| Domain | Methods | Approx. lines | Has tests | Has mock/fake |
|--------|---------|---------------|-----------|---------------|
| Authentication / Session | 6 | ~114 | no | no |
| Repositories / Contents / Branches | 7 | ~76 | no | no |
| Issues / Basic Issue List | 3 | ~40 | no | no |
| Repository Files / Uploads / Branch Mutation | 9 | ~185 | no | no |
| Pull Requests / Reviews | 11 | ~144 | no | no |
| Issues Extended Metadata / Reactions | 9 | ~73 | no | no |
| Repository Social / README Metadata | 7 | ~45 | no | no |
| Releases / Release Assets | 12 | ~241 | no | no |
| Gists | 4 | ~44 | no | no |
| Search / Commit Diff | 2 | ~38 | no | no |
| Actions / Workflows | 57 | ~766 | no | no |
| Notifications / Watching | 9 | ~66 | no | no |
| Advanced Search | 5 | ~148 | no | no |
| Users / Organizations | 8 | ~65 | no | no |
| Issue Metadata / Labels / Milestones | 10 | ~87 | no | no |
| Directory Upload / Account Settings | 28 | ~239 | no | no |
| Repository Administration | 18 | ~340 | no | no |
| PR Review Comments / Compare / Timeline | 13 | ~208 | no | no |
| Webhooks | 12 | ~145 | no | no |
| Discussions | 8 | ~251 | no | no |
| Projects | 24 | ~637 | no | no |
| Packages | 6 | ~80 | no | no |
| Rulesets | 7 | ~182 | no | no |
| Security / Community | 13 | ~256 | no | no |
| **Total** | **288 public / 353 total** | **~5294 file lines** |  |  |

## 8. Notes

- Public methods not found in project-wide `GitHubManager.<method>(...)` calls: **24** unique method names. They may be reserved for future UI, called indirectly, overloaded under another signature, or dead surface: `addLabelsToIssue`, `approveWorkflowRunForFork`, `clearProjectV2ItemFieldValue`, `createLabel`, `createMilestone`, `createRelease`, `deleteBranch`, `deleteIssueReaction`, `deleteLabel`, `deleteRepo`, `getArtifact`, `getRepoActionsPublicKey`, `getRepoTopics`, `getWorkflowDispatchSchemas`, `getWorkflowRunAttemptLogs`, `getWorkflowRunLogs`, `getWorkflowUsage`, `setRepoActionsPermissions`, `setRepoActionsRetention`, `setRepoActionsWorkflowPermissions`, `transferRepo`, `updateRelease`, `uploadDirectory`, `uploadReleaseAsset`.
- No TODO/FIXME comments were found inside `GitHubManager.kt` by grep.
- Method naming is mostly verb-based (`get*`, `create*`, `update*`, `delete*`), but return behavior is inconsistent by domain: failures often return `emptyList()`, `null`, `false`, empty string, or `GHActionResult` depending on method.
- The manager mixes REST and GraphQL in one object. REST methods use `ApiResult`; GraphQL methods return nullable `JSONObject` data and generally collapse errors to `null`/empty results.
- The file contains both API operations and all GitHub data classes; data classes start immediately after the manager object around line 4538.
- Direct transfer methods (`cloneRepo`, release/artifact downloads/uploads, workflow/job logs) bypass `request()` and repeat token/header/connection behavior locally.
- `searchRepos(context, query)` interpolates the query directly into the endpoint, while several other methods use `URLEncoder.encode(...)` for query/path parts.
- No OkHttp/Moshi/serialization dependency is present here; parsing is entirely manual with `org.json`.
