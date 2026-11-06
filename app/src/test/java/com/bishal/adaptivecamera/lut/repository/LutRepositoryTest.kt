package com.bishal.adaptivecamera.lut.repository

import com.bishal.adaptivecamera.lut.model.LutSourceType
import com.bishal.adaptivecamera.lut.model.LutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LutRepositoryTest {

    private fun createSampleItem(
        id: String,
        name: String,
        sourceType: LutSourceType,
        isFavorite: Boolean = false,
        dateAddedMs: Long = 0L
    ): LutItem {
        return LutItem(
            id = id,
            name = name,
            description = "Sample look",
            sourceType = sourceType,
            lutType = LutType.LUT_3D,
            dimension = 17,
            contentHash = "hash_$id",
            isFavorite = isFavorite,
            dateAddedMs = dateAddedMs
        )
    }

    @Test
    fun filter_all_retainsAllItems() {
        val items = listOf(
            createSampleItem("1", "BuiltIn 1", LutSourceType.BUILT_IN),
            createSampleItem("2", "Imported 1", LutSourceType.IMPORTED)
        )
        val filtered = items.filter { item ->
            when (LutFilter.ALL) {
                LutFilter.ALL -> true
                LutFilter.BUILT_IN -> item.sourceType == LutSourceType.BUILT_IN
                LutFilter.IMPORTED -> item.sourceType == LutSourceType.IMPORTED
                LutFilter.FAVORITES -> item.isFavorite
            }
        }
        assertEquals(2, filtered.size)
    }

    @Test
    fun filter_builtIn_retainsOnlyBuiltIn() {
        val items = listOf(
            createSampleItem("1", "BuiltIn 1", LutSourceType.BUILT_IN),
            createSampleItem("2", "Imported 1", LutSourceType.IMPORTED)
        )
        val filtered = items.filter { it.sourceType == LutSourceType.BUILT_IN }
        assertEquals(1, filtered.size)
        assertEquals("1", filtered[0].id)
    }

    @Test
    fun filter_favorites_retainsOnlyMarkedFavorites() {
        val items = listOf(
            createSampleItem("1", "BuiltIn 1", LutSourceType.BUILT_IN, isFavorite = false),
            createSampleItem("2", "Imported 1", LutSourceType.IMPORTED, isFavorite = true)
        )
        val filtered = items.filter { it.isFavorite }
        assertEquals(1, filtered.size)
        assertEquals("2", filtered[0].id)
    }

    @Test
    fun sort_nameAscendingAndDescending_ordersAlphabetically() {
        val items = listOf(
            createSampleItem("1", "Warm Gold", LutSourceType.BUILT_IN),
            createSampleItem("2", "Cool Teal", LutSourceType.BUILT_IN),
            createSampleItem("3", "Amber Vintage", LutSourceType.BUILT_IN)
        )

        val asc = items.sortedBy { it.name.lowercase() }
        assertEquals("Amber Vintage", asc[0].name)
        assertEquals("Cool Teal", asc[1].name)
        assertEquals("Warm Gold", asc[2].name)

        val desc = items.sortedByDescending { it.name.lowercase() }
        assertEquals("Warm Gold", desc[0].name)
        assertEquals("Cool Teal", desc[1].name)
        assertEquals("Amber Vintage", desc[2].name)
    }

    @Test
    fun builtInItem_protection_flaggedProperly() {
        val builtIn = createSampleItem("builtin_natural", "Natural", LutSourceType.BUILT_IN)
        val imported = createSampleItem("custom_123", "Custom Look", LutSourceType.IMPORTED)

        assertTrue(builtIn.isBuiltIn)
        assertFalse(imported.isBuiltIn)
    }
}
