package com.example.services

import com.example.BuildConfig

class GeminiKhataAssistant {

    fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun isAiEnabled(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateKhataSummary(
        totalYouWillGet: Double,
        totalYouWillGive: Double,
        customerCount: Int
    ): String {
        if (!isAiEnabled()) {
            return "AI Insights ready. Connect your Gemini API Key in secrets to unlock automated smart credit summaries."
        }
        val netBalance = totalYouWillGet - totalYouWillGive
        return "Smart Insight: Your net ledger balance is PKR ${"%.2f".format(netBalance)} across $customerCount customer accounts. Total receivables: PKR ${"%.2f".format(totalYouWillGet)}, payables: PKR ${"%.2f".format(totalYouWillGive)}."
    }
}

