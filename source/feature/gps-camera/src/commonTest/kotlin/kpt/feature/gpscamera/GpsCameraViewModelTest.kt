package kpt.feature.gpscamera

import kpt.core.data.visit.VisitRepository
import kpt.core.model.visit.PhotoCategory
import kpt.core.model.visit.Visit
import kpt.core.model.visit.VisitPhoto
import kpt.feature.gpscamera.ui.GpsCameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GpsCameraViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fakeRepo = object : VisitRepository {
        override fun getAllVisits(): Flow<List<Visit>> = emptyFlow()
        override suspend fun getVisitById(id: String): Visit? = null
        override suspend fun saveVisit(visit: Visit) = Unit
        override suspend fun deleteVisit(id: String) = Unit
        override suspend fun addPhotoToVisit(photo: VisitPhoto) = Unit
        override suspend fun deletePhoto(photoId: String) = Unit
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialCameraFacingIsBack() = runTest {
        val vm = GpsCameraViewModel(fakeRepo)
        assertFalse(vm.uiState.value.isFrontCamera, "Default camera facing should be back camera")
    }

    @Test
    fun testToggleCameraFlipsFacing() = runTest {
        val vm = GpsCameraViewModel(fakeRepo)
        assertFalse(vm.uiState.value.isFrontCamera)

        vm.toggleCamera()
        assertTrue(vm.uiState.value.isFrontCamera, "Toggling camera should switch to front camera")

        vm.toggleCamera()
        assertFalse(vm.uiState.value.isFrontCamera, "Toggling camera again should switch back to back camera")
    }

    @Test
    fun testSwitchCameraDirect() = runTest {
        val vm = GpsCameraViewModel(fakeRepo)
        vm.switchCamera(useFront = true)
        assertTrue(vm.uiState.value.isFrontCamera)

        vm.switchCamera(useFront = false)
        assertFalse(vm.uiState.value.isFrontCamera)
    }

    @Test
    fun testToggleTorchAndGrid() = runTest {
        val vm = GpsCameraViewModel(fakeRepo)
        assertFalse(vm.uiState.value.isTorchOn)
        assertTrue(vm.uiState.value.showGrid)

        vm.toggleTorch()
        assertTrue(vm.uiState.value.isTorchOn)

        vm.toggleGrid()
        assertFalse(vm.uiState.value.showGrid)
    }

    @Test
    fun testSetCaseAndOfficer() = runTest {
        val vm = GpsCameraViewModel(fakeRepo)
        vm.setCaseAndOfficer(caseNumber = "CASE-101", officerName = "Rahul Sharma", officerId = "EMP-555")

        assertEquals("CASE-101", vm.uiState.value.caseNumber)
        assertEquals("Rahul Sharma", vm.uiState.value.officerName)
        assertEquals("EMP-555", vm.uiState.value.officerId)
    }

    @Test
    fun testUpdateCategoryAndCaption() = runTest {
        val vm = GpsCameraViewModel(fakeRepo)
        vm.updateCategory(PhotoCategory.STOCK)
        assertEquals(PhotoCategory.STOCK, vm.uiState.value.selectedCategory)

        vm.updateCaption("Factory floor inventory")
        assertEquals("Factory floor inventory", vm.uiState.value.caption)
    }
}
