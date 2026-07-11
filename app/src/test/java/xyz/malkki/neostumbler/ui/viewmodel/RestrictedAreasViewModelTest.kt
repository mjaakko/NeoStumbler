package xyz.malkki.neostumbler.ui.viewmodel

import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xyz.malkki.neostumbler.data.restrictedarea.RestrictedArea
import xyz.malkki.neostumbler.data.restrictedarea.RestrictedAreaManager
import xyz.malkki.neostumbler.geography.Circle
import xyz.malkki.neostumbler.geography.LatLng

class RestrictedAreasViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var _restrictedAreas: MutableStateFlow<List<RestrictedArea>>
    private lateinit var restrictedAreaManager: RestrictedAreaManager
    private lateinit var viewModel: RestrictedAreasViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        _restrictedAreas = MutableStateFlow(emptyList())

        restrictedAreaManager =
            object : RestrictedAreaManager {

                override val restrictedAreas: Flow<List<RestrictedArea>>
                    get() = _restrictedAreas

                override suspend fun deleteRestrictedArea(vararg ids: UUID) {
                    _restrictedAreas.update { it.filter { area -> area.id !in ids } }
                }

                override suspend fun addRestrictedArea(restrictedArea: RestrictedArea) {
                    _restrictedAreas.update { it.plus(restrictedArea) }
                }
            }

        viewModel = RestrictedAreasViewModel(restrictedAreaManager)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Initial state is correct`() =
        runTest(testDispatcher) {
            assertTrue(viewModel.showExplanation.value)
            assertEquals(emptySet<UUID>(), viewModel.selectedRestrictedAreas.value)

            val (center, zoom) = viewModel.mapViewport.value
            assertEquals(LatLng.ORIGIN, center)
            assertEquals(12.0, zoom, 0.0)
        }

    @Test
    fun `closeExplanation sets showExplanation to false`() =
        runTest(testDispatcher) {
            viewModel.closeExplanation()
            assertFalse(viewModel.showExplanation.value)
        }

    @Test
    fun `select and deselect restricted area updates state correctly`() =
        runTest(testDispatcher) {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()

            viewModel.selectRestrictedAreaById(id1)
            viewModel.selectRestrictedAreaById(id2)
            assertEquals(setOf(id1, id2), viewModel.selectedRestrictedAreas.value)

            viewModel.deselectRestrictedAreaById(id1)
            assertEquals(setOf(id2), viewModel.selectedRestrictedAreas.value)
        }

    @Test
    fun `addRestrictedArea creates a new restricted area`() = runTest {
        val circle =
            Circle(center = LatLng(latitude = 59.827273, longitude = 22.968209), radius = 500.0)

        viewModel.addRestrictedArea(circle)
        advanceUntilIdle()

        val restrictedAreas = _restrictedAreas.value
        assertEquals(1, restrictedAreas.size)
        assertEquals(circle, restrictedAreas.first().circle)
    }

    @Test
    fun `deleteSelectedRestrictedAreas clears selection and deletes the selected area`() = runTest {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        _restrictedAreas.value =
            listOf(
                RestrictedArea(
                    id = id1,
                    circle =
                        Circle(
                            center = LatLng(latitude = 59.827273, longitude = 22.968209),
                            radius = 500.0,
                        ),
                ),
                RestrictedArea(
                    id = id2,
                    circle =
                        Circle(
                            center = LatLng(latitude = 59.827273, longitude = 22.968209),
                            radius = 500.0,
                        ),
                ),
            )

        viewModel.selectRestrictedAreaById(id1)
        viewModel.selectRestrictedAreaById(id2)

        viewModel.deleteSelectedRestrictedAreas()
        advanceUntilIdle()

        assertEquals(emptySet<UUID>(), viewModel.selectedRestrictedAreas.value)

        assertTrue(_restrictedAreas.value.isEmpty())
    }
}
