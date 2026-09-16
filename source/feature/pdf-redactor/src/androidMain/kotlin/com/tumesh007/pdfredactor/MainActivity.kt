package com.tumesh007.pdfredactor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowBack
import kotlinx.coroutines.CompletableDeferred

class PdfRedactorActivity : ComponentActivity() {

    private val watermarkViewModel: WatermarkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RedactionEngine.initIfNeeded(applicationContext)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WatermarkAppScreen(
                        viewModel = watermarkViewModel,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkAppScreen(
    viewModel: WatermarkViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        RedactionEngine.initIfNeeded(context)
    }

    val uiState by viewModel.uiState.collectAsState()
    val employeeId by viewModel.employeeId.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    var pendingCreateDocumentDeferred by remember { mutableStateOf<CompletableDeferred<Uri?>?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        pendingCreateDocumentDeferred?.complete(uri)
        pendingCreateDocumentDeferred = null
    }

    val singleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFilesSelected(context, listOf(it)) }
    }

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onFilesSelected(context, uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Watermark Remover") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Advanced Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
        ) {
            when (val state = uiState) {
                is WatermarkUiState.Idle -> {
                    HomeScreenIdle(
                        employeeId = employeeId,
                        onEmployeeIdChange = { viewModel.updateEmployeeId(it) },
                        onSelectSingle = { singleFilePicker.launch(arrayOf("application/pdf")) },
                        onSelectMultiple = { multiFilePicker.launch(arrayOf("application/pdf")) }
                    )
                }
                is WatermarkUiState.FilesSelected -> {
                    FileSelectedScreen(
                        employeeId = employeeId,
                        onEmployeeIdChange = { viewModel.updateEmployeeId(it) },
                        fileNames = state.fileNames,
                        onStartRemoval = {
                            viewModel.processWatermarkRemoval(context) { defaultName ->
                                val deferred = CompletableDeferred<Uri?>()
                                pendingCreateDocumentDeferred = deferred
                                createDocumentLauncher.launch(defaultName)
                                deferred.await()
                            }
                        },
                        onCancel = { viewModel.reset() }
                    )
                }
                is WatermarkUiState.Processing -> {
                    ProcessingScreen(
                        currentFileName = state.currentFileName,
                        currentIndex = state.currentIndex,
                        totalFiles = state.totalFiles
                    )
                }
                is WatermarkUiState.Success -> {
                    ResultScreen(
                        summary = state.result,
                        onDone = { viewModel.reset() }
                    )
                }
                is WatermarkUiState.Error -> {
                    ErrorScreen(
                        message = state.message,
                        onRetry = { viewModel.reset() }
                    )
                }
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}

@Composable
fun HomeScreenIdle(
    employeeId: String,
    onEmployeeIdChange: (String) -> Unit,
    onSelectSingle: () -> Unit,
    onSelectMultiple: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PDF Watermark Remover",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Remove portal/download watermarks automatically from downloaded PDF documents.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = employeeId,
            onValueChange = onEmployeeIdChange,
            label = { Text("Employee ID (optional)") },
            placeholder = { Text("e.g. 5135265") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSelectSingle,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(52.dp)
        ) {
            Text("Select PDF", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onSelectMultiple,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(52.dp)
        ) {
            Text("Select Multiple PDFs", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(36.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Automatic Detection",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Works offline • Original PDF is preserved • Removes full watermark layer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun FileSelectedScreen(
    employeeId: String,
    onEmployeeIdChange: (String) -> Unit,
    fileNames: List<String>,
    onStartRemoval: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (fileNames.size == 1) "Selected PDF" else "${fileNames.size} PDFs Selected",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))

                fileNames.take(5).forEach { name ->
                    Text(
                        text = "• $name",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (fileNames.size > 5) {
                    Text(
                        text = "... and ${fileNames.size - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = employeeId,
                    onValueChange = onEmployeeIdChange,
                    label = { Text("Employee ID (optional)") },
                    placeholder = { Text("e.g. 5135265") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Watermark detection: Automatic",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = onStartRemoval,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(if (fileNames.size == 1) "Remove Watermark" else "Remove Watermarks", fontSize = 16.sp)
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

@Composable
fun ProcessingScreen(
    currentFileName: String,
    currentIndex: Int,
    totalFiles: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(60.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Removing watermark…",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "$currentFileName",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Processing file $currentIndex of $totalFiles",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please wait.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ResultScreen(
    summary: BatchResultSummary,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = if (summary.totalPagesCleaned > 0) "Watermark Removed" else "No Watermark Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (summary.totalPagesCleaned > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (summary.totalPagesCleaned > 0) {
                    Text(
                        text = "${summary.successfulFiles} of ${summary.totalFiles} file(s) cleaned successfully",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pages cleaned: ${summary.totalPagesCleaned}")
                    if (summary.totalPagesUnmatched > 0) {
                        Text("Pages unchanged (no watermark match): ${summary.totalPagesUnmatched}")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The original PDF was not modified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "No matching watermark content layer was detected.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The original PDF was not modified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (summary.failures.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Failures:", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    summary.failures.forEach { fail ->
                        Text("• $fail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Done", fontSize = 16.sp)
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Watermark Removal Error",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Back to Home")
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PDF Watermark Remover Settings",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Output files are saved with '_clean.pdf' suffix.\nMetadata stripping and date/time pattern detection are enabled automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
