package com.bishal.adaptivecamera.ui.lut

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bishal.adaptivecamera.lut.model.LutDefinition
import com.bishal.adaptivecamera.lut.repository.LutFilter
import com.bishal.adaptivecamera.lut.repository.LutItem
import com.bishal.adaptivecamera.lut.repository.LutRepository
import com.bishal.adaptivecamera.lut.repository.LutRepositoryImpl
import com.bishal.adaptivecamera.lut.repository.LutSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LutUiState(
    val luts: List<LutItem> = emptyList(),
    val filteredLuts: List<LutItem> = emptyList(),
    val selectedLutId: String? = null,
    val selectedLut: LutItem? = null,
    val searchQuery: String = "",
    val selectedFilter: LutFilter = LutFilter.ALL,
    val selectedSort: LutSort = LutSort.NAME_ASC,
    val selectedLutForDetail: LutItem? = null,
    val isLoading: Boolean = false,
    val toastMessage: String? = null
)

class LutViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: LutRepository = LutRepositoryImpl(application.applicationContext)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LutUiState())
    val uiState: StateFlow<LutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allLutsFlow,
                repository.selectedLutIdFlow
            ) { allLuts, selectedId ->
                Pair(allLuts, selectedId)
            }.collect { (allLuts, selectedId) ->
                _uiState.update { current ->
                    val filtered = applyFilteringAndSorting(
                        luts = allLuts,
                        query = current.searchQuery,
                        filter = current.selectedFilter,
                        sort = current.selectedSort
                    )
                    current.copy(
                        luts = allLuts,
                        filteredLuts = filtered,
                        selectedLutId = selectedId,
                        selectedLut = allLuts.firstOrNull { it.id == selectedId }
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = applyFilteringAndSorting(current.luts, query, current.selectedFilter, current.selectedSort)
            current.copy(searchQuery = query, filteredLuts = filtered)
        }
    }

    fun setFilter(filter: LutFilter) {
        _uiState.update { current ->
            val filtered = applyFilteringAndSorting(current.luts, current.searchQuery, filter, current.selectedSort)
            current.copy(selectedFilter = filter, filteredLuts = filtered)
        }
    }

    fun setSort(sort: LutSort) {
        _uiState.update { current ->
            val filtered = applyFilteringAndSorting(current.luts, current.searchQuery, current.selectedFilter, sort)
            current.copy(selectedSort = sort, filteredLuts = filtered)
        }
    }

    fun selectLut(id: String?) {
        viewModelScope.launch {
            repository.selectLut(id)
            val name = _uiState.value.luts.firstOrNull { it.id == id }?.name ?: "None"
            _uiState.update { it.copy(toastMessage = if (id != null) "Selected: $name" else "Reset to default Rec.709") }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun importLutFromUri(uri: Uri) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.importLutFromUri(uri)
            _uiState.update { current ->
                if (result.isSuccess) {
                    val imported = result.getOrNull()
                    current.copy(
                        isLoading = false,
                        toastMessage = "Successfully imported: ${imported?.name}"
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Import failed"
                    current.copy(
                        isLoading = false,
                        toastMessage = "Error: $error"
                    )
                }
            }
        }
    }

    fun deleteLut(id: String) {
        viewModelScope.launch {
            val result = repository.deleteLut(id)
            _uiState.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        selectedLutForDetail = null,
                        toastMessage = "Deleted LUT successfully"
                    )
                } else {
                    current.copy(
                        toastMessage = "Cannot delete: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun openDetail(lut: LutItem) {
        _uiState.update { it.copy(selectedLutForDetail = lut) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedLutForDetail = null) }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    suspend fun loadLutDefinition(id: String): LutDefinition? {
        return repository.loadLutDefinition(id)
    }

    private fun applyFilteringAndSorting(
        luts: List<LutItem>,
        query: String,
        filter: LutFilter,
        sort: LutSort
    ): List<LutItem> {
        var result = luts

        // 1. Text Search
        if (query.isNotBlank()) {
            val lower = query.trim().lowercase()
            result = result.filter {
                it.name.lowercase().contains(lower) ||
                it.description.lowercase().contains(lower) ||
                (it.originalFileName?.lowercase()?.contains(lower) == true)
            }
        }

        // 2. Filter Category
        result = when (filter) {
            LutFilter.ALL -> result
            LutFilter.BUILT_IN -> result.filter { it.isBuiltIn }
            LutFilter.IMPORTED -> result.filter { !it.isBuiltIn }
            LutFilter.FAVORITES -> result.filter { it.isFavorite }
        }

        // 3. Sorting
        result = when (sort) {
            LutSort.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            LutSort.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
            LutSort.DATE_DESC -> result.sortedByDescending { it.dateAddedMs }
            LutSort.DATE_ASC -> result.sortedBy { it.dateAddedMs }
        }

        return result
    }
}
