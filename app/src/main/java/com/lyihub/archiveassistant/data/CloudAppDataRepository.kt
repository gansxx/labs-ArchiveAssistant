package com.lyihub.archiveassistant.data

import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Snapshot-compatible cloud implementation; PostgreSQL credentials never leave the server. */
class CloudAppDataRepository(
  baseUrl: String,
  workspaceId: String,
  private val apiKey: String,
  private val client: OkHttpClient = OkHttpClient(),
) : AppDataSource {
  private val snapshotUrl =
    "${baseUrl.trimEnd('/')}/v1/workspaces/${encodePathSegment(workspaceId)}/snapshot"

  override suspend fun loadSnapshot(): AppDataSnapshot =
    withContext(Dispatchers.IO) {
      val request = authenticatedRequest().get().build()
      client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          throw IOException("Cloud snapshot load failed with HTTP ${response.code}")
        }
        val json = JSONObject(body)
        AppDataSnapshot(
          topics =
            AppDataPreferences.DecodeResult.Valid(
              AppDataPreferences.parseTopics(json.getJSONArray("topics").toString())
            ),
          items =
            AppDataPreferences.DecodeResult.Valid(
              AppDataPreferences.parseItems(json.getJSONArray("items").toString())
            ),
        )
      }
    }

  override suspend fun saveAll(topics: List<Topic>, items: List<KnowledgeItem>) {
    withContext(Dispatchers.IO) {
      val body =
        JSONObject()
          .put("topics", org.json.JSONArray(AppDataPreferences.encodeTopicsJson(topics)))
          .put("items", org.json.JSONArray(AppDataPreferences.encodeItemsJson(items)))
          .toString()
          .toRequestBody(JsonMediaType)
      val request = authenticatedRequest().put(body).build()
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          throw IOException("Cloud snapshot save failed with HTTP ${response.code}")
        }
      }
    }
  }

  private fun authenticatedRequest(): Request.Builder =
    Request.Builder().url(snapshotUrl).header("Authorization", "Bearer $apiKey")

  private companion object {
    val JsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun encodePathSegment(value: String): String {
      require(value.matches(Regex("[A-Za-z0-9._-]{1,128}"))) { "Invalid workspace id" }
      return value
    }
  }
}
