package com.lyihub.archiveassistant.data

import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic

enum class StorageBackend {
  LOCAL,
  CLOUD;

  companion object {
    fun from(value: String): StorageBackend =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LOCAL
  }
}

/**
 * Stable repository boundary for selecting local DataStore or the cloud API.
 * Switching affects subsequent reads and writes and never deletes the inactive backend's data.
 */
class SwitchableAppDataSource(
  private val local: AppDataSource,
  private val cloud: AppDataSource?,
  initialBackend: StorageBackend = StorageBackend.LOCAL,
) : AppDataSource {
  @Volatile private var selectedBackend = initialBackend

  init {
    require(initialBackend != StorageBackend.CLOUD || cloud != null) {
      "Cloud storage is not configured"
    }
  }

  val backend: StorageBackend
    get() = selectedBackend

  fun switchTo(backend: StorageBackend) {
    require(backend != StorageBackend.CLOUD || cloud != null) {
      "Cloud storage is not configured"
    }
    selectedBackend = backend
  }

  override suspend fun loadSnapshot(): AppDataSnapshot? = selected().loadSnapshot()

  override suspend fun saveAll(topics: List<Topic>, items: List<KnowledgeItem>) =
    selected().saveAll(topics, items)

  private fun selected(): AppDataSource =
    if (selectedBackend == StorageBackend.CLOUD) {
      requireNotNull(cloud) { "Cloud storage is not configured" }
    } else {
      local
    }
}
