package kpt.feature.gpscamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kpt.feature.gpscamera.ui.GpsCameraScreen
import kpt.feature.gpscamera.ui.GpsCameraViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class GpsVerificationCameraActivity : ComponentActivity() {

    private val viewModel: GpsCameraViewModel by viewModel()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (!cameraGranted || !locationGranted) {
            Toast.makeText(
                this,
                "Camera & Fine Location permissions are required for tamper-proof verification.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GpsCameraPlatform.setActivityProvider { this }
        checkAndRequestPermissions()

        val visitId = intent.getStringExtra("EXTRA_VISIT_ID")
        val caseNumber = intent.getStringExtra("EXTRA_CASE_NUMBER") ?: ""
        val officerName = intent.getStringExtra("EXTRA_OFFICER_NAME") ?: ""
        val officerId = intent.getStringExtra("EXTRA_OFFICER_ID") ?: ""

        if (!visitId.isNullOrBlank()) {
            viewModel.setVisitId(visitId)
        }
        if (caseNumber.isNotBlank() || officerName.isNotBlank() || officerId.isNotBlank()) {
            viewModel.setCaseAndOfficer(caseNumber, officerName, officerId)
        }

        setContent {
            GpsCameraScreen(
                viewModel = viewModel,
                onPhotoCaptured = { photo ->
                    setResult(RESULT_OK, intent.putExtra("CAPTURED_PHOTO_PATH", photo.originalPath))
                    finish()
                },
                onNavigateBack = { finish() }
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val required = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
