# GlassFiles GitHub Module

This README describes the local GitHub module architecture in GlassFiles: where the code lives, how screens are split, what style rules apply, and how to add new GitHub features without reintroducing Material UI or duplicating transport logic.

## Scope

The GitHub module is the in-app GitHub client. It covers:

- authentication and profile state;
- repository browsing and search;
- file tree, README rendering, diff and code editor;
- issues, pull requests, reviews and comments;
- commits, branches and compare views;
- Actions, workflow runs, logs, artifacts and checks;
- releases, assets, gists, discussions, projects, packages, teams, webhooks and security settings;
- AI Agent GitHub tools through `GitHubToolExecutor`.

The module is Android/Compose UI backed by direct GitHub REST/GraphQL helpers. It is not a standalone library.

## Main Files

### Data Layer

- `app/src/main/java/com/glassfiles/data/github/GitHubManager.kt`
  Main GitHub API facade. Contains auth/session helpers, REST/GraphQL request helpers, parsers and most endpoint wrappers.

- `app/src/main/java/com/glassfiles/data/github/GitHubRepoSettingsManager.kt`
  Repository settings APIs that are large enough to keep separate from `GitHubManager`.

- `app/src/main/java/com/glassfiles/data/github/GitHubSecretCrypto.kt`
  Local secret/token crypto helpers.

- `app/src/main/java/com/glassfiles/data/github/KernelErrorPatterns.kt`
  Known error pattern helpers used by Actions/log analysis flows.

### UI Entry Points

- `app/src/main/java/com/glassfiles/ui/screens/GitHubScreen.kt`
  Top-level GitHub module entry point and navigation shell.

- `app/src/main/java/com/glassfiles/ui/screens/GitHubHomeModule.kt`
  Main signed-in home/dashboard surface.

- `app/src/main/java/com/glassfiles/ui/screens/GitHubRepoModule.kt`
  Repository detail screen and tab host for repo-level views.

- `app/src/main/java/com/glassfiles/ui/screens/GitHubSharedUiModule.kt`
  Shared terminal-style GitHub UI primitives. Prefer these over creating per-screen button/card/input styles.

### Repo Tabs And Feature Screens

- `GitHubActionsModule.kt` - workflow runs, jobs, logs, artifacts, checks, reruns and dispatch.
- `GitHubAdvancedSearchModule.kt` - advanced GitHub search flows.
- `GitHubBranchProtectionModule.kt` - branch protection UI.
- `GitHubCheckRunsModule.kt` - check run presentation.
- `GitHubCodeEditorModule.kt` - file editor, search, outline, preview and commit flow.
- `GitHubCollaboratorsModule.kt` - collaborators/access surfaces.
- `GitHubCompareModule.kt` - compare refs and changed files.
- `GitHubDiffModule.kt` - reusable diff rendering.
- `GitHubDiscussionsModule.kt` - discussions UI.
- `GitHubExploreModule.kt` - explore/search/discovery.
- `GitHubGistsAndDialogsModule.kt` - gists and shared dialogs.
- `GitHubMarkdownModule.kt` - lightweight markdown rendering for README/comments.
- `GitHubNotificationsModule.kt` - GitHub notifications.
- `GitHubPackagesModule.kt` - packages.
- `GitHubProfileModule.kt` - profile/org style surfaces.
- `GitHubProjectsModule.kt` - projects.
- `GitHubReleasesModule.kt` - releases and assets.
- `GitHubRepoSettingsModule.kt` / `GitHubRepoSettingsScreen.kt` - repository settings UI.
- `GitHubSecurityModule.kt` - security, alerts, rules and related settings.
- `GitHubSettingsModule.kt` - module-level GitHub settings.
- `GitHubTeamsModule.kt` - teams.
- `GitHubWebhooksModule.kt` - webhooks.
- `GitHubGlyphs.kt` - shared terminal glyph constants.

## Design Rules

The GitHub module must match the AI/GitHub terminal style.

Required:

- use `AiModuleTheme`, `AiModuleSurface`, `AiModuleText`, `AiModuleIcon`, `AiModuleIconButton` and shared GitHub terminal primitives;
- use `JetBrainsMono` for terminal text, status labels, code-like labels and compact metadata;
- use bordered, low-radius terminal controls;
- keep buttons symmetric and compact;
- prefer bottom sheets/dialogs that match the terminal style;
- keep repeated repo/list items dense and scannable.

Forbidden:

- no `androidx.compose.material3` imports in `GitHub*.kt`;
- no Material `DropdownMenu`, `DropdownMenuItem`, `MaterialTheme`, `TextButton`, `OutlinedTextField`, `FloatingActionButton`, `CircularProgressIndicator` in GitHub module screens;
- no card-heavy marketing layout;
- no unrelated palette/theme changes inside feature work.

If a feature needs a new reusable control, add it to `GitHubSharedUiModule.kt` or `AiModulePrimitives.kt` instead of styling it inline in one screen.

## Data Flow

Typical read flow:

1. UI screen receives `Context`, repo owner/name and optional branch/ref.
2. Screen calls `GitHubManager` or `GitHubRepoSettingsManager` from a coroutine.
3. API helper performs direct HTTP/GraphQL call.
4. Data classes are returned to UI.
5. UI renders terminal-style rows, tabs, dialogs or bottom sheets.

Typical write flow:

1. User opens an action from a terminal-style button/menu.
2. UI collects required fields in a terminal-style dialog or sheet.
3. UI calls the relevant manager method.
4. UI shows success/error inline, toast/snackbar, or refreshes the list.

Write operations should be explicit in UI. Avoid hidden mutation on simple row taps.

## AI Agent Integration

GitHub agent tools live in:

- `app/src/main/java/com/glassfiles/data/ai/agent/AiTool.kt`
- `app/src/main/java/com/glassfiles/data/ai/agent/GitHubToolExecutor.kt`
- `app/src/main/java/com/glassfiles/data/ai/agent/AgentToolRegistry.kt`
- `app/src/main/java/com/glassfiles/ui/screens/AiAgentScreen.kt`

The agent should use GitHub tools for authenticated repository operations and local tools only for the chat/session workspace. In chat-only mode, repository assumptions must not leak into the prompt.

When adding a GitHub capability that should be available to the agent:

1. Add or reuse a `GitHubManager` method.
2. Add an `AiTool` definition if the capability is model-callable.
3. Add metadata in `AgentToolRegistry`.
4. Route execution through `GitHubToolExecutor`.
5. Ensure write/destructive operations go through approval policy.

## Adding A New GitHub Feature

Use this checklist:

- identify whether the endpoint belongs in `GitHubManager` or `GitHubRepoSettingsManager`;
- add a small data class if existing models do not fit;
- keep endpoint parsing close to the manager method;
- add UI in the narrowest existing module;
- reuse `GitHubSharedUiModule.kt` primitives;
- keep loading/error/empty states terminal-style;
- do not add Material3;
- update this README if the new feature adds a new module or convention.

## Verification Policy

For this project, do not run Gradle unless explicitly requested.

Static checks that are safe:

```bash
rg -n "androidx\\.compose\\.material3|\\bDropdownMenu\\b|\\bDropdownMenuItem\\b|\\bMaterialTheme\\b|\\bTextButton\\b|\\bOutlinedTextField\\b|\\bFloatingActionButton\\b|\\bCircularProgressIndicator\\b" app/src/main/java/com/glassfiles/ui/screens/GitHub*.kt
git diff --check
```

Known generated/local scratch files should not be committed unless explicitly requested.

## Related Docs

- `GITHUB_MANAGER_MAP.md` - structural map of `GitHubManager.kt`.
- `GITHUB_API_ANALYSIS.md` - API analysis notes.
- `GITHUB_SETTINGS_API.md` - settings API notes.
- `WORKLOG_GITHUB_MODULES.md` - historical worklog.
