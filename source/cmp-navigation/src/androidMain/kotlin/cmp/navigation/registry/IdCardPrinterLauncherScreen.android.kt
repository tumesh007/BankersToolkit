package cmp.navigation.registry

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.idcardprinter.app.IdCardMainActivity

@Composable
actual fun IdCardPrinterLauncherScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        val intent = Intent(context, IdCardMainActivity::class.java)
        launcher.launch(intent)
    }
}
