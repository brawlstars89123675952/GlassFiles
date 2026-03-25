package com.glassfiles.data.terminal

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class LinuxDistro(val label: String) {
    UBUNTU("Ubuntu 22.04"),
    ALPINE("Alpine Linux")
}

class TermuxBootstrap(private val context: Context) {

    companion object {
        private const val TAG = "LINUX"
        private const val FAKE_KERNEL = "5.4.0-faked"
        private const val PREFS = "bootstrap_prefs"
        private const val KEY_DISTRO = "distro"

        private fun getProotUrl(): String {
            val arch = if (Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) "aarch64" else "armv7"
            return "https://skirsten.github.io/proot-portable-android-binaries/$arch/proot"
        }

        private fun getUbuntuRootfsUrl(): String {
            val arch = if (Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) "arm64" else "armhf"
            return "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04-base-$arch.tar.gz"
        }

        private fun getAlpineRootfsUrl(): String {
            val arch = if (Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) "aarch64" else "armhf"
            return "https://dl-cdn.alpinelinux.org/alpine/v3.20/releases/$arch/alpine-minirootfs-3.20.6-$arch.tar.gz"
        }

        private fun getOpencodeUrl(): String {
            val arch = if (Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) "arm64" else "x64"
            return "https://github.com/anomalyco/opencode/releases/latest/download/opencode-linux-$arch.tar.gz"
        }
    }

    val rootfsDir: File get() = File(context.filesDir, "ubuntu")
    val homeDir: File get() = File(context.filesDir, "ubuntu-home")
    val tmpDir: File get() = File(context.filesDir, "ubuntu-tmp")
    val prootBin: File get() = File(context.filesDir, "proot")

    private val _status = MutableStateFlow<BootstrapStatus>(BootstrapStatus.Checking)
    val status = _status.asStateFlow()

    var lastDebug: String = ""
        private set

    fun updateStatus(s: BootstrapStatus) { _status.value = s }

    fun getDistro(): LinuxDistro {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DISTRO, "UBUNTU") ?: "UBUNTU"
        return try { LinuxDistro.valueOf(saved) } catch (_: Exception) { LinuxDistro.UBUNTU }
    }

    private fun saveDistro(distro: LinuxDistro) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DISTRO, distro.name).apply()
    }

    fun isInstalled(): Boolean {
        if (!rootfsDir.exists()) return false
        val hasShell = File(rootfsDir, "bin/sh").exists() ||
            File(rootfsDir, "bin/bash").exists() ||
            File(rootfsDir, "bin/busybox").exists()
        return hasShell
    }

    fun hasProot(): Boolean {
        return prootBin.exists() && prootBin.canExecute() && prootBin.length() > 50_000
    }

    fun getEnvironment(): Map<String, String> {
        return mapOf(
            "HOME" to "/storage/emulated/0",
            "TERM" to "xterm-256color",
            "LANG" to "en_US.UTF-8",
            "PATH" to "/system/bin:/system/xbin",
            "PWD" to "/storage/emulated/0",
            "PROOT_TMP_DIR" to tmpDir.absolutePath
        )
    }

    fun buildProotCommand(userCommand: String): String {
        val rootfs = rootfsDir.absolutePath
        val home = homeDir.absolutePath
        val tmp = tmpDir.absolutePath
        val proot = prootBin.absolutePath
        val escaped = userCommand.replace("'", "'\\''")
        val shell = if (File(rootfsDir, "bin/bash").exists()) "/bin/bash" else "/bin/sh"

        return buildString {
            append("'$proot'")
            append(" --link2symlink")
            append(" --rootfs='$rootfs'")
            append(" -0")
            append(" -w /root")
            append(" -b /dev")
            append(" -b /proc")
            append(" -b /sys")
            append(" -b '$home:/root'")
            append(" -b '$tmp:/tmp'")
            append(" -b /storage/emulated/0:/sdcard")
            append(" -b /storage")
            append(" /usr/bin/env -i")
            append(" HOME=/root USER=root LOGNAME=root")
            append(" PATH=/root/.bun/bin:/root/.opencode/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
            append(" TERM=xterm-256color LANG=C.UTF-8 TMPDIR=/tmp")
            append(" $shell -c '$escaped'")
        }
    }

    suspend fun install(distro: LinuxDistro = LinuxDistro.UBUNTU) {
        withContext(Dispatchers.IO) {
            val debug = StringBuilder()

            try {
                _status.value = BootstrapStatus.Downloading(0f)
                saveDistro(distro)

                rootfsDir.deleteRecursively()
                homeDir.deleteRecursively()
                tmpDir.deleteRecursively()
                rootfsDir.mkdirs(); homeDir.mkdirs(); tmpDir.mkdirs()

                // === Step 1: Download proot ===
                if (!hasProot()) {
                    debug.appendLine("⬇️ Downloading proot...")
                    downloadFileDirect(getProotUrl(), prootBin)
                    prootBin.setExecutable(true, false)
                    prootBin.setReadable(true, false)
                    debug.appendLine("proot: ${prootBin.length()} bytes")
                }

                // === Step 2: Download rootfs ===
                _status.value = BootstrapStatus.Downloading(0.05f)
                val rootfsUrl = when (distro) {
                    LinuxDistro.UBUNTU -> { debug.appendLine("⬇️ Downloading Ubuntu rootfs..."); getUbuntuRootfsUrl() }
                    LinuxDistro.ALPINE -> { debug.appendLine("⬇️ Downloading Alpine rootfs..."); getAlpineRootfsUrl() }
                }
                val tarFile = File(context.cacheDir, "rootfs.tar.gz")
                downloadFile(rootfsUrl, tarFile, 0.05f, 0.45f)
                debug.appendLine("rootfs: ${tarFile.length() / 1024} KB (url: $rootfsUrl)")

                if (tarFile.length() < 10_000) {
                    // Probably an error page, not a real tar.gz
                    val content = tarFile.readText().take(200)
                    debug.appendLine("⚠️ File too small, content: $content")
                    throw IllegalStateException("Download failed: file too small (${tarFile.length()} bytes)")
                }

                // === Step 3: Extract rootfs ===
                _status.value = BootstrapStatus.Extracting
                debug.appendLine("📦 Extracting rootfs...")
                rootfsDir.deleteRecursively(); rootfsDir.mkdirs(); tmpDir.mkdirs()

                // Try proot extraction first
                val extractCmd = buildString {
                    append("'${prootBin.absolutePath}' --link2symlink")
                    append(" /system/bin/sh -c")
                    append(" 'tar xzf ${tarFile.absolutePath} -C ${rootfsDir.absolutePath}'")
                }

                val extract = ProcessBuilder("/system/bin/sh", "-c", extractCmd)
                    .apply { environment()["PROOT_TMP_DIR"] = tmpDir.absolutePath }
                    .redirectErrorStream(true).start()
                val extractLog = extract.inputStream.bufferedReader().readText()
                val extractExit = extract.waitFor()

                debug.appendLine("proot extract exit=$extractExit")
                if (extractLog.isNotBlank()) debug.appendLine("log: ${extractLog.take(200)}")

                // Fallback: direct tar if proot extraction failed
                val shellPath = when (distro) {
                    LinuxDistro.UBUNTU -> "bin/bash"
                    LinuxDistro.ALPINE -> "bin/busybox"
                }
                val shellAlt = "bin/sh"
                if (!File(rootfsDir, shellPath).exists() && !File(rootfsDir, shellAlt).exists()) {
                    debug.appendLine("⚠️ Proot extract failed, trying direct tar...")
                    rootfsDir.deleteRecursively(); rootfsDir.mkdirs()
                    val fallback = ProcessBuilder("/system/bin/sh", "-c",
                        "tar xzf '${tarFile.absolutePath}' -C '${rootfsDir.absolutePath}'"
                    ).redirectErrorStream(true).start()
                    val fbLog = fallback.inputStream.bufferedReader().readText()
                    val fbExit = fallback.waitFor()
                    debug.appendLine("direct tar exit=$fbExit")
                    if (fbLog.isNotBlank()) debug.appendLine("log: ${fbLog.take(200)}")
                }

                tarFile.delete()

                debug.appendLine("extract exit=$extractExit")
                if (extractLog.contains("Error") || extractLog.contains("error")) {
                    debug.appendLine(extractLog.take(300))
                }

                // Verify
                val hasShell = File(rootfsDir, "bin/sh").exists() ||
                    File(rootfsDir, "bin/bash").exists() ||
                    File(rootfsDir, "bin/busybox").exists()
                if (!hasShell) {
                    val files = rootfsDir.listFiles()?.map { it.name } ?: emptyList()
                    debug.appendLine("rootfs contents: $files")
                    val binFiles = File(rootfsDir, "bin").listFiles()?.map { it.name } ?: emptyList()
                    debug.appendLine("bin/ contents: $binFiles")
                    throw IllegalStateException("Rootfs extraction failed: no shell found in /bin/")
                }
                debug.appendLine("extract OK ✅")

                // === Step 4: Create fake /proc files ===
                debug.appendLine("⚙️ Creating fake proc files...")
                createFakeProcFiles()
                File(rootfsDir, "dev").mkdirs()

                // === Step 5: Configure ===
                _status.value = BootstrapStatus.Configuring
                when (distro) {
                    LinuxDistro.UBUNTU -> {
                        debug.appendLine("⚙️ Configuring Ubuntu...")
                        configureUbuntu()
                    }
                    LinuxDistro.ALPINE -> {
                        debug.appendLine("⚙️ Configuring Alpine...")
                        configureAlpine()
                    }
                }

                // === Step 6: Download opencode ===
                debug.appendLine("⬇️ Downloading opencode...")
                val ocDir = File(homeDir, ".opencode/bin")
                ocDir.mkdirs()
                try {
                    val ocTarFile = File(context.cacheDir, "opencode.tar.gz")
                    downloadFileDirect(getOpencodeUrl(), ocTarFile)
                    debug.appendLine("opencode archive: ${ocTarFile.length() / 1024} KB")
                    ProcessBuilder("/system/bin/sh", "-c",
                        "cd '${ocDir.absolutePath}' && tar xzf '${ocTarFile.absolutePath}' && chmod +x opencode"
                    ).redirectErrorStream(true).start().also { it.inputStream.bufferedReader().readText(); it.waitFor() }
                    ocTarFile.delete()
                    debug.appendLine("opencode: ${if (File(ocDir, "opencode").exists()) "OK" else "FAIL"}")
                } catch (e: Exception) {
                    debug.appendLine("opencode err: ${e.message}")
                }

                // === Step 7: Install packages ===
                when (distro) {
                    LinuxDistro.UBUNTU -> {
                        debug.appendLine("📦 Installing packages + Node.js...")
                        writeUbuntuSetupScript()
                        val setupResult = runProotScript("/root/setup.sh")
                        debug.appendLine(setupResult.stdout.trim().takeLast(600))
                    }
                    LinuxDistro.ALPINE -> {
                        debug.appendLine("📦 Installing packages + Node.js + npm...")
                        writeAlpineSetupScript()
                        val setupResult = runProotScript("/root/setup.sh", "/bin/sh")
                        debug.appendLine(setupResult.stdout.trim().takeLast(600))
                    }
                }

                lastDebug = debug.toString()

                if (isInstalled() && hasProot()) {
                    _status.value = BootstrapStatus.Ready
                } else {
                    _status.value = BootstrapStatus.Error("Not completed\n\n$debug")
                }
            } catch (e: Exception) {
                debug.appendLine("❌ ${e.javaClass.simpleName}: ${e.message}")
                lastDebug = debug.toString()
                _status.value = BootstrapStatus.Error("${e.javaClass.simpleName}: ${e.message}\n\n$debug")
            }
        }
    }

    fun uninstall() {
        rootfsDir.deleteRecursively()
        homeDir.deleteRecursively()
        tmpDir.deleteRecursively()
        lastDebug = ""
        _status.value = BootstrapStatus.NotInstalled
    }

    private fun createFakeProcFiles() {
        val procDir = File(rootfsDir, "proc")
        procDir.mkdirs()
        try {
            File(procDir, ".loadavg").writeText("0.12 0.07 0.02 2/165 765\n")
            File(procDir, ".stat").writeText(buildString {
                appendLine("cpu  1050008 127632 898432 43828767 37203 63 99244 0 0 0")
                appendLine("cpu0 212383 20476 204529 8389202 7253 42 12597 0 0 0")
                appendLine("intr 53261351 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0")
                appendLine("ctxt 38014093"); appendLine("btime 1609459200")
                appendLine("processes 26442"); appendLine("procs_running 1"); appendLine("procs_blocked 0")
            })
            File(procDir, ".uptime").writeText("124689.56 495674.32\n")
            File(procDir, ".version").writeText("Linux version $FAKE_KERNEL (termux@androidos) (gcc version 4.9.x) #1 SMP PREEMPT Fri Jul 10 00:00:00 UTC 2020\n")
            File(procDir, ".vmstat").writeText(buildString {
                appendLine("nr_free_pages 146031"); appendLine("nr_zone_inactive_anon 196744")
                appendLine("nr_zone_active_anon 301503"); appendLine("nr_zone_inactive_file 2457066")
                appendLine("pgpgin 763641"); appendLine("pgpgout 674732"); appendLine("pswpin 0"); appendLine("pswpout 0")
            })
        } catch (e: Exception) {
            // Permission errors OK in proot — proc files are optional
        }
    }

    // ═══════════════════════════════════
    // Alpine configuration
    // ═══════════════════════════════════

    private fun configureAlpine() {
        // DNS
        File(rootfsDir, "etc/resolv.conf").writeText("nameserver 8.8.8.8\nnameserver 8.8.4.4\n")
        File(rootfsDir, "etc/hostname").writeText("glassfiles\n")
        File(rootfsDir, "etc/hosts").writeText("127.0.0.1 localhost glassfiles\n")

        // APK repos
        File(rootfsDir, "etc/apk/repositories").writeText("""
http://dl-cdn.alpinelinux.org/alpine/v3.20/main
http://dl-cdn.alpinelinux.org/alpine/v3.20/community
""".trimIndent() + "\n")

        // Home dirs
        File(rootfsDir, "root").mkdirs()
        File(rootfsDir, "tmp").mkdirs()
        homeDir.mkdirs()

        // Android GIDs
        val groupFile = File(rootfsDir, "etc/group")
        if (!groupFile.exists()) groupFile.writeText("root:x:0:\n")
        if (!groupFile.readText().contains("aid_inet")) {
            groupFile.appendText("aid_inet:x:3003:\naid_net_raw:x:3004:\n")
        }

        // .profile
        File(homeDir, ".profile").writeText("""
export PATH="/root/.opencode/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
export HOME=/root
export LANG=C.UTF-8
export TERM=xterm-256color
export TMPDIR=/tmp
alias ll='ls -la --color=auto'
alias la='ls -A --color=auto'
alias ls='ls --color=auto'
alias op='opencode'
PS1='\033[01;32malpine\033[00m:\033[01;34m\w\033[00m# '
""")
    }

    private fun writeAlpineSetupScript() {
        File(homeDir, "setup.sh").writeText("#!/bin/sh\n" +
            "echo '=== apk update ==='\n" +
            "apk update 2>&1 | tail -3\n" +
            "\n" +
            "echo '=== apk install base ==='\n" +
            "apk add --no-cache --allow-untrusted bash curl wget git python3 nano ca-certificates openssh-client 2>&1 | tail -5\n" +
            "\n" +
            "echo '=== apk install nodejs + npm ==='\n" +
            "apk add --no-cache --allow-untrusted nodejs npm 2>&1 | tail -5\n" +
            "\n" +
            "echo '=== Fix npm/npx symlinks for proot ==='\n" +
            "rm -f /usr/bin/npm /usr/bin/npx 2>/dev/null\n" +
            "printf '#!/bin/sh\\nexec /usr/bin/node /usr/lib/node_modules/npm/lib/cli.js \"\\$@\"\\n' > /usr/bin/npm\n" +
            "printf '#!/bin/sh\\nexec /usr/bin/node /usr/lib/node_modules/npm/lib/cli.js \"\\$@\" -- npx\\n' > /usr/bin/npx\n" +
            "chmod +x /usr/bin/npm /usr/bin/npx\n" +
            "\n" +
            "echo '=== Verify ==='\n" +
            "echo \"node: \$(node --version 2>/dev/null || echo FAIL)\"\n" +
            "echo \"npm: \$(npm --version 2>/dev/null || echo FAIL)\"\n" +
            "echo \"python3: \$(python3 --version 2>/dev/null || echo FAIL)\"\n" +
            "echo \"git: \$(git --version 2>/dev/null || echo FAIL)\"\n" +
            "echo '=== SETUP COMPLETE ==='\n"
        )
        File(homeDir, "setup.sh").setExecutable(true, false)
    }

    // ═══════════════════════════════════
    // Ubuntu configuration (unchanged)
    // ═══════════════════════════════════

    private fun configureUbuntu() {
        File(rootfsDir, "etc/resolv.conf").writeText("nameserver 8.8.8.8\nnameserver 8.8.4.4\n")
        File(rootfsDir, "etc/hostname").writeText("glassfiles\n")
        File(rootfsDir, "etc/hosts").writeText("127.0.0.1 localhost glassfiles\n")
        File(rootfsDir, "etc/apt/sources.list").writeText("""
deb http://ports.ubuntu.com/ubuntu-ports/ jammy main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports/ jammy-updates main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports/ jammy-security main restricted universe multiverse
""".trimIndent() + "\n")
        File(rootfsDir, "root").mkdirs(); File(rootfsDir, "tmp").mkdirs(); homeDir.mkdirs()
        File(rootfsDir, "etc/profile.d").mkdirs()
        File(rootfsDir, "etc/profile.d/glassfiles.sh").writeText("export PULSE_SERVER=127.0.0.1\n")
        File(homeDir, ".bashrc").writeText("""export PATH="/root/.bun/bin:/root/.opencode/bin:${'$'}PATH"
export HOME=/root
export LANG=C.UTF-8
export TERM=xterm-256color
export TMPDIR=/tmp
export BUN_INSTALL=/root/.bun
alias ll='ls -la --color=auto'
alias la='ls -A --color=auto'
alias ls='ls --color=auto'
alias op='opencode'
alias npm='bun'
alias npx='bunx'
PS1='\[\033[01;32m\]ubuntu\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]$ '
""")
        File(homeDir, ".profile").writeText("[ -f ~/.bashrc ] && . ~/.bashrc\n")
        // Fix locale — use C.UTF-8 which is always available
        File(homeDir, ".bash_profile").writeText("""[ -f ~/.bashrc ] && . ~/.bashrc
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
""")
        // Readline config — ↑↓ search history by prefix instead of cycling all
        File(homeDir, ".inputrc").writeText("""
"\e[A": history-search-backward
"\e[B": history-search-forward
"\e[C": forward-char
"\e[D": backward-char
"\e[1;5C": forward-word
"\e[1;5D": backward-word
set completion-ignore-case on
set show-all-if-ambiguous on
set colored-stats on
""")
        val groupFile = File(rootfsDir, "etc/group")
        if (!groupFile.exists()) groupFile.writeText("root:x:0:\n")
        val groupText = groupFile.readText()
        // Fix Android GID warnings — add all common Android groups
        val androidGroups = listOf(
            "aid_inet:x:3003:", "aid_net_raw:x:3004:",
            "aid_everybody:x:9997:", "aid_misc:x:9998:",
            "aid_cache:x:1077:", "aid_sdcard_rw:x:1015:",
            "aid_media_rw:x:1023:", "aid_ext_data_rw:x:1078:",
            "aid_ext_obb_rw:x:1079:",
            "u0_a599:x:10599:", "u0_a20599:x:20599:", "u0_a50599:x:50599:",
            "u0_12801023:x:12801023:", "u0_12809997:x:12809997:"
        )
        val missing = androidGroups.filter { !groupText.contains(it.split(":")[0]) }
        if (missing.isNotEmpty()) groupFile.appendText(missing.joinToString("\n") + "\n")
        listOf("var/lib/dpkg", "var/lib/dpkg/info", "var/lib/dpkg/updates", "var/lib/dpkg/triggers",
            "var/lib/apt", "var/lib/apt/lists", "var/lib/apt/lists/partial",
            "var/cache/apt", "var/cache/apt/archives", "var/cache/apt/archives/partial",
            "var/log", "var/log/apt"
        ).forEach { File(rootfsDir, it).mkdirs() }
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "777", File(rootfsDir, "var").absolutePath)).waitFor()
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "777", File(rootfsDir, "tmp").absolutePath)).waitFor()
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", File(rootfsDir, "etc").absolutePath)).waitFor()
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", File(rootfsDir, "usr").absolutePath)).waitFor()
        } catch (_: Exception) {}
    }

    private fun writeUbuntuSetupScript() {
        File(homeDir, "setup.sh").writeText("#!/bin/bash\n" +
            "set +e\n" +
            "export DEBIAN_FRONTEND=noninteractive\n" +
            "\n" +
            "# === Fix dbus to prevent dpkg errors in proot ===\n" +
            "echo '#!/bin/sh' > /var/lib/dpkg/info/dbus.postinst 2>/dev/null\n" +
            "echo 'exit 0' >> /var/lib/dpkg/info/dbus.postinst 2>/dev/null\n" +
            "chmod +x /var/lib/dpkg/info/dbus.postinst 2>/dev/null\n" +
            "dpkg --configure -a --force-confold 2>/dev/null || true\n" +
            "\n" +
            "echo '=== apt update ==='\n" +
            "timeout 120 apt update -y 2>&1 | tail -3\n" +
            "\n" +
            "echo '=== apt install base ==='\n" +
            "timeout 180 apt install -y --no-install-recommends curl wget git python3 nano ca-certificates unzip 2>&1 | tail -5\n" +
            "\n" +
            "echo '=== Install Bun ==='\n" +
            "BUN_INSTALL=/root/.bun\n" +
            "mkdir -p \$BUN_INSTALL/bin\n" +
            "ARCH=\$(uname -m)\n" +
            "if [ \"\$ARCH\" = \"aarch64\" ]; then BUN_ARCH=\"aarch64\"; else BUN_ARCH=\"x64\"; fi\n" +
            "echo \"Downloading bun for \$BUN_ARCH...\"\n" +
            "timeout 90 curl -fsSL --connect-timeout 15 https://github.com/oven-sh/bun/releases/latest/download/bun-linux-\$BUN_ARCH.zip -o /tmp/bun.zip\n" +
            "if [ -f /tmp/bun.zip ]; then\n" +
            "  cd /tmp && unzip -o bun.zip 2>/dev/null\n" +
            "  cp /tmp/bun-linux-\$BUN_ARCH/bun \$BUN_INSTALL/bin/bun 2>/dev/null\n" +
            "  chmod +x \$BUN_INSTALL/bin/bun 2>/dev/null\n" +
            "  rm -rf /tmp/bun.zip /tmp/bun-linux-*\n" +
            "else\n" +
            "  echo 'Bun: не удалось скачать (пропускаем)'\n" +
            "fi\n" +
            "export PATH=\"\$BUN_INSTALL/bin:\$PATH\"\n" +
            "\n" +
            "echo '=== Verify ==='\n" +
            "echo \"bun: \$(bun --version 2>/dev/null || echo не установлен)\"\n" +
            "echo \"python3: \$(python3 --version 2>/dev/null || echo не установлен)\"\n" +
            "echo \"git: \$(git --version 2>/dev/null || echo не установлен)\"\n" +
            "echo '=== SETUP COMPLETE ==='\n"
        )
        File(homeDir, "setup.sh").setExecutable(true, false)
    }

    // ═══════════════════════════════════
    // Proot execution
    // ═══════════════════════════════════

    private fun runProotScript(scriptPath: String, shell: String = "/bin/bash"): CmdResult {
        return try {
            val rootfs = rootfsDir.absolutePath
            val home = homeDir.absolutePath
            val tmp = tmpDir.absolutePath
            val proot = prootBin.absolutePath

            val cmd = buildString {
                append("'$proot'")
                append(" --link2symlink")
                append(" --rootfs='$rootfs'")
                append(" -0")
                append(" -w /root")
                append(" -b /dev")
                append(" -b /proc")
                append(" -b /sys")
                append(" -b '$home:/root'")
                append(" -b '$tmp:/tmp'")
                append(" -b /storage/emulated/0:/sdcard")
                append(" -b /storage")
                append(" /usr/bin/env -i")
                append(" HOME=/root USER=root LOGNAME=root")
                append(" PATH=/root/.bun/bin:/root/.opencode/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
                append(" TERM=xterm-256color LANG=C.UTF-8 TMPDIR=/tmp")
                append(" $shell $scriptPath")
            }

            val pb = ProcessBuilder("/system/bin/sh", "-c", cmd)
            pb.redirectErrorStream(true) // merge stderr into stdout to avoid deadlock
            pb.environment().clear()
            pb.environment().putAll(getEnvironment())
            val process = pb.start()

            // Read output in background thread to prevent buffer deadlock
            val outputBuilder = StringBuilder()
            val readerThread = Thread {
                try {
                    process.inputStream.bufferedReader().forEachLine { line ->
                        outputBuilder.appendLine(line)
                    }
                } catch (_: Exception) {}
            }
            readerThread.start()

            // Wait with 5 minute timeout
            val finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES)
            if (!finished) {
                process.destroyForcibly()
                readerThread.join(2000)
                CmdResult(outputBuilder.toString(), "Timeout: setup aborted after 10 minutes", 1)
            } else {
                readerThread.join(3000)
                CmdResult(outputBuilder.toString(), "", process.exitValue())
            }
        } catch (e: Exception) {
            CmdResult("", e.message ?: "error", 1)
        }
    }

    private fun downloadFileDirect(urlStr: String, output: File) {
        var conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true; conn.connectTimeout = 30000; conn.readTimeout = 120000
        var redir = 0
        while (redir < 5) {
            conn.connect()
            if (conn.responseCode in listOf(301, 302, 307, 308)) {
                val u = conn.getHeaderField("Location"); conn.disconnect()
                conn = URL(u).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true; conn.connectTimeout = 30000; conn.readTimeout = 120000; redir++
            } else break
        }
        conn.inputStream.use { i -> FileOutputStream(output).use { o -> i.copyTo(o) } }
        conn.disconnect()
    }

    private suspend fun downloadFile(urlStr: String, output: File, progressStart: Float = 0f, progressEnd: Float = 1f) {
        var conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true; conn.connectTimeout = 30000; conn.readTimeout = 120000
        var redir = 0
        while (redir < 5) {
            conn.connect()
            if (conn.responseCode in listOf(301, 302, 307, 308)) {
                val u = conn.getHeaderField("Location"); conn.disconnect()
                conn = URL(u).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true; conn.connectTimeout = 30000; conn.readTimeout = 120000; redir++
            } else break
        }
        val total = conn.contentLengthLong.coerceAtLeast(1L); var dl = 0L
        conn.inputStream.use { i -> FileOutputStream(output).use { o ->
            val buf = ByteArray(8192); var n: Int
            while (i.read(buf).also { n = it } != -1) {
                o.write(buf, 0, n); dl += n
                _status.value = BootstrapStatus.Downloading(progressStart + (progressEnd - progressStart) * (dl.toFloat() / total))
            }
        } }
        conn.disconnect()
    }

    private data class CmdResult(val stdout: String, val stderr: String, val exitCode: Int)
}

sealed class BootstrapStatus {
    data object Checking : BootstrapStatus()
    data object NotInstalled : BootstrapStatus()
    data class Downloading(val progress: Float) : BootstrapStatus()
    data object Extracting : BootstrapStatus()
    data object Configuring : BootstrapStatus()
    data object Ready : BootstrapStatus()
    data class Error(val message: String) : BootstrapStatus()
}
