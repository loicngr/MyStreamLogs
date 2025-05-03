package fr.loicnogier.mystreamlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// ViewModel for the Most Streamed screen
class MostStreamedViewModel(trackHistoryDao: TrackHistoryDao) : ViewModel() {

    // Expose the flow of most streamed tracks from the DAO
    val mostStreamedTracks: StateFlow<List<TrackStreamCount>> = trackHistoryDao.getMostStreamedTracks()
        .stateIn(
            scope = viewModelScope,
            // Keep the data available for 5 seconds after the UI stops observing
            started = SharingStarted.WhileSubscribed(5000),
            // Initial value is an empty list
            initialValue = emptyList()
        )
}

// Factory to create the MostStreamedViewModel with the DAO dependency
class MostStreamedViewModelFactory(private val dao: TrackHistoryDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MostStreamedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MostStreamedViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}