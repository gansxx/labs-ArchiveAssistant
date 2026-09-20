package com.lyihub.archiveassistant.data

import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SwitchableAppDataSourceTest {
  @Test
  fun defaultsToLocalAndCanSwitchWithoutDeletingEitherBackend() = runTest {
    val local = FakeSource("local")
    val cloud = FakeSource("cloud")
    val source = SwitchableAppDataSource(local, cloud)

    source.saveAll(emptyList(), emptyList())
    source.switchTo(StorageBackend.CLOUD)
    source.saveAll(emptyList(), emptyList())

    assertEquals(StorageBackend.CLOUD, source.backend)
    assertEquals(1, local.saveCount)
    assertEquals(1, cloud.saveCount)
  }

  @Test
  fun cloudSelectionRequiresCloudConfiguration() {
    val source = SwitchableAppDataSource(FakeSource("local"), cloud = null)

    assertThrows(IllegalArgumentException::class.java) { source.switchTo(StorageBackend.CLOUD) }
    assertEquals(StorageBackend.LOCAL, source.backend)
  }

  private class FakeSource(private val name: String) : AppDataSource {
    var saveCount = 0

    override suspend fun loadSnapshot(): AppDataSnapshot =
      AppDataSnapshot(
        AppDataPreferences.DecodeResult.Valid(emptyList<Topic>()),
        AppDataPreferences.DecodeResult.Valid(emptyList<KnowledgeItem>()),
      )

    override suspend fun saveAll(topics: List<Topic>, items: List<KnowledgeItem>) {
      saveCount += 1
    }

    override fun toString(): String = name
  }
}
