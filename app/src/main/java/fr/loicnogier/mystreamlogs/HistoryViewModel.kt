package fr.loicnogier.mystreamlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class HistoryViewModel(private val trackHistoryDao: TrackHistoryDao) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<TrackHistory>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                trackHistoryDao.getAllHistory()
            } else {
                trackHistoryDao.searchHistory(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

class HistoryViewModelFactory(private val dao: TrackHistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}