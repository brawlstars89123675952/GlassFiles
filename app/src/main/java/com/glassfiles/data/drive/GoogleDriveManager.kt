package com.glassfiles.data.drive

import android.content.Context
import android.content.Intent
import android.util.Log
import com.glassfiles.data.FileItem
import com.glassfiles.data.FileType
import com.glassfiles.data.FolderColor
import com.glassfiles.data.getFileType
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class DriveResult(
    val files: List<FileItem> = emptyList(),
    val error: String = "",
    val debug: String = ""
)

object GoogleDriveManager {

    private const val TAG = "GDRIVE"
    private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(context: Context): Intent {
        return getSignInClient(context).signInIntent
    }

    fun getCurrentAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun signOut(context: Context) {
        withContext(Dispatchers.IO) {
            getSignInClient(context).signOut()
        }
    }

    fun isSignedIn(context: Context): Boolean {
        val account = getCurrentAccount(context)
        return account != null && !account.isExpired
    }

    /**
     * List files with full debug info returned
     */
    suspend fun listFilesDebug(context: Context, folderId: String = "root"): DriveResult {
        return withContext(Dispatchers.IO) {
            val debugLog = StringBuilder()

            // Step 1: Check account
            val account = getCurrentAccount(context)
            if (account == null) {
                return@withContext DriveResult(error = "Account not found. Sign in again.")
            }
            debugLog.appendLine("Account: ${account.email}")
            debugLog.appendLine("Scopes: ${account.grantedScopes?.joinToString { it.scopeUri }}")

            // Step 2: Get token
            val token: String?
            try {
                token = account.account?.let { acc ->
                    com.google.android.gms.auth.GoogleAuthUtil.getToken(
                        context,
                        acc,
                        "oauth2:${DriveScopes.DRIVE_READONLY}"
                    )
                }
            } catch (e: Exception) {
                return@withContext DriveResult(
                    error = "Token error: ${e.javaClass.simpleName}: ${e.message}",
                    debug = debugLog.toString()
                )
            }

            if (token == null) {
                return@withContext DriveResult(error = "Token null", debug = debugLog.toString())
            }
            debugLog.appendLine("Token received (${token.length} chars)")

            // Step 3: Make API request
            try {
                val query = "'$folderId' in parents and trashed = false"
                val fields = "files(id,name,mimeType,size,modifiedTime)"
                val urlStr = "$DRIVE_FILES_URL?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=${java.net.URLEncoder.encode(fields, "UTF-8")}&orderBy=folder,name&pageSize=100"

                debugLog.appendLine("URL: $urlStr")

                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val responseCode = conn.responseCode
                debugLog.appendLine("HTTP: $responseCode")

                if (responseCode == 200) {
                    val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                    debugLog.appendLine("Response: ${response.length} chars")
                    debugLog.appendLine("Preview: ${response.take(300)}")
                    val files = parseDriveFiles(response)
                    debugLog.appendLine("Files: ${files.size}")
                    DriveResult(files = files, debug = debugLog.toString())
                } else {
                    val errorBody = try {
                        BufferedReader(InputStreamReader(conn.errorStream)).readText()
                    } catch (e: Exception) { "no error body" }
                    debugLog.appendLine("Error: $errorBody")
                    DriveResult(
                        error = "HTTP $responseCode",
                        debug = debugLog.toString()
                    )
                }
            } catch (e: Exception) {
                DriveResult(
                    error = "${e.javaClass.simpleName}: ${e.message}",
                    debug = debugLog.toString()
                )
            }
        }
    }

    /**
     * Simple version (calls debug version internally)
     */
    suspend fun listFiles(context: Context, folderId: String = "root"): List<FileItem> {
        return listFilesDebug(context, folderId).files
    }

    private fun parseDriveFiles(json: String): List<FileItem> {
        val files = mutableListOf<FileItem>()
        val jsonObj = JSONObject(json)
        val filesArray = jsonObj.optJSONArray("files") ?: return emptyList()

        for (i in 0 until filesArray.length()) {
            val file = filesArray.getJSONObject(i)
            val id = file.getString("id")
            val name = file.getString("name")
            val mimeType = file.optString("mimeType", "")
            val size = file.optLong("size", 0)
            val modifiedTime = file.optString("modifiedTime", "")
            val isFolder = mimeType == "application/vnd.google-apps.folder"

            val ext = name.substringAfterLast('.', "")
            val fileType = when {
                isFolder -> FileType.FOLDER
                mimeType.startsWith("image/") -> FileType.IMAGE
                mimeType.startsWith("video/") -> FileType.VIDEO
                mimeType.startsWith("audio/") -> FileType.AUDIO
                mimeType == "application/pdf" -> FileType.PDF
                mimeType.contains("document") || mimeType.contains("word") -> FileType.DOCUMENT
                mimeType.contains("spreadsheet") || mimeType.contains("excel") -> FileType.SPREADSHEET
                mimeType.contains("presentation") || mimeType.contains("powerpoint") -> FileType.PRESENTATION
                mimeType.contains("zip") || mimeType.contains("archive") || mimeType.contains("compressed") -> FileType.ARCHIVE
                else -> getFileType(ext)
            }

            val lastMod = try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    .parse(modifiedTime.take(19))?.time ?: System.currentTimeMillis()
            } catch (e: Exception) { System.currentTimeMillis() }

            files.add(
                FileItem(
                    name = name,
                    path = "gdrive://$id",
                    size = size,
                    lastModified = lastMod,
                    type = fileType,
                    isDirectory = isFolder,
                    folderColor = if (isFolder) FolderColor.BLUE else FolderColor.BLUE,
                    itemCount = 0,
                    extension = if (isFolder) "" else ext,
                    isDownloaded = false
                )
            )
        }
        return files
    }
}
