package com.glassfiles.data.terminal

import android.util.Base64
import android.util.Log

/**
 * Wrapper around a native PTY subprocess.
 * Creates a pseudoterminal and forks a child process.
 */
class PtyProcess private constructor(
    private var masterFd: Int,
    private var pid: Int
) {
    val isAlive: Boolean get() = masterFd >= 0 && pid > 0

    fun write(data: ByteArray) {
        if (masterFd >= 0) PtyNative.nativeWrite(masterFd, data, data.size)
    }

    fun write(text: String) = write(text.toByteArray())

    /**
     * Read available bytes from PTY. Returns bytes read or -1 on EOF/error.
     */
    fun read(buffer: ByteArray): Int {
        return if (masterFd >= 0) PtyNative.nativeRead(masterFd, buffer) else -1
    }

    fun resize(rows: Int, cols: Int) {
        if (masterFd >= 0) PtyNative.nativeResize(masterFd, rows, cols)
    }

    fun destroy() {
        if (masterFd >= 0) {
            PtyNative.nativeClose(masterFd)
            masterFd = -1
        }
        if (pid > 0) {
            try { android.os.Process.sendSignal(pid, 9) } catch (_: Exception) {}
            pid = -1
        }
    }

    fun waitFor(): Int {
        return if (pid > 0) PtyNative.nativeWaitFor(pid) else -1
    }

    companion object {
        private const val TAG = "PtyProcess"

        /**
         * Start a new PTY process.
         * @param cmd Command to execute (e.g. "/system/bin/sh")
         * @param args Arguments
         * @param env Environment variables as "KEY=VALUE" strings
         * @param rows Terminal rows
         * @param cols Terminal columns
         */
        fun start(
            cmd: String,
            args: Array<String> = emptyArray(),
            env: Array<String> = emptyArray(),
            rows: Int = 24,
            cols: Int = 80
        ): PtyProcess? {
            return try {
                val result = PtyNative.nativeCreate(cmd, args, env, rows, cols)
                if (result != null && result.size == 2 && result[0] >= 0) {
                    Log.d(TAG, "PTY created: fd=${result[0]} pid=${result[1]}")
                    PtyProcess(result[0], result[1])
                } else {
                    Log.e(TAG, "Failed to create PTY")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "PTY error: ${e.message}")
                null
            }
        }
    }
}
