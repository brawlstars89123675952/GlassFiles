package com.glassfiles.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CmdEntry(val cmd: String, val desc: String)
private fun s(ru: String, en: String): String = if (com.glassfiles.data.Strings.lang == com.glassfiles.data.AppLanguage.RUSSIAN) ru else en
data class CmdCategory(val name: String, val icon: String, val commands: List<CmdEntry>)

// ═══════════════════════════════════
// Команды, специфичные для дистрибутива
// ═══════════════════════════════════

private val ubuntuPackages = CmdCategory(s("Управление пакетами (apt)", "Package Management (apt)"), "📥", listOf(
    CmdEntry("apt update", s("Обновить список пакетов", "Update package list")),
    CmdEntry("apt upgrade -y", s("Обновить все пакеты", "Upgrade all packages")),
    CmdEntry("apt install package", s("Установить пакет", "Install package")),
    CmdEntry("apt install -y pkg1 pkg2", s("Установить несколько", "Install multiple")),
    CmdEntry("apt remove package", s("Удалить пакет", "Remove package")),
    CmdEntry("apt purge package", s("Полное удаление с конфигами", "Full removal with configs")),
    CmdEntry("apt autoremove", s("Удалить ненужные зависимости", "Remove unused dependencies")),
    CmdEntry("apt search keyword", s("Поиск пакета", "Search package")),
    CmdEntry("apt list --installed", s("Список установленных", "List installed")),
    CmdEntry("apt list --upgradable", s("Доступные обновления", "Available updates")),
    CmdEntry("apt show package", s("Информация о пакете", "Package info")),
    CmdEntry("apt-cache policy package", s("Доступные версии", "Available versions")),
    CmdEntry("dpkg -i package.deb", s("Установить .deb файл", "Install .deb file")),
    CmdEntry("dpkg -l", s("Список всех пакетов", "List all packages")),
    CmdEntry("dpkg -l | grep name", s("Найти установленный пакет", "Find installed package")),
    CmdEntry("dpkg -S /usr/bin/file", s("Какой пакет владеет файлом", "Which package owns file")),
    CmdEntry("pip install package", s("Python пакет", "Python package")),
    CmdEntry("pip install --upgrade pip", s("Обновить pip", "Update pip")),
    CmdEntry("pip list", s("Список Python пакетов", "List Python packages")),
    CmdEntry("pip freeze > req.txt", s("Сохранить зависимости", "Save dependencies")),
    CmdEntry("npm install package", s("Node.js пакет (локально)", "Node.js package (local)")),
    CmdEntry("npm install -g package", s("Node.js пакет (глобально)", "Node.js package (global)")),
    CmdEntry("npm list -g", s("Глобальные Node.js пакеты", "Global Node.js packages")),
))

private val alpinePackages = CmdCategory(s("Управление пакетами (apk)", "Package Management (apk)"), "📥", listOf(
    CmdEntry("apk update", s("Обновить индекс пакетов", "Update package index")),
    CmdEntry("apk upgrade", s("Обновить все пакеты", "Upgrade all packages")),
    CmdEntry("apk add package", s("Установить пакет", "Install package")),
    CmdEntry("apk add --no-cache pkg1 pkg2", s("Установить без кеша", "Install without cache")),
    CmdEntry("apk del package", s("Удалить пакет", "Remove package")),
    CmdEntry("apk search keyword", s("Поиск пакета", "Search package")),
    CmdEntry("apk list --installed", s("Список установленных", "List installed")),
    CmdEntry("apk info package", s("Информация о пакете", "Package info")),
    CmdEntry("apk info -L package", s("Файлы в пакете", "Files in package")),
    CmdEntry("apk -e info package", s("Установлен ли пакет", "Is package installed")),
    CmdEntry("apk stats", s("Статистика пакетов", "Package statistics")),
    CmdEntry("apk cache clean", s("Очистить кеш", "Clean cache")),
    CmdEntry("pip install package", s("Python пакет", "Python package")),
    CmdEntry("pip install --upgrade pip", s("Обновить pip", "Update pip")),
    CmdEntry("pip list", s("Список Python пакетов", "List Python packages")),
    CmdEntry("pip freeze > req.txt", s("Сохранить зависимости", "Save dependencies")),
    CmdEntry("npm install package", s("Node.js пакет (локально)", "Node.js package (local)")),
    CmdEntry("npm install -g package", s("Node.js пакет (глобально)", "Node.js package (global)")),
    CmdEntry("npx create-react-app app", s("Создать React проект", "Create React project")),
    CmdEntry("npm list -g", s("Глобальные Node.js пакеты", "Global Node.js packages")),
))

private val ubuntuSoftware = CmdCategory(s("Установка софта (apt)", "Software Installation (apt)"), "🔧", listOf(
    CmdEntry("apt install build-essential", s("GCC, Make и др. для компиляции", "GCC, Make etc. for compilation")),
    CmdEntry("apt install python3 python3-pip", "Python 3 + pip"),
    CmdEntry("apt install nodejs npm", "Node.js + npm"),
    CmdEntry("apt install golang-go", s("Язык Go", "Go language")),
    CmdEntry("apt install openjdk-17-jdk", "Java 17 (JDK)"),
    CmdEntry("apt install ruby", "Ruby"),
    CmdEntry("apt install rustc cargo", "Rust + Cargo"),
    CmdEntry("apt install git", s("Git (система контроля версий)", "Git (version control)")),
    CmdEntry("apt install curl wget", s("Скачивание файлов из сети", "Download files from network")),
    CmdEntry("apt install htop", s("Продвинутый монитор процессов", "Advanced process monitor")),
    CmdEntry("apt install tmux", s("Терминальный мультиплексор", "Terminal multiplexer")),
    CmdEntry("apt install screen", s("Сессии терминала в фоне", "Background terminal sessions")),
    CmdEntry("apt install ffmpeg", s("Мультимедиа (конвертация)", "Multimedia (conversion)")),
    CmdEntry("apt install imagemagick", s("Обработка изображений", "Image processing")),
    CmdEntry("apt install nginx", s("Веб-сервер Nginx", "Nginx web server")),
    CmdEntry("apt install sqlite3", s("SQLite база данных", "SQLite database")),
    CmdEntry("apt install postgresql", s("PostgreSQL БД", "PostgreSQL DB")),
    CmdEntry("apt install redis-server", s("Redis (кеш/БД)", "Redis (cache/DB)")),
    CmdEntry("apt install neofetch", s("Информация о системе (красиво)", "System info (pretty)")),
    CmdEntry("apt install jq", s("Парсер JSON в терминале", "JSON parser in terminal")),
    CmdEntry("apt install tree", s("Дерево каталогов", "Directory tree")),
    CmdEntry("apt install mc", s("Midnight Commander (файлы)", "Midnight Commander (files)")),
    CmdEntry("apt install nmap", s("Сканер сети", "Network scanner")),
    CmdEntry("apt install netcat-openbsd", s("Netcat (сетевая утилита)", "Netcat (network utility)")),
))

private val alpineSoftware = CmdCategory(s("Установка софта (apk)", "Software Installation (apk)"), "🔧", listOf(
    CmdEntry("apk add build-base", s("GCC, Make и др. для компиляции", "GCC, Make etc. for compilation")),
    CmdEntry("apk add python3 py3-pip", "Python 3 + pip"),
    CmdEntry("apk add nodejs npm", s("Node.js + npm (работает!)", "Node.js + npm (works!)")),
    CmdEntry("apk add go", s("Язык Go", "Go language")),
    CmdEntry("apk add openjdk17", "Java 17 (JDK)"),
    CmdEntry("apk add ruby", "Ruby"),
    CmdEntry("apk add rust cargo", "Rust + Cargo"),
    CmdEntry("apk add git", s("Git (система контроля версий)", "Git (version control)")),
    CmdEntry("apk add curl wget", s("Скачивание файлов из сети", "Download files from network")),
    CmdEntry("apk add htop", s("Продвинутый монитор процессов", "Advanced process monitor")),
    CmdEntry("apk add tmux", s("Терминальный мультиплексор", "Terminal multiplexer")),
    CmdEntry("apk add screen", s("Сессии терминала в фоне", "Background terminal sessions")),
    CmdEntry("apk add ffmpeg", s("Мультимедиа (конвертация)", "Multimedia (conversion)")),
    CmdEntry("apk add imagemagick", s("Обработка изображений", "Image processing")),
    CmdEntry("apk add nginx", s("Веб-сервер Nginx", "Nginx web server")),
    CmdEntry("apk add sqlite", s("SQLite база данных", "SQLite database")),
    CmdEntry("apk add postgresql16", s("PostgreSQL БД", "PostgreSQL DB")),
    CmdEntry("apk add redis", s("Redis (кеш/БД)", "Redis (cache/DB)")),
    CmdEntry("apk add neofetch", s("Информация о системе (красиво)", "System info (pretty)")),
    CmdEntry("apk add jq", s("Парсер JSON в терминале", "JSON parser in terminal")),
    CmdEntry("apk add tree", s("Дерево каталогов", "Directory tree")),
    CmdEntry("apk add mc", s("Midnight Commander (файлы)", "Midnight Commander (files)")),
    CmdEntry("apk add nmap", s("Сканер сети", "Network scanner")),
    CmdEntry("apk add nmap-ncat", s("Ncat (сетевая утилита)", "Ncat (network utility)")),
))

// ═══════════════════════════════════
// Common commands (same for both)
// ═══════════════════════════════════

private val commonCategories = listOf(
    CmdCategory(s("Навигация", "Navigation"), "📁", listOf(
        CmdEntry("pwd", s("Показать текущую директорию", "Show current directory")),
        CmdEntry("ls", s("Список файлов", "List files")),
        CmdEntry("ls -la", s("Подробный список (включая скрытые)", "Detailed list (including hidden)")),
        CmdEntry("ls -lh", s("Список с читаемыми размерами", "List with readable sizes")),
        CmdEntry("ls -lS", s("Сортировка по размеру", "Sort by size")),
        CmdEntry("ls -lt", s("Сортировка по дате", "Sort by date")),
        CmdEntry("ls -R", s("Рекурсивный список", "Recursive list")),
        CmdEntry("cd /path", s("Перейти в директорию", "Go to directory")),
        CmdEntry("cd ..", s("На уровень выше", "Up one level")),
        CmdEntry("cd ~", s("В домашнюю директорию", "Go to home directory")),
        CmdEntry("cd -", s("В предыдущую директорию", "Previous directory")),
        CmdEntry("cd /sdcard", s("В хранилище Android", "Android storage")),
        CmdEntry("tree", s("Дерево каталогов", "Directory tree")),
        CmdEntry("tree -L 2", s("Дерево глубиной 2 уровня", "Tree 2 levels deep")),
        CmdEntry("tree -a", s("Дерево с скрытыми файлами", "Tree with hidden files")),
        CmdEntry("find / -name 'file'", s("Найти файл по имени", "Find file by name")),
        CmdEntry("find . -type f -size +100M", s("Файлы больше 100МБ", "Files larger than 100MB")),
        CmdEntry("find . -name '*.kt' -type f", s("Найти все .kt файлы", "Find all .kt files")),
        CmdEntry("find . -mtime -1", s("Изменённые за последний день", "Modified last day")),
        CmdEntry("find . -empty", s("Пустые файлы и папки", "Empty files and directories")),
        CmdEntry("which command", s("Путь к исполняемому файлу", "Path to executable")),
        CmdEntry("whereis command", s("Расположение бинарника", "Binary location")),
        CmdEntry("type command", s("Тип команды (builtin/alias/file)", "Command type (builtin/alias/file)")),
    )),
    CmdCategory(s("Файлы и папки", "Files & Folders"), "📄", listOf(
        CmdEntry("touch file.txt", s("Создать пустой файл", "Create empty file")),
        CmdEntry("mkdir folder", s("Создать папку", "Create directory")),
        CmdEntry("mkdir -p a/b/c", s("Создать вложенные папки", "Create nested directories")),
        CmdEntry("cp file1 file2", s("Копировать файл", "Copy file")),
        CmdEntry("cp -r dir1 dir2", s("Копировать папку рекурсивно", "Copy directory recursively")),
        CmdEntry("cp -v file1 file2", s("Копировать с выводом", "Copy with output")),
        CmdEntry("mv old new", s("Переместить / переименовать", "Move / rename")),
        CmdEntry("rm file", s("Удалить файл", "Delete file")),
        CmdEntry("rm -rf dir", s("Удалить папку рекурсивно", "Remove directory recursively")),
        CmdEntry("rm -i file", s("Удалить с подтверждением", "Delete with confirmation")),
        CmdEntry("ln -s target link", s("Символическая ссылка", "Symbolic link")),
        CmdEntry("ln target link", s("Жёсткая ссылка", "Hard link")),
        CmdEntry("readlink -f link", s("Куда ведёт ссылка", "Link target")),
        CmdEntry("chmod 755 file", s("Изменить права доступа", "Change permissions")),
        CmdEntry("chmod +x script.sh", s("Сделать исполняемым", "Make executable")),
        CmdEntry("chmod -R 644 dir", s("Права рекурсивно", "Permissions recursively")),
        CmdEntry("chown user:group file", s("Сменить владельца", "Change owner")),
        CmdEntry("stat file", s("Подробная информация о файле", "Detailed file info")),
        CmdEntry("file filename", s("Определить тип файла", "Determine file type")),
        CmdEntry("du -sh *", s("Размер каждого элемента", "Size of each item")),
        CmdEntry("du -sh .", s("Размер текущей папки", "Current directory size")),
        CmdEntry("du -sh * | sort -rh", s("Отсортировать по размеру", "Sort by size")),
        CmdEntry("df -h", s("Свободное место на дисках", "Free space on disks")),
        CmdEntry("df -hT", s("Место с типами файловых систем", "Space with filesystem types")),
        CmdEntry("basename /path/to/file", s("Имя файла из пути", "Filename from path")),
        CmdEntry("dirname /path/to/file", s("Директория из пути", "Directory from path")),
        CmdEntry("realpath file", s("Полный абсолютный путь", "Full absolute path")),
        CmdEntry("md5sum file", s("MD5 хеш файла", "MD5 file hash")),
        CmdEntry("sha256sum file", s("SHA256 хеш файла", "SHA256 file hash")),
    )),
    CmdCategory(s("Просмотр и редактирование", "View & Edit"), "📝", listOf(
        CmdEntry("cat file", s("Вывести содержимое", "Print contents")),
        CmdEntry("cat file1 file2 > merged", s("Объединить файлы", "Merge files")),
        CmdEntry("less file", s("Постраничный просмотр", "Paged view")),
        CmdEntry("more file", s("Просмотр (простой)", "View (simple)")),
        CmdEntry("head -n 20 file", s("Первые 20 строк", "First 20 lines")),
        CmdEntry("tail -n 20 file", s("Последние 20 строк", "Last 20 lines")),
        CmdEntry("tail -f log.txt", s("Следить за файлом в реальном времени", "Follow file in real time")),
        CmdEntry("nano file", s("Редактор Nano (простой)", "Nano editor (simple)")),
        CmdEntry("vim file", s("Редактор Vim (продвинутый)", "Vim editor (advanced)")),
        CmdEntry("vi file", s("Редактор Vi", "Vi editor")),
        CmdEntry("wc -l file", s("Подсчитать строки", "Count lines")),
        CmdEntry("wc -w file", s("Подсчитать слова", "Count words")),
        CmdEntry("wc -c file", s("Подсчитать байты", "Count bytes")),
        CmdEntry("diff file1 file2", s("Сравнить файлы", "Compare files")),
        CmdEntry("diff -u file1 file2", s("Unified diff формат", "Unified diff format")),
        CmdEntry("sort file", s("Отсортировать строки", "Sort lines")),
        CmdEntry("sort -n file", s("Сортировка по числам", "Sort by numbers")),
        CmdEntry("sort -r file", s("Обратная сортировка", "Reverse sort")),
        CmdEntry("uniq", s("Убрать дубликаты (после sort)", "Remove duplicates (after sort)")),
        CmdEntry("uniq -c", s("С количеством повторов", "With occurrence count")),
        CmdEntry("tee file", s("Записать вывод в файл и на экран", "Write output to file and screen")),
        CmdEntry("tr 'a-z' 'A-Z'", s("Замена символов (в верхний регистр)", "Character replace (uppercase)")),
        CmdEntry("rev", s("Перевернуть строку", "Reverse string")),
        CmdEntry("nl file", s("Пронумеровать строки", "Number lines")),
        CmdEntry("xxd file | head", s("Hex-дамп файла", "Hex dump of file")),
    )),
    CmdCategory(s("Поиск и фильтры", "Search & Filters"), "🔍", listOf(
        CmdEntry("grep 'text' file", s("Найти текст в файле", "Find text in file")),
        CmdEntry("grep -r 'text' dir", s("Рекурсивный поиск", "Recursive search")),
        CmdEntry("grep -i 'text' file", s("Без учёта регистра", "Case insensitive")),
        CmdEntry("grep -n 'text' file", s("С номерами строк", "With line numbers")),
        CmdEntry("grep -c 'text' file", s("Количество совпадений", "Match count")),
        CmdEntry("grep -v 'text' file", s("Строки БЕЗ совпадений", "Lines WITHOUT matches")),
        CmdEntry("grep -l 'text' *.kt", s("Только имена файлов", "File names only")),
        CmdEntry("grep -E 'a|b' file", s("Регулярное выражение (или)", "Regex (or)")),
        CmdEntry("grep -w 'word' file", s("Только целые слова", "Whole words only")),
        CmdEntry("awk '{print \$1}' file", s("Вывести первый столбец", "Print first column")),
        CmdEntry("awk -F',' '{print \$2}' f", s("CSV: второй столбец", "CSV: second column")),
        CmdEntry("awk 'NR==5' file", s("Только 5-я строка", "Only 5th line")),
        CmdEntry("awk 'END{print NR}' file", s("Количество строк", "Line count")),
        CmdEntry("sed 's/old/new/g' file", s("Заменить текст", "Replace text")),
        CmdEntry("sed -i 's/old/new/g' file", s("Заменить в файле", "Replace in file")),
        CmdEntry("sed -n '5,10p' file", s("Строки с 5 по 10", "Lines 5 to 10")),
        CmdEntry("sed '/pattern/d' file", s("Удалить строки с паттерном", "Delete lines with pattern")),
        CmdEntry("cut -d',' -f1 file.csv", s("Первый столбец CSV", "First CSV column")),
        CmdEntry("cut -c1-10 file", s("Первые 10 символов каждой строки", "First 10 chars of each line")),
        CmdEntry("xargs", s("Передать вывод как аргументы", "Pass output as arguments")),
        CmdEntry("xargs -I{} cmd {}", s("Подстановка в команду", "Command substitution")),
    )),
    CmdCategory(s("Архивы и сжатие", "Archives & Compression"), "📦", listOf(
        CmdEntry("tar czf archive.tar.gz dir", s("Создать .tar.gz", "Create .tar.gz")),
        CmdEntry("tar xzf archive.tar.gz", s("Распаковать .tar.gz", "Extract .tar.gz")),
        CmdEntry("tar xf archive.tar", s("Распаковать .tar", "Extract .tar")),
        CmdEntry("tar tf archive.tar.gz", s("Список файлов в архиве", "List files in archive")),
        CmdEntry("tar czf - dir | wc -c", s("Размер архива без создания", "Archive size without creating")),
        CmdEntry("zip -r archive.zip dir", s("Создать .zip", "Create .zip")),
        CmdEntry("zip -e archive.zip file", s("ZIP с паролем", "ZIP with password")),
        CmdEntry("unzip archive.zip", s("Распаковать .zip", "Extract .zip")),
        CmdEntry("unzip -l archive.zip", s("Список файлов в zip", "List files in zip")),
        CmdEntry("unzip -d /path archive.zip", s("Распаковать в папку", "Extract to directory")),
        CmdEntry("gzip file", s("Сжать файл (gzip)", "Compress file (gzip)")),
        CmdEntry("gunzip file.gz", s("Распаковать gzip", "Extract gzip")),
        CmdEntry("gzip -k file", s("Сжать, оставив оригинал", "Compress, keep original")),
        CmdEntry("bzip2 file", s("Сжать (bzip2, лучше сжатие)", "Compress (bzip2, better ratio)")),
        CmdEntry("bunzip2 file.bz2", s("Распаковать bzip2", "Extract bzip2")),
        CmdEntry("xz file", s("Сжать (xz, лучшее сжатие)", "Compress (xz, best ratio)")),
        CmdEntry("unxz file.xz", s("Распаковать xz", "Extract xz")),
        CmdEntry("zcat file.gz", s("Прочитать gzip без распаковки", "Read gzip without extracting")),
        CmdEntry("zgrep 'text' file.gz", s("Grep внутри gzip", "Grep inside gzip")),
    )),
    CmdCategory(s("Процессы", "Processes"), "⚙️", listOf(
        CmdEntry("ps aux", s("Все процессы", "All processes")),
        CmdEntry("ps aux | grep name", s("Найти процесс", "Find process")),
        CmdEntry("ps -ef --forest", s("Дерево процессов", "Process tree")),
        CmdEntry("top", s("Монитор процессов", "Process monitor")),
        CmdEntry("htop", s("Продвинутый монитор", "Advanced monitor")),
        CmdEntry("kill PID", s("Завершить процесс", "Kill process")),
        CmdEntry("kill -9 PID", s("Принудительно завершить", "Force kill")),
        CmdEntry("killall name", s("Завершить по имени", "Kill by name")),
        CmdEntry("pkill -f pattern", s("Убить по имени/паттерну", "Kill by name/pattern")),
        CmdEntry("bg", s("Продолжить в фоне", "Continue in background")),
        CmdEntry("fg", s("Вернуть на передний план", "Bring to foreground")),
        CmdEntry("fg %2", s("Конкретную задачу на передний план", "Specific job to foreground")),
        CmdEntry("jobs", s("Список фоновых задач", "List background jobs")),
        CmdEntry("nohup command &", s("Запустить в фоне (не завершится)", "Run in background (persistent)")),
        CmdEntry("command &", s("Запустить в фоне", "Run in background")),
        CmdEntry("disown %1", s("Отвязать процесс от терминала", "Detach process from terminal")),
        CmdEntry("nice -n 10 command", s("Запустить с низким приоритетом", "Run with low priority")),
        CmdEntry("time command", s("Замерить время выполнения", "Measure execution time")),
        CmdEntry("timeout 10 command", s("Ограничить время (10 сек)", "Limit time (10 sec)")),
        CmdEntry("watch -n 2 command", s("Повторять каждые 2 сек", "Repeat every 2 sec")),
        CmdEntry("Ctrl+C", s("Прервать выполнение", "Abort execution")),
        CmdEntry("Ctrl+Z", s("Приостановить процесс", "Suspend process")),
        CmdEntry("Ctrl+D", s("Конец ввода / выход из shell", "End input / exit shell")),
    )),
    CmdCategory(s("Сеть", "Network"), "🌐", listOf(
        CmdEntry("ping -c 3 host", s("Проверить доступность (3 пакета)", "Check availability (3 pings)")),
        CmdEntry("curl url", s("Скачать содержимое URL", "Download URL contents")),
        CmdEntry("curl -O url", s("Скачать файл (сохранить)", "Download file (save)")),
        CmdEntry("curl -I url", s("Только заголовки", "Headers only")),
        CmdEntry("curl -s url | jq .", s("JSON ответ (красиво)", "JSON response (pretty)")),
        CmdEntry("curl -X POST -d 'data' url", s("POST запрос", "POST request")),
        CmdEntry("curl -u user:pass url", s("С авторизацией", "With auth")),
        CmdEntry("wget url", s("Скачать файл", "Download file")),
        CmdEntry("wget -r url", s("Рекурсивное скачивание", "Recursive download")),
        CmdEntry("wget -c url", s("Докачать файл", "Resume download")),
        CmdEntry("wget -q -O - url", s("Скачать в stdout (тихо)", "Download to stdout (quiet)")),
        CmdEntry("ip addr", s("IP адреса", "IP addresses")),
        CmdEntry("ifconfig", s("Сетевые интерфейсы", "Network interfaces")),
        CmdEntry("netstat -tulnp", s("Открытые порты", "Open ports")),
        CmdEntry("ss -tulnp", s("Открытые порты (новый)", "Open ports (new)")),
        CmdEntry("ssh user@host", s("SSH подключение", "SSH connection")),
        CmdEntry("ssh -p 2222 user@host", s("SSH на другой порт", "SSH on different port")),
        CmdEntry("ssh-keygen -t ed25519", s("Сгенерировать SSH ключ", "Generate SSH key")),
        CmdEntry("scp file user@host:/path", s("Копировать по SSH", "Copy via SSH")),
        CmdEntry("scp -r dir user@host:/path", s("Папку по SSH", "Directory via SSH")),
        CmdEntry("nc -l -p 8080", s("Слушать порт", "Listen on port")),
        CmdEntry("nc host port", s("Подключиться к порту", "Connect to port")),
        CmdEntry("nslookup domain", s("DNS запрос", "DNS query")),
        CmdEntry("host domain", s("DNS информация", "DNS info")),
        CmdEntry("dig domain", s("Подробный DNS запрос", "Detailed DNS query")),
        CmdEntry("curl ifconfig.me", s("Узнать внешний IP", "Get external IP")),
        CmdEntry("python3 -m http.server 8000", s("Простой HTTP сервер", "Simple HTTP server")),
    )),
    CmdCategory("Git", "🔀", listOf(
        CmdEntry("git init", s("Инициализировать репо", "Init repo")),
        CmdEntry("git clone url", s("Клонировать репо", "Clone repo")),
        CmdEntry("git clone --depth 1 url", s("Клонировать (только последний коммит)", "Clone (last commit only)")),
        CmdEntry("git status", s("Статус изменений", "Change status")),
        CmdEntry("git add .", s("Добавить все файлы", "Add all files")),
        CmdEntry("git add file", s("Добавить файл", "Add file")),
        CmdEntry("git add -p", s("Интерактивное добавление", "Interactive add")),
        CmdEntry("git commit -m 'msg'", s("Коммит", "Commit")),
        CmdEntry("git commit -am 'msg'", s("Add + commit (tracked файлы)", "Add + commit (tracked files)")),
        CmdEntry("git push", s("Отправить на сервер", "Push to server")),
        CmdEntry("git push -u origin main", s("Первый push", "First push")),
        CmdEntry("git pull", s("Получить с сервера", "Pull from server")),
        CmdEntry("git pull --rebase", s("Pull с rebase", "Pull with rebase")),
        CmdEntry("git log --oneline", s("История коммитов (кратко)", "Commit history (compact)")),
        CmdEntry("git log --oneline --graph", s("Граф веток", "Branch graph")),
        CmdEntry("git log -5", s("Последние 5 коммитов", "Last 5 commits")),
        CmdEntry("git branch", s("Список веток", "List branches")),
        CmdEntry("git branch name", s("Создать ветку", "Create branch")),
        CmdEntry("git branch -d name", s("Удалить ветку", "Delete branch")),
        CmdEntry("git checkout branch", s("Переключить ветку", "Switch branch")),
        CmdEntry("git checkout -b new", s("Создать и переключить", "Create and switch")),
        CmdEntry("git merge branch", s("Слить ветку", "Merge branch")),
        CmdEntry("git diff", s("Показать изменения", "Show changes")),
        CmdEntry("git diff --staged", s("Изменения в stage", "Staged changes")),
        CmdEntry("git stash", s("Отложить изменения", "Stash changes")),
        CmdEntry("git stash pop", s("Вернуть отложенные", "Pop stash")),
        CmdEntry("git stash list", s("Список отложенных", "List stash")),
        CmdEntry("git reset --hard HEAD", s("Откатить все изменения", "Revert all changes")),
        CmdEntry("git reset HEAD~1", s("Отменить последний коммит", "Undo last commit")),
        CmdEntry("git cherry-pick hash", s("Применить конкретный коммит", "Cherry-pick commit")),
        CmdEntry("git remote -v", s("Список удалённых репо", "List remotes")),
        CmdEntry("git tag v1.0", s("Создать тег", "Create tag")),
        CmdEntry("git config --global user.name 'n'", s("Установить имя", "Set name")),
        CmdEntry("git config --global user.email 'e'", s("Установить email", "Set email")),
    )),
    CmdCategory(s("Системная информация", "System Info"), "💻", listOf(
        CmdEntry("uname -a", s("Информация о системе", "System information")),
        CmdEntry("uname -m", s("Архитектура (aarch64)", "Architecture (aarch64)")),
        CmdEntry("cat /etc/os-release", s("Версия ОС", "OS version")),
        CmdEntry("cat /proc/cpuinfo", s("Информация о ядрах CPU", "CPU core info")),
        CmdEntry("lscpu", s("Детали процессора", "CPU details")),
        CmdEntry("hostname", s("Имя хоста", "Hostname")),
        CmdEntry("uptime", s("Время работы", "Uptime")),
        CmdEntry("whoami", s("Текущий пользователь", "Current user")),
        CmdEntry("id", s("ID пользователя и группы", "User and group ID")),
        CmdEntry("date", s("Текущая дата и время", "Current date and time")),
        CmdEntry("date +%s", "Unix timestamp"),
        CmdEntry("date -d @1700000000", s("Timestamp в дату", "Timestamp to date")),
        CmdEntry("cal", s("Календарь", "Calendar")),
        CmdEntry("cal 2026", s("Календарь на год", "Year calendar")),
        CmdEntry("free -h", s("Использование памяти", "Memory usage")),
        CmdEntry("lsblk", s("Блочные устройства", "Block devices")),
        CmdEntry("mount", s("Смонтированные файловые системы", "Mounted filesystems")),
        CmdEntry("env", s("Переменные окружения", "Environment variables")),
        CmdEntry("printenv PATH", s("Значение переменной", "Variable value")),
        CmdEntry("echo \$PATH", s("Текущий PATH", "Current PATH")),
        CmdEntry("echo \$SHELL", s("Текущий shell", "Current shell")),
        CmdEntry("export VAR=value", s("Установить переменную", "Set variable")),
        CmdEntry("unset VAR", s("Удалить переменную", "Unset variable")),
        CmdEntry("history", s("История команд", "Command history")),
        CmdEntry("history | grep cmd", s("Поиск в истории", "Search history")),
        CmdEntry("!!", s("Повторить последнюю команду", "Repeat last command")),
        CmdEntry("!n", s("Повторить команду #n из истории", "Repeat command #n from history")),
        CmdEntry("neofetch", s("Системная инфо (красиво)", "System info (pretty)")),
    )),
    CmdCategory(s("Права и пользователи", "Permissions & Users"), "👤", listOf(
        CmdEntry("adduser name", s("Добавить пользователя", "Add user")),
        CmdEntry("passwd", s("Сменить пароль", "Change password")),
        CmdEntry("su - user", s("Переключить пользователя", "Switch user")),
        CmdEntry("groups", s("Группы пользователя", "User groups")),
        CmdEntry("chmod 644 file", s("rw-r--r-- (чтение всем)", "rw-r--r-- (read for all)")),
        CmdEntry("chmod 755 file", s("rwxr-xr-x (исполняемый)", "rwxr-xr-x (executable)")),
        CmdEntry("chmod 600 file", s("rw------- (только владелец)", "rw------- (owner only)")),
        CmdEntry("chmod 777 file", s("rwxrwxrwx (все права — опасно!)", "rwxrwxrwx (all perms — dangerous!)")),
        CmdEntry("chmod +x script.sh", s("Сделать исполняемым", "Make executable")),
        CmdEntry("chmod -R 755 dir", s("Рекурсивно на папку", "Recursively on directory")),
        CmdEntry("chown user file", s("Сменить владельца", "Change owner")),
        CmdEntry("chown -R user:group dir", s("Рекурсивно", "Recursively")),
    )),
    CmdCategory(s("Перенаправление и пайпы", "Redirects & Pipes"), "🔗", listOf(
        CmdEntry("cmd > file", s("Вывод в файл (перезапись)", "Output to file (overwrite)")),
        CmdEntry("cmd >> file", s("Добавить в файл", "Append to file")),
        CmdEntry("cmd 2> file", s("Ошибки в файл", "Errors to file")),
        CmdEntry("cmd 2>&1", s("Ошибки → стандартный вывод", "Errors → stdout")),
        CmdEntry("cmd &> file", s("Всё в файл", "All to file")),
        CmdEntry("cmd1 | cmd2", s("Пайп: выход → вход", "Pipe: output → input")),
        CmdEntry("cmd < file", s("Файл как ввод", "File as input")),
        CmdEntry("cmd1 && cmd2", s("cmd2 если cmd1 успех", "cmd2 if cmd1 success")),
        CmdEntry("cmd1 || cmd2", s("cmd2 если cmd1 ошибка", "cmd2 if cmd1 error")),
        CmdEntry("cmd1 ; cmd2", s("Выполнить обе последовательно", "Execute both sequentially")),
        CmdEntry("(cmd1 ; cmd2) > file", s("Группировка вывода", "Output grouping")),
        CmdEntry("cmd <<< 'text'", s("Строка как ввод", "String as input")),
        CmdEntry("cat << EOF\ntext\nEOF", "Here-document"),
    )),
    CmdCategory(s("Tmux (мультиплексор)", "Tmux (multiplexer)"), "🖥️", listOf(
        CmdEntry("tmux", s("Запустить tmux", "Start tmux")),
        CmdEntry("tmux new -s name", s("Новая сессия с именем", "New named session")),
        CmdEntry("tmux ls", s("Список сессий", "List sessions")),
        CmdEntry("tmux attach -t name", s("Подключиться к сессии", "Attach to session")),
        CmdEntry("tmux kill-session -t name", s("Убить сессию", "Kill session")),
        CmdEntry("Ctrl+B, D", s("Отключиться от сессии", "Detach from session")),
        CmdEntry("Ctrl+B, C", s("Новое окно", "New window")),
        CmdEntry("Ctrl+B, N", s("Следующее окно", "Next window")),
        CmdEntry("Ctrl+B, P", s("Предыдущее окно", "Previous window")),
        CmdEntry("Ctrl+B, %", s("Разделить вертикально", "Split vertically")),
        CmdEntry("Ctrl+B, \"", s("Разделить горизонтально", "Split horizontally")),
        CmdEntry(s("Ctrl+B, стрелки", "Ctrl+B, arrows"), s("Перейти между панелями", "Switch between panes")),
    )),
    CmdCategory("Python", "🐍", listOf(
        CmdEntry("python3 --version", s("Версия Python", "Python version")),
        CmdEntry("python3 script.py", s("Запустить скрипт", "Run script")),
        CmdEntry("python3 -c 'print(1+1)'", s("Однострочник", "One-liner")),
        CmdEntry("python3 -m venv env", s("Создать виртуальное окружение", "Create virtual env")),
        CmdEntry("source env/bin/activate", s("Активировать venv", "Activate venv")),
        CmdEntry("deactivate", s("Деактивировать venv", "Deactivate venv")),
        CmdEntry("python3 -m http.server", s("HTTP сервер (порт 8000)", "HTTP server (port 8000)")),
        CmdEntry("python3 -m json.tool f.json", s("Форматировать JSON", "Format JSON")),
        CmdEntry("python3 -m pip install -r req.txt", s("Установить из файла", "Install from file")),
        CmdEntry("python3 -c 'import sys; print(sys.platform)'", s("Платформа", "Platform")),
    )),
    CmdCategory("Bun (JS runtime)", "🥟", listOf(
        CmdEntry("bun --version", s("Версия Bun", "Bun version")),
        CmdEntry("bun run script.js", s("Запустить JS файл", "Run JS file")),
        CmdEntry("bun run script.ts", s("Запустить TypeScript", "Run TypeScript")),
        CmdEntry("bun init", s("Создать новый проект", "Create new project")),
        CmdEntry("bun add express", s("Установить пакет", "Install package")),
        CmdEntry("bun add -d typescript", s("Dev-зависимость", "Dev dependency")),
        CmdEntry("bun remove package", s("Удалить пакет", "Remove package")),
        CmdEntry("bun install", s("Установить все зависимости", "Install all dependencies")),
        CmdEntry("bun update", s("Обновить зависимости", "Update dependencies")),
        CmdEntry("bunx create-react-app app", s("Создать React проект", "Create React project")),
        CmdEntry("bunx serve .", s("Быстрый HTTP сервер", "Quick HTTP server")),
        CmdEntry("bun build index.ts --outdir ./out", s("Собрать проект", "Build project")),
        CmdEntry("bun test", s("Запустить тесты", "Run tests")),
        CmdEntry("bun repl", s("Интерактивная консоль JS", "Interactive JS console")),
    )),
    CmdCategory("AI Coding Agents", "🤖", listOf(
        CmdEntry("opencode", s("Запустить OpenCode (AI агент)", "Run OpenCode (AI agent)")),
        CmdEntry("pip install aider-chat", s("Установить Aider", "Install Aider")),
        CmdEntry("aider --model gemini/gemini-2.5-flash", s("Aider с Gemini", "Aider with Gemini")),
        CmdEntry("aider --model openrouter/...", s("Aider с OpenRouter", "Aider with OpenRouter")),
        CmdEntry("export GEMINI_API_KEY=...", s("Ключ Gemini для Aider", "Gemini key for Aider")),
        CmdEntry("export OPENROUTER_API_KEY=...", s("Ключ OpenRouter для Aider", "OpenRouter key for Aider")),
        CmdEntry("aider --help", s("Справка Aider", "Aider help")),
        CmdEntry("aider file1.py file2.py", s("Aider с конкретными файлами", "Aider with specific files")),
    )),
    CmdCategory(s("Утилиты", "Utilities"), "🔧", listOf(
        CmdEntry("pfetch", s("Инфо о системе (минимально)", "System info (minimal)")),
        CmdEntry("neofetch", s("Инфо о системе (красиво)", "System info (pretty)")),
        CmdEntry("htop", s("Монитор процессов", "Process monitor")),
        CmdEntry("mc", s("Midnight Commander (файлы)", "Midnight Commander (files)")),
        CmdEntry("tmux", s("Мультиплексор терминала", "Terminal multiplexer")),
        CmdEntry("screen", s("Сессии в фоне", "Background sessions")),
        CmdEntry("jq '.key' file.json", s("Парсер JSON", "JSON parser")),
        CmdEntry("tree -L 2", s("Дерево каталогов", "Directory tree")),
        CmdEntry("watch -n 1 'date'", s("Повтор команды каждую секунду", "Repeat command every second")),
        CmdEntry("cal", s("Календарь", "Calendar")),
        CmdEntry("bc", s("Калькулятор", "Calculator")),
    )),
    CmdCategory(s("Полезные однострочники", "Useful One-liners"), "⚡", listOf(
        CmdEntry("find . -name '*.log' -delete", s("Удалить все .log", "Delete all .log")),
        CmdEntry("du -sh * | sort -rh | head", s("Топ-10 по размеру", "Top 10 by size")),
        CmdEntry("find . -type f | wc -l", s("Количество файлов", "File count")),
        CmdEntry("grep -rl 'text' .", s("Файлы содержащие текст", "Files containing text")),
        CmdEntry("find . -name '*.kt' | xargs grep 'fun'", s("Grep по kt файлам", "Grep in kt files")),
        CmdEntry("find . -type f -exec chmod 644 {} +", s("Массовое chmod", "Bulk chmod")),
        CmdEntry("for f in *.txt; do echo \$f; done", s("Цикл по файлам", "Loop over files")),
        CmdEntry("while read l; do echo \$l; done < f", s("Чтение файла построчно", "Read file line by line")),
        CmdEntry("echo 'text' | base64", s("Кодировать в base64", "Encode to base64")),
        CmdEntry("echo 'dGV4dA==' | base64 -d", s("Декодировать base64", "Decode base64")),
        CmdEntry("openssl rand -hex 16", s("Случайный hex (32 символа)", "Random hex (32 chars)")),
        CmdEntry("date +%Y%m%d_%H%M%S", s("Дата для имени файла", "Date for filename")),
        CmdEntry("seq 1 100 | shuf | head -1", s("Случайное число 1-100", "Random number 1-100")),
        CmdEntry("ls -1 | wc -l", s("Количество файлов в папке", "File count in directory")),
        CmdEntry("diff <(sort f1) <(sort f2)", s("Сравнить отсортированные", "Compare sorted")),
        CmdEntry("paste file1 file2", s("Объединить по столбцам", "Join by columns")),
        CmdEntry("column -t -s',' file.csv", s("Таблица из CSV", "Table from CSV")),
        CmdEntry("yes | head -5", s("Повторить 'y' 5 раз", "Repeat 'y' 5 times")),
        CmdEntry("printf '%*s\\n' 40 '' | tr ' ' '-'", s("Линия из 40 дефисов", "Line of 40 dashes")),
        CmdEntry("alias ll='ls -la'", s("Создать alias", "Create alias")),
        CmdEntry("tar czf - dir | ssh host 'tar xzf -'", s("Копировать папку по SSH", "Copy directory via SSH")),
    )),
    CmdCategory(s(s("Bash скрипты", "Bash Scripts"), "Bash Scripts"), "📜", listOf(
        CmdEntry("#!/bin/bash", s("Shebang (первая строка скрипта)", "Shebang (first line of script)")),
        CmdEntry("VAR=\"value\"", s("Переменная", "Variable")),
        CmdEntry("echo \$VAR", s("Вывести переменную", "Print variable")),
        CmdEntry(s("read -p 'Имя: ' NAME", "read -p 'Name: ' NAME"), s("Ввод от пользователя", "User input")),
        CmdEntry("if [ -f file ]; then ... fi", s("Если файл существует", "If file exists")),
        CmdEntry("if [ -d dir ]; then ... fi", s("Если папка существует", "If directory exists")),
        CmdEntry("if [ -z \"\$VAR\" ]; then ... fi", s("Если пустая строка", "If empty string")),
        CmdEntry("for i in 1 2 3; do echo \$i; done", s("Цикл for", "For loop")),
        CmdEntry("for f in *.txt; do echo \$f; done", s("По файлам", "By files")),
        CmdEntry("while true; do cmd; sleep 1; done", s("Бесконечный цикл", "Infinite loop")),
        CmdEntry("case \$VAR in a) ... ;; esac", "Switch/case"),
        CmdEntry("\$1, \$2, \$@", s("Аргументы скрипта", "Script arguments")),
        CmdEntry("\$?", s("Код возврата последней команды", "Last command exit code")),
        CmdEntry("\$!", s("PID последнего фонового процесса", "PID of last background process")),
        CmdEntry("exit 0", s("Выход с кодом 0 (успех)", "Exit with code 0 (success)")),
        CmdEntry("set -e", s("Остановка при ошибке", "Stop on error")),
        CmdEntry("set -x", s("Вывод каждой команды (debug)", "Print each command (debug)")),
    )),
)

private fun getCategories(isAlpine: Boolean): List<CmdCategory> {
    val pkgs = if (isAlpine) alpinePackages else ubuntuPackages
    val soft = if (isAlpine) alpineSoftware else ubuntuSoftware
    // Вставляем пакеты после s("Архивы", "Archives") (индекс 4) и софт после пакетов
    val result = commonCategories.toMutableList()
    val insertIdx = minOf(4, result.size)
    result.add(insertIdx, pkgs)
    result.add(insertIdx + 1, soft)
    return result
}

@Composable
fun CommandsReferenceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var isAlpine by remember { mutableStateOf(true) }

    val allCategories = remember(isAlpine) { getCategories(isAlpine) }

    val filtered = if (searchQuery.length < 2) allCategories else {
        val q = searchQuery.lowercase()
        allCategories.map { cat ->
            cat.copy(commands = cat.commands.filter {
                it.cmd.lowercase().contains(q) || it.desc.lowercase().contains(q)
            })
        }.filter { it.commands.isNotEmpty() }
    }

    fun copy(text: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("cmd", text))
        Toast.makeText(context, s("Скопировано: $text", "Copied: $text"), Toast.LENGTH_SHORT).show()
    }

    val totalCmds = remember(isAlpine) { allCategories.sumOf { it.commands.size } }
    val distroLabel = if (isAlpine) "Alpine" else "Ubuntu"

    Column(Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(top = 36.dp, start = 4.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Color.White) }
            Text(s("Команды", "Commands"), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text("$totalCmds", color = Color(0xFF888888), fontSize = 13.sp)
        }

        // Переключатель Alpine / Ubuntu
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(true to "Alpine", false to "Ubuntu").forEach { (alpine, label) ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (isAlpine == alpine) Color(0xFF00E676).copy(0.15f) else Color(0xFF1A1A1A))
                        .border(1.dp, if (isAlpine == alpine) Color(0xFF00E676).copy(0.4f) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { isAlpine = alpine }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isAlpine == alpine) Color(0xFF00E676) else Color(0xFF888888),
                        fontSize = 14.sp, fontWeight = if (isAlpine == alpine) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text(s("Поиск команды...", "Search command..."), color = Color(0xFF555555)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFF555555)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Rounded.Clear, null, Modifier.size(18.dp), tint = Color(0xFF555555))
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            singleLine = true, shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color(0xFF222222),
                cursorColor = Color(0xFF00E676), focusedTextColor = Color.White, unfocusedTextColor = Color.White
            )
        )

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filtered.forEach { cat ->
                val isExpanded = expandedCategory == cat.name || searchQuery.length >= 2
                item(key = "h_${cat.name}") {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A1A))
                        .clickable { expandedCategory = if (expandedCategory == cat.name) null else cat.name }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(cat.icon, fontSize = 20.sp)
                        Column(Modifier.weight(1f)) {
                            Text(cat.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("${cat.commands.size} ${s("команд", "cmds")}", color = Color(0xFF666666), fontSize = 12.sp)
                        }
                        Icon(if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, Modifier.size(20.dp), tint = Color(0xFF555555))
                    }
                }
                if (isExpanded) {
                    items(cat.commands, key = { "c_${cat.name}_${it.cmd}" }) { entry ->
                        Row(Modifier.fillMaxWidth().padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF111111))
                            .clickable { copy(entry.cmd) }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(entry.cmd, color = Color(0xFF00E676), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.45f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(entry.desc, color = Color(0xFF999999), fontSize = 12.sp, modifier = Modifier.weight(0.45f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp), tint = Color(0xFF444444))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
