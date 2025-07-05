package fr.loicnogier.mystreamlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(private val trackHistoryDao: TrackHistoryDao) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedYear = MutableStateFlow<String?>(null)
    val selectedYear: StateFlow<String?> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth: StateFlow<String?> = _selectedMonth.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedYear(year: String?) {
        _selectedYear.value = year
    }

    fun setSelectedMonth(month: String?) {
        _selectedMonth.value = month
    }

    // Combine year and month into a format that the DAO expects: "YYYY-MM"
    private val formattedYearMonth = combine(_selectedYear, _selectedMonth) { year, month ->
        when {
            year.isNullOrBlank() && month.isNullOrBlank() -> null
            year.isNullOrBlank() -> {
                // If only month is selected, use current year
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                "$currentYear-$month"
            }
            month.isNullOrBlank() -> null
            else -> "$year-$month"
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<TrackHistory>> = combine(searchQuery, formattedYearMonth) { query, yearMonth ->
        Pair(query, yearMonth)
    }.flatMapLatest { (query, yearMonth) ->
        when {
            query.isBlank() && yearMonth.isNullOrBlank() -> trackHistoryDao.getAllHistory()
            query.isBlank() -> trackHistoryDao.getHistoryByMonth(yearMonth!!)
            yearMonth.isNullOrBlank() -> trackHistoryDao.searchHistory(query)
            else -> trackHistoryDao.searchHistoryByMonth(query, yearMonth)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteTrack(id: Long) {
        viewModelScope.launch {
            trackHistoryDao.deleteById(id)
        }
    }
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
