package com.glassfiles.data.ai.workspace

import android.content.Context
import com.glassfiles.data.github.GitHubManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Read-only GitHub-backed filesystem used as the real repository layer
 * underneath [WorkspaceFileSystem]. Writes stay in the workspace overlay
 * until a later review/commit stage wires an explicit committer.
 */
class GitHubRepositoryFileSystem(
    private val context: Context,
    private val owner: String,
    private val repo: String,
    private val branch: String,
) : WorkspaceBackingFileSystem {

    override suspend fun read(path: String): String {
        val clean = normalize(path)
        val content = GitHubManager.getFileContent(context, owner, repo, clean, branch)
        if (content.isEmpty() && !exists(clean)) throw FileNotFoundException(clean)
        return content
    }

    override suspend fun readOrNull(path: String): String? =
        runCatching { read(path) }.getOrNull()

    override suspend fun write(path: String, content: String) {
        throw UnsupportedOperationException("GitHub writes must go through workspace commit")
    }

    override suspend fun delete(path: String) {
        throw UnsupportedOperationException("GitHub deletes must go through workspace commit")
    }

    override suspend fun exists(path: String): Boolean {
        val clean = normalize(path)
        if (clean.isBlank()) return true
        val parent = clean.substringBeforeLast('/', "")
        return GitHubManager.getRepoContents(context, owner, repo, parent, branch)
            .any { it.path == clean }
    }

    override suspend fun list(directory: String): List<String> {
        val clean = normalize(directory)
        return withContext(Dispatchers.IO) {
            GitHubManager.getRepoContents(context, owner, repo, clean, branch)
                .map { it.path.trim('/') }
                .filter { it.isNotBlank() }
                .sorted()
        }
    }

    override fun normalize(path: String): String {
        val clean = path.trim().trim('/').replace('\\', '/')
        if (clean.isBlank() || clean == ".") return ""
        require(clean.split('/').none { it == ".." }) { "Parent traversal is not allowed: $path" }
        require(!clean.startsWith("/")) { "Absolute paths are not allowed: $path" }
        return clean
    }
}
