package com.example.contestreminder

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

data class Contest(
    val name: String,
    val url: String,
    val site: String,       // "Codeforces" | "CodeChef" | "LeetCode"
    val startTimeMillis: Long
)

object ContestFetcher {

    // kontests.net aggregates Codeforces / CodeChef / LeetCode / AtCoder / HackerRank etc.
    // "/all" returns every upcoming + running contest across sites in one call.
    private const val API_URL = "https://kontests.net/api/v1/all"

    private val trackedSites = setOf("Codeforces", "CodeChef", "LeetCode")

    /**
     * Fetches all upcoming contests and returns only the ones we care about,
     * sorted soonest-first. Safe to call from a background thread only.
     */
    fun fetchUpcoming(): List<Contest> {
        val raw = httpGet(API_URL) ?: return emptyList()
        val arr = JSONArray(raw)
        val result = mutableListOf<Contest>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val site = obj.optString("site")
            if (site !in trackedSites) continue

            val status = obj.optString("status")
            if (status != "BEFORE") continue // only contests that haven't started yet

            val startTimeStr = obj.optString("start_time")
            val startMillis = try {
                Instant.parse(startTimeStr).toEpochMilli()
            } catch (e: Exception) {
                continue
            }

            // skip anything more than 10 days out - no point scheduling alarms that far ahead
            if (startMillis - System.currentTimeMillis() > 10L * 24 * 60 * 60 * 1000) continue

            result.add(
                Contest(
                    name = obj.optString("name"),
                    url = obj.optString("url"),
                    site = site,
                    startTimeMillis = startMillis
                )
            )
        }
        return result.sortedBy { it.startTimeMillis }
    }

    private fun httpGet(urlStr: String): String? {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            text
        } catch (e: Exception) {
            null
        }
    }
}
