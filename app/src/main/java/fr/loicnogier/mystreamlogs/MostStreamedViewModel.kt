package fr.loicnogier.mystreamlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MostStreamedViewModel(private val trackHistoryDao: TrackHistoryDao) : ViewModel() {

    private val _selectedYear = MutableStateFlow<String?>(null)
    val selectedYear: StateFlow<String?> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth: StateFlow<String?> = _selectedMonth.asStateFlow()

    private val _selectedPlatform = MutableStateFlow<String?>(null)
    val selectedPlatform: StateFlow<String?> = _selectedPlatform.asStateFlow()

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
    val mostStreamedTracks: StateFlow<List<TrackStreamCount>> = combine(
        formattedYearMonth,
        selectedPlatform
    ) { yearMonth, platform ->
        Pair(yearMonth, platform)
    }.flatMapLatest { (yearMonth, platform) ->
        when {
            // No filters
            yearMonth.isNullOrBlank() && platform.isNullOrBlank() ->
                trackHistoryDao.getMostStreamedTracks()

            // Only platform filter
            yearMonth.isNullOrBlank() && !platform.isNullOrBlank() ->
                trackHistoryDao.getMostStreamedTracksByPlatform(platform)

            // Only month filter
            !yearMonth.isNullOrBlank() && platform.isNullOrBlank() ->
                trackHistoryDao.getMostStreamedTracksByMonth(yearMonth)

            // Both month and platform filters
            else ->
                trackHistoryDao.getMostStreamedTracksByMonthAndPlatform(yearMonth!!, platform!!)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteTrack(trackTitle: String, artistName: String) {
        viewModelScope.launch {
            trackHistoryDao.deleteByTrackAndArtist(trackTitle, artistName)
        }
    }
}

class MostStreamedViewModelFactory(private val dao: TrackHistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MostStreamedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MostStreamedViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
