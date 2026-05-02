package com.glassfiles.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveHelper {

    enum class ArchiveFormat(val ext: String) { ZIP("zip"), TAR("tar"), TAR_GZ("tar.gz"), SEVEN_Z("7z") }

    /** Detect format from extension */
    fun detectFormat(file: File): ArchiveFormat? = when {
        file.name.endsWith(".zip", true) ||
            file.name.endsWith(".jar", true) ||
            file.name.endsWith(".aar", true) -> ArchiveFormat.ZIP
        file.name.endsWith(".tar.gz", true) || file.name.endsWith(".tgz", true) -> ArchiveFormat.TAR_GZ
        file.name.endsWith(".tar", true) -> ArchiveFormat.TAR
        file.name.endsWith(".7z", true) -> ArchiveFormat.SEVEN_Z
        else -> null
    }

    // ═══════════════════════════════════
    // Compress
    // ═══════════════════════════════════

    /** Compress file or directory to specified format */
    suspend fun compress(source: File, format: ArchiveFormat = ArchiveFormat.ZIP, onProgress: (Float) -> Unit = {}): File? =
        withContext(Dispatchers.IO) {
            when (format) {
                ArchiveFormat.ZIP -> compressZip(source, onProgress)
                ArchiveFormat.TAR -> compressTar(source, onProgress)
                ArchiveFormat.TAR_GZ -> compressTarGz(source, onProgress)
                ArchiveFormat.SEVEN_Z -> compress7z(source, onProgress)
            }
        }

    private fun compressZip(source: File, onProgress: (Float) -> Unit): File? {
        return try {
            val zipFile = File(source.parent, "${source.nameWithoutExtension}.zip")
            val allFiles = if (source.isDirectory) source.walkTopDown().filter { it.isFile }.toList() else listOf(source)
            val total = allFiles.sumOf { it.length() }.toFloat().coerceAtLeast(1f)
            var written = 0L

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                allFiles.forEach { file ->
                    val entryName = if (source.isDirectory) file.relativeTo(source).path else file.name
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        val buf = ByteArray(8192); var n: Int
                        while (fis.read(buf).also { n = it } != -1) {
                            zos.write(buf, 0, n); written += n; onProgress(written / total)
                        }
                    }
                    zos.closeEntry()
                }
            }
            zipFile
        } catch (_: Exception) { null }
    }

    private fun compressTarGz(source: File, onProgress: (Float) -> Unit): File? {
        return try {
            val tarGzFile = File(source.parent, "${source.nameWithoutExtension}.tar.gz")
            val allFiles = if (source.isDirectory) source.walkTopDown().filter { it.isFile }.toList() else listOf(source)
            val total = allFiles.sumOf { it.length() }.toFloat().coerceAtLeast(1f)
            var written = 0L

            val fos = FileOutputStream(tarGzFile)
            val gzos = GZIPOutputStream(BufferedOutputStream(fos))
            val tos = org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(gzos)
            tos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU)

            allFiles.forEach { file ->
                val entryName = if (source.isDirectory) file.relativeTo(source).path else file.name
                val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry(file, entryName)
                tos.putArchiveEntry(entry)
                FileInputStream(file).use { fis ->
                    val buf = ByteArray(8192); var n: Int
                    while (fis.read(buf).also { n = it } != -1) {
                        tos.write(buf, 0, n); written += n; onProgress(written / total)
                    }
                }
                tos.closeArchiveEntry()
            }
            tos.close()
            tarGzFile
        } catch (_: Exception) { null }
    }

    private fun compressTar(source: File, onProgress: (Float) -> Unit): File? {
        return try {
            val tarFile = File(source.parent, "${source.nameWithoutExtension}.tar")
            val allFiles = if (source.isDirectory) source.walkTopDown().filter { it.isFile }.toList() else listOf(source)
            val total = allFiles.sumOf { it.length() }.toFloat().coerceAtLeast(1f)
            var written = 0L

            val tos = org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(
                BufferedOutputStream(FileOutputStream(tarFile)),
            )
            tos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU)

            allFiles.forEach { file ->
                val entryName = if (source.isDirectory) file.relativeTo(source).path else file.name
                val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry(file, entryName)
                tos.putArchiveEntry(entry)
                FileInputStream(file).use { fis ->
                    val buf = ByteArray(8192); var n: Int
                    while (fis.read(buf).also { n = it } != -1) {
                        tos.write(buf, 0, n); written += n; onProgress(written / total)
                    }
                }
                tos.closeArchiveEntry()
            }
            tos.close()
            tarFile
        } catch (_: Exception) { null }
    }

    private fun compress7z(source: File, onProgress: (Float) -> Unit): File? {
        return try {
            val sevenZFile = File(source.parent, "${source.nameWithoutExtension}.7z")
            val allFiles = if (source.isDirectory) source.walkTopDown().filter { it.isFile }.toList() else listOf(source)
            val total = allFiles.sumOf { it.length() }.toFloat().coerceAtLeast(1f)
            var written = 0L

            org.apache.commons.compress.archivers.sevenz.SevenZOutputFile(sevenZFile).use { szof ->
                allFiles.forEach { file ->
                    val entryName = if (source.isDirectory) file.relativeTo(source).path else file.name
                    val entry = szof.createArchiveEntry(file, entryName)
                    szof.putArchiveEntry(entry)
                    FileInputStream(file).use { fis ->
                        val buf = ByteArray(8192); var n: Int
                        while (fis.read(buf).also { n = it } != -1) {
                            szof.write(buf, 0, n); written += n; onProgress(written / total)
                        }
                    }
                    szof.closeArchiveEntry()
                }
            }
            sevenZFile
        } catch (_: Exception) { null }
    }

    // ═══════════════════════════════════
    // Decompress
    // ═══════════════════════════════════

    /** Decompress any supported archive */
    suspend fun decompress(archiveFile: File, onProgress: (Float) -> Unit = {}): File? = withContext(Dispatchers.IO) {
        when (detectFormat(archiveFile)) {
            ArchiveFormat.ZIP -> decompressZip(archiveFile, onProgress)
            ArchiveFormat.TAR -> decompressTar(archiveFile, onProgress)
            ArchiveFormat.TAR_GZ -> decompressTarGz(archiveFile, onProgress)
            ArchiveFormat.SEVEN_Z -> decompress7z(archiveFile, onProgress)
            null -> null
        }
    }

    private fun decompressZip(zipFile: File, onProgress: (Float) -> Unit): File? {
        return try {
            val outDir = File(zipFile.parent, zipFile.nameWithoutExtension)
            outDir.mkdirs()
            val total = zipFile.length().toFloat().coerceAtLeast(1f)
            var read = 0L

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val e = entry ?: continue
                    val outFile = safeOutputFile(outDir, e.name) ?: continue
                    if (e.isDirectory) { outFile.mkdirs(); continue }
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buf = ByteArray(8192); var n: Int
                        while (zis.read(buf).also { n = it } != -1) {
                            fos.write(buf, 0, n); read += n; onProgress(read / total)
                        }
                    }
                    zis.closeEntry()
                }
            }
            outDir
        } catch (_: Exception) { null }
    }

    private fun decompressTar(tarFile: File, onProgress: (Float) -> Unit): File? {
        return try {
            val outDir = File(tarFile.parent, tarFile.nameWithoutExtension)
            outDir.mkdirs()
            val total = tarFile.length().toFloat().coerceAtLeast(1f)
            var read = 0L

            val tis = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(
                BufferedInputStream(FileInputStream(tarFile)),
            )

            var entry = tis.nextTarEntry
            while (entry != null) {
                val outFile = safeOutputFile(outDir, entry.name)
                if (outFile != null) {
                    if (entry.isDirectory) { outFile.mkdirs() }
                    else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            val buf = ByteArray(8192); var n: Int
                            while (tis.read(buf).also { n = it } != -1) {
                                fos.write(buf, 0, n); read += n; onProgress(read / total)
                            }
                        }
                    }
                }
                entry = tis.nextTarEntry
            }
            tis.close()
            outDir
        } catch (_: Exception) { null }
    }

    private fun decompressTarGz(tarGzFile: File, onProgress: (Float) -> Unit): File? {
        return try {
            val baseName = tarGzFile.name.removeSuffix(".tar.gz").removeSuffix(".tgz")
            val outDir = File(tarGzFile.parent, baseName)
            outDir.mkdirs()
            val total = tarGzFile.length().toFloat().coerceAtLeast(1f)
            var read = 0L

            val fis = FileInputStream(tarGzFile)
            val gzis = GZIPInputStream(BufferedInputStream(fis))
            val tis = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzis)

            var entry = tis.nextTarEntry
            while (entry != null) {
                val outFile = safeOutputFile(outDir, entry.name)
                if (outFile != null) {
                    if (entry.isDirectory) { outFile.mkdirs() }
                    else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            val buf = ByteArray(8192); var n: Int
                            while (tis.read(buf).also { n = it } != -1) {
                                fos.write(buf, 0, n); read += n; onProgress(read / total)
                            }
                        }
                    }
                }
                entry = tis.nextTarEntry
            }
            tis.close()
            outDir
        } catch (_: Exception) { null }
    }

    private fun decompress7z(sevenZFile: File, onProgress: (Float) -> Unit): File? {
        return try {
            val outDir = File(sevenZFile.parent, sevenZFile.nameWithoutExtension)
            outDir.mkdirs()

            org.apache.commons.compress.archivers.sevenz.SevenZFile(sevenZFile).use { szf ->
                var entry = szf.nextEntry
                val total = szf.entries.sumOf { it.size }.toFloat().coerceAtLeast(1f)
                var read = 0L
                // Re-open because we consumed entries for total
                val szf2 = org.apache.commons.compress.archivers.sevenz.SevenZFile(sevenZFile)
                var entry2 = szf2.nextEntry
                while (entry2 != null) {
                    val outFile = safeOutputFile(outDir, entry2.name)
                    if (outFile != null) {
                        if (entry2.isDirectory) { outFile.mkdirs() }
                        else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                val buf = ByteArray(8192); var n: Int
                                while (szf2.read(buf).also { n = it } != -1) {
                                    fos.write(buf, 0, n); read += n; onProgress(read / total)
                                }
                            }
                        }
                    }
                    entry2 = szf2.nextEntry
                }
                szf2.close()
            }
            outDir
        } catch (_: Exception) { null }
    }

    // ═══════════════════════════════════
    // List contents
    // ═══════════════════════════════════

    /** List contents of any supported archive without extracting */
    fun listContents(archiveFile: File): List<String> = when (detectFormat(archiveFile)) {
        ArchiveFormat.ZIP -> listZipContents(archiveFile)
        ArchiveFormat.TAR -> listTarContents(archiveFile)
        ArchiveFormat.TAR_GZ -> listTarGzContents(archiveFile)
        ArchiveFormat.SEVEN_Z -> list7zContents(archiveFile)
        null -> emptyList()
    }

    fun listZipContents(zipFile: File): List<String> {
        val entries = mutableListOf<String>()
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    entries.add(entry!!.name)
                    zis.closeEntry()
                }
            }
        } catch (_: Exception) {}
        return entries
    }

    private fun listTarContents(tarFile: File): List<String> {
        val entries = mutableListOf<String>()
        try {
            val tis = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(
                BufferedInputStream(FileInputStream(tarFile)),
            )
            var entry = tis.nextTarEntry
            while (entry != null) {
                entries.add(entry.name)
                entry = tis.nextTarEntry
            }
            tis.close()
        } catch (_: Exception) {}
        return entries
    }

    private fun listTarGzContents(tarGzFile: File): List<String> {
        val entries = mutableListOf<String>()
        try {
            val tis = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(
                GZIPInputStream(BufferedInputStream(FileInputStream(tarGzFile)))
            )
            var entry = tis.nextTarEntry
            while (entry != null) {
                entries.add(entry.name)
                entry = tis.nextTarEntry
            }
            tis.close()
        } catch (_: Exception) {}
        return entries
    }

    private fun list7zContents(sevenZFile: File): List<String> {
        val entries = mutableListOf<String>()
        try {
            org.apache.commons.compress.archivers.sevenz.SevenZFile(sevenZFile).use { szf ->
                var entry = szf.nextEntry
                while (entry != null) {
                    entries.add(entry.name)
                    entry = szf.nextEntry
                }
            }
        } catch (_: Exception) {}
        return entries
    }

    private fun safeOutputFile(outDir: File, entryName: String): File? {
        val outFile = File(outDir, entryName)
        val outRoot = outDir.canonicalPath + File.separator
        val outPath = outFile.canonicalPath
        return if (outPath.startsWith(outRoot)) outFile else null
    }
}
