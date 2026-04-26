package xyz.malkki.neostumbler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.data.restrictedarea.RestrictedArea
import xyz.malkki.neostumbler.data.restrictedarea.RestrictedAreaManager
import xyz.malkki.neostumbler.geography.Circle
import xyz.malkki.neostumbler.geography.LatLng

private const val INITIAL_MAP_ZOOM = 12.0

private val INITIAL_MAP_VIEW = LatLng.ORIGIN to INITIAL_MAP_ZOOM

class RestrictedAreasViewModel(private val restrictedAreaManager: RestrictedAreaManager) :
    ViewModel() {
    private val _showExplanation = MutableStateFlow(true)
    val showExplanation = _showExplanation.asStateFlow()

    private val _mapViewport = MutableStateFlow(INITIAL_MAP_VIEW)
    val mapViewport = _mapViewport.asStateFlow()

    val restrictedAreas =
        restrictedAreaManager.restrictedAreas.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList(),
        )

    private val _selectedRestrictedAreas = MutableStateFlow<Set<UUID>>(emptySet())
    val selectedRestrictedAreas = _selectedRestrictedAreas.asStateFlow()

    init {
        viewModelScope.launch {
            restrictedAreas
                .mapNotNull { it.lastOrNull() }
                .collect { _mapViewport.update { (_, zoom) -> it.circle.center to zoom } }
        }
    }

    fun addRestrictedArea(circle: Circle) = viewModelScope.launch {
        restrictedAreaManager.addRestrictedArea(
            RestrictedArea(id = UUID.randomUUID(), circle = circle)
        )
    }

    fun selectRestrictedAreaById(id: UUID) {
        _selectedRestrictedAreas.update { selected -> selected + id }
    }

    fun deselectRestrictedAreaById(id: UUID) {
        _selectedRestrictedAreas.update { selected -> selected - id }
    }

    fun deleteSelectedRestrictedAreas() = viewModelScope.launch {
        val toDelete = _selectedRestrictedAreas.getAndUpdate { emptySet() }

        restrictedAreaManager.deleteRestrictedArea(*toDelete.toTypedArray())
    }

    fun setMapViewport(center: LatLng, zoom: Double) {
        _mapViewport.value = center to zoom
    }

    fun closeExplanation() {
        _showExplanation.value = false
    }
}
