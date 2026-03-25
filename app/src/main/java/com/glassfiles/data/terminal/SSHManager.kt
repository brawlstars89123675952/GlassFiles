package com.glassfiles.data.terminal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SSHConnection(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val user: String,
    val password: String = "",
    val keyPath: String = "",
    val useKey: Boolean = false
) {
    fun toCommand(): String {
        val portFlag = if (port != 22) "-p $port " else ""
        return if (useKey && keyPath.isNotEmpty()) {
            "ssh ${portFlag}-i $keyPath $user@$host"
        } else {
            "ssh ${portFlag}$user@$host"
        }
    }
}

class SSHManager(context: Context) {
    private val file = File(context.filesDir, "ssh_connections.json")

    fun getAll(): List<SSHConnection> {
        val arr = try { JSONArray(file.readText()) } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SSHConnection(
                id = o.optString("id", System.currentTimeMillis().toString()),
                name = o.getString("name"),
                host = o.getString("host"),
                port = o.optInt("port", 22),
                user = o.getString("user"),
                password = o.optString("password", ""),
                keyPath = o.optString("keyPath", ""),
                useKey = o.optBoolean("useKey", false)
            )
        }
    }

    fun save(conn: SSHConnection) {
        val all = getAll().toMutableList()
        val idx = all.indexOfFirst { it.id == conn.id }
        if (idx >= 0) all[idx] = conn else all.add(conn)
        writeAll(all)
    }

    fun delete(id: String) {
        writeAll(getAll().filter { it.id != id })
    }

    private fun writeAll(list: List<SSHConnection>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("host", c.host)
                put("port", c.port); put("user", c.user); put("password", c.password)
                put("keyPath", c.keyPath); put("useKey", c.useKey)
            })
        }
        file.writeText(arr.toString(2))
    }
}
