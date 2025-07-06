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

    private val _selectedPlatform = MutableStateFlow<String?>(null)
    val selectedPlatform: StateFlow<String?> = _selectedPlatform.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedYear(year: String?) {
        _selectedYear.value = year
    }

    fun setSelectedMonth(month: String?) {
        _selectedMonth.value = month
    }

    fun setSelectedPlatform(platform: String?) {
        _selectedPlatform.value = platform
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
    val history: StateFlow<List<TrackHistory>> = combine(
        searchQuery, 
        formattedYearMonth,
        selectedPlatform
    ) { query, yearMonth, platform ->
        Triple(query, yearMonth, platform)
    }.flatMapLatest { (query, yearMonth, platform) ->
        when {
            // No filters
            query.isBlank() && yearMonth.isNullOrBlank() && platform.isNullOrBlank() -> 
                trackHistoryDao.getAllHistory()

            // Only platform filter
            query.isBlank() && yearMonth.isNullOrBlank() && !platform.isNullOrBlank() -> 
                trackHistoryDao.getHistoryByPlatform(platform)

            // Only month filter
            query.isBlank() && !yearMonth.isNullOrBlank() && platform.isNullOrBlank() -> 
                trackHistoryDao.getHistoryByMonth(yearMonth)

            // Month and platform filters
            query.isBlank() && !yearMonth.isNullOrBlank() && !platform.isNullOrBlank() -> 
                trackHistoryDao.getHistoryByMonthAndPlatform(yearMonth, platform)

            // Only search query
            !query.isBlank() && yearMonth.isNullOrBlank() && platform.isNullOrBlank() -> 
                trackHistoryDao.searchHistory(query)

            // Search query and platform filter
            !query.isBlank() && yearMonth.isNullOrBlank() && !platform.isNullOrBlank() -> 
                trackHistoryDao.searchHistoryByPlatform(query, platform)

            // Search query and month filter
            !query.isBlank() && !yearMonth.isNullOrBlank() && platform.isNullOrBlank() -> 
                trackHistoryDao.searchHistoryByMonth(query, yearMonth)

            // All filters (search query, month, and platform)
            else -> 
                trackHistoryDao.searchHistoryByMonthAndPlatform(query, yearMonth!!, platform!!)
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
