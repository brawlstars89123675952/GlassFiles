package com.glassfiles.data.terminal

object PtyNative {
    init { System.loadLibrary("glassfiles-pty") }

    /** Create subprocess with PTY. Returns [masterFd, pid] or null on error. */
    external fun nativeCreate(
        cmd: String, args: Array<String>?, env: Array<String>?,
        rows: Int, cols: Int
    ): IntArray?

    external fun nativeRead(fd: Int, buf: ByteArray): Int
    external fun nativeWrite(fd: Int, data: ByteArray, len: Int): Int
    external fun nativeResize(fd: Int, rows: Int, cols: Int)
    external fun nativeClose(fd: Int)
    external fun nativeWaitFor(pid: Int): Int
}
