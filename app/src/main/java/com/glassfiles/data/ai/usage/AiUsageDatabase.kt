package com.glassfiles.data.ai.usage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.roundToInt

/** SQLite persistence for robust usage tracking and tokenizer calibration. */
class AiUsageDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS message_usage (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message_id TEXT NOT NULL,
                chat_id TEXT NOT NULL,
                provider TEXT NOT NULL,
                model TEXT NOT NULL,
                estimated_input_tokens INTEGER,
                estimated_output_tokens INTEGER,
                estimated_cost REAL,
                reported_input_tokens INTEGER,
                reported_output_tokens INTEGER,
                reported_cost REAL,
                is_estimated INTEGER DEFAULT 1,
                has_cache_hit INTEGER DEFAULT 0,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_usage_chat ON message_usage(chat_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_usage_provider_date ON message_usage(provider, timestamp)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS provider_calibration (
                provider TEXT NOT NULL,
                model TEXT NOT NULL,
                our_estimate_total INTEGER,
                api_actual_total INTEGER,
                sample_count INTEGER,
                factor REAL,
                last_updated INTEGER,
                PRIMARY KEY (provider, model)
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) onCreate(db)
    }

    fun insertUsage(record: MessageUsageRecord) {
        writableDatabase.insert(
            "message_usage",
            null,
            ContentValues().apply {
                put("message_id", record.messageId)
                put("chat_id", record.chatId)
                put("provider", record.provider)
                put("model", record.model)
                put("estimated_input_tokens", record.estimatedInputTokens)
                put("estimated_output_tokens", record.estimatedOutputTokens)
                put("estimated_cost", record.estimatedCost)
                put("reported_input_tokens", record.reportedInputTokens)
                put("reported_output_tokens", record.reportedOutputTokens)
                put("reported_cost", record.reportedCost)
                put("is_estimated", if (record.isEstimated) 1 else 0)
                put("has_cache_hit", if (record.hasCacheHit) 1 else 0)
                put("timestamp", record.timestamp)
            },
        )
    }

    fun getCalibration(provider: String, model: String): ProviderCalibration? {
        readableDatabase.query(
            "provider_calibration",
            arrayOf("provider", "model", "our_estimate_total", "api_actual_total", "sample_count", "factor", "last_updated"),
            "provider = ? AND model = ?",
            arrayOf(provider, model),
            null,
            null,
            null,
            "1",
        ).use { c ->
            if (!c.moveToFirst()) return null
            return ProviderCalibration(
                provider = c.getString(0),
                model = c.getString(1),
                ourEstimateTotal = c.getInt(2),
                apiActualTotal = c.getInt(3),
                sampleCount = c.getInt(4),
                factor = c.getDouble(5),
                lastUpdated = c.getLong(6),
            )
        }
    }

    fun listCalibrations(): List<ProviderCalibration> {
        readableDatabase.query(
            "provider_calibration",
            arrayOf("provider", "model", "our_estimate_total", "api_actual_total", "sample_count", "factor", "last_updated"),
            null,
            null,
            null,
            null,
            "provider ASC, model ASC",
        ).use { c ->
            val out = mutableListOf<ProviderCalibration>()
            while (c.moveToNext()) {
                out += ProviderCalibration(
                    provider = c.getString(0),
                    model = c.getString(1),
                    ourEstimateTotal = c.getInt(2),
                    apiActualTotal = c.getInt(3),
                    sampleCount = c.getInt(4),
                    factor = c.getDouble(5),
                    lastUpdated = c.getLong(6),
                )
            }
            return out
        }
    }

    fun upsertCalibration(provider: String, model: String, estimatedTotal: Int, actualTotal: Int) {
        if (estimatedTotal <= 0 || actualTotal <= 0) return
        val now = System.currentTimeMillis()
        val current = getCalibration(provider, model)
        val sampleFactor = actualTotal.toDouble() / estimatedTotal.toDouble()
        val nextFactor = if (current == null) sampleFactor else current.factor * 0.8 + sampleFactor * 0.2
        val nextSamples = (current?.sampleCount ?: 0) + 1
        writableDatabase.insertWithOnConflict(
            "provider_calibration",
            null,
            ContentValues().apply {
                put("provider", provider)
                put("model", model)
                put("our_estimate_total", estimatedTotal)
                put("api_actual_total", actualTotal)
                put("sample_count", nextSamples)
                put("factor", nextFactor)
                put("last_updated", now)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun calibratedTokenCount(provider: String, model: String, rawTokens: Int): Int {
        val factor = getCalibration(provider, model)?.factor ?: 1.0
        return (rawTokens * factor).roundToInt().coerceAtLeast(0)
    }

    companion object {
        private const val DB_NAME = "ai_usage.db"
        private const val DB_VERSION = 1

        @Volatile private var INSTANCE: AiUsageDatabase? = null

        fun get(context: Context): AiUsageDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AiUsageDatabase(context).also { INSTANCE = it }
        }
    }
}

data class MessageUsageRecord(
    val messageId: String,
    val chatId: String,
    val provider: String,
    val model: String,
    val estimatedInputTokens: Int?,
    val estimatedOutputTokens: Int?,
    val estimatedCost: Double?,
    val reportedInputTokens: Int?,
    val reportedOutputTokens: Int?,
    val reportedCost: Double?,
    val isEstimated: Boolean,
    val hasCacheHit: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

data class ProviderCalibration(
    val provider: String,
    val model: String,
    val ourEstimateTotal: Int,
    val apiActualTotal: Int,
    val sampleCount: Int,
    val factor: Double,
    val lastUpdated: Long,
)
