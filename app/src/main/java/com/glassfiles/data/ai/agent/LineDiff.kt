package com.glassfiles.data.ai.agent

/**
 * Tiny line-level LCS diff used by the agent UI to preview destructive
 * tool calls (write_file / edit_file) before the user approves them.
 *
 * Quadratic in `old.size * new.size` and not optimised for very large
 * files — but the agent's destructive tools target single source files
 * (typically < 5_000 lines), and the preview is rendered on a background
 * thread so the cost is acceptable.
 */
object LineDiff {

    /** A single rendered diff line. */
    sealed class Line {
        abstract val text: String
        /** Line is unchanged in both versions. */
        data class Same(override val text: String) : Line()
        /** Line was present in `old` but removed in `new`. */
        data class Del(override val text: String) : Line()
        /** Line was added in `new`. */
        data class Add(override val text: String) : Line()
    }

    /**
     * Compute a line-level diff between [oldText] and [newText]. The two
     * strings are split on `\n`. Returned ordering walks both versions in
     * a canonical interleaving (deletions before additions at the same
     * point) so it can be rendered top-to-bottom as a unified diff.
     */
    fun diff(oldText: String, newText: String): List<Line> {
        val a = oldText.split('\n')
        val b = newText.split('\n')
        val n = a.size
        val m = b.size
        // dp[i][j] = length of LCS of a[i..] and b[j..]
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1
                else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val out = mutableListOf<Line>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                a[i] == b[j] -> { out += Line.Same(a[i]); i++; j++ }
                dp[i + 1][j] >= dp[i][j + 1] -> { out += Line.Del(a[i]); i++ }
                else -> { out += Line.Add(b[j]); j++ }
            }
        }
        while (i < n) { out += Line.Del(a[i]); i++ }
        while (j < m) { out += Line.Add(b[j]); j++ }
        return out
    }

    /**
     * Trim a diff to the changed regions plus [contextLines] of
     * surrounding context, replacing skipped runs of [Line.Same] with a
     * single `null`-bearing separator. Useful for keeping the rendered
     * preview short on large files.
     */
    fun compact(lines: List<Line>, contextLines: Int = 2): List<Line?> {
        if (lines.isEmpty()) return emptyList()
        // Mark which indices must stay (changes + context window).
        val keep = BooleanArray(lines.size)
        for (idx in lines.indices) {
            if (lines[idx] !is Line.Same) {
                val from = (idx - contextLines).coerceAtLeast(0)
                val to = (idx + contextLines).coerceAtMost(lines.size - 1)
                for (k in from..to) keep[k] = true
            }
        }
        val out = mutableListOf<Line?>()
        var skipping = false
        for (idx in lines.indices) {
            if (keep[idx]) {
                out += lines[idx]
                skipping = false
            } else if (!skipping) {
                out += null  // separator marker
                skipping = true
            }
        }
        // Drop a trailing separator if the diff ended on context.
        if (out.isNotEmpty() && out.last() == null) out.removeAt(out.size - 1)
        return out
    }

    /** Add / removed / unchanged counts for the diff. */
    data class Stats(val added: Int, val removed: Int, val unchanged: Int)

    fun stats(lines: List<Line>): Stats {
        var add = 0; var del = 0; var same = 0
        for (l in lines) when (l) {
            is Line.Add -> add++
            is Line.Del -> del++
            is Line.Same -> same++
        }
        return Stats(add, del, same)
    }
}
