package com.glassfiles.data.terminal

import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class SessionRecorder {
    private var writer: FileWriter? = null
    private var file: File? = null
    var isRecording: Boolean = false
        private set

    fun start(outputDir: File): String {
        outputDir.mkdirs()
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val f = File(outputDir, "session_${sdf.format(Date())}.log")
        file = f
        writer = FileWriter(f, true)
        writer?.write("=== Session recording started: ${Date()} ===\n")
        writer?.flush()
        isRecording = true
        return f.absolutePath
    }

    fun write(data: String) {
        if (!isRecording) return
        try {
            // Strip ANSI escape codes for readable log
            val clean = data.replace(Regex("\u001B\\[[0-9;]*[a-zA-Z]"), "")
                .replace(Regex("\u001B\\][^\u0007]*\u0007"), "")
            if (clean.isNotEmpty()) {
                writer?.write(clean)
                writer?.flush()
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            writer?.write("\n=== Session recording ended: ${Date()} ===\n")
            writer?.flush()
            writer?.close()
        } catch (_: Exception) {}
        writer = null
        isRecording = false
    }

    fun getFilePath(): String? = file?.absolutePath
}
