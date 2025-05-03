package fr.loicnogier.mystreamlogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MostStreamedViewModel(trackHistoryDao: TrackHistoryDao) : ViewModel() {

    val mostStreamedTracks: StateFlow<List<TrackStreamCount>> = trackHistoryDao.getMostStreamedTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
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