package kpt.feature.pdfredactor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kpt.feature.pdfredactor.PdfDocumentPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfRedactorScreen(
    viewModel: PdfRedactorViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    PdfDocumentPicker.Register(viewModel::setSelectedFiles)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Redactor & Watermark Remover", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Target PDF Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = PdfDocumentPicker::launch,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CHOOSE PDF DOCUMENTS")
                    }

                    if (state.selectedFilePaths.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Selected Files (${state.selectedFilePaths.size}):", fontWeight = FontWeight.SemiBold)
                        state.selectedFilePaths.forEach { path ->
                            Text("📄 ${path.substringAfterLast("/")}", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Redaction Mode Chips
            Text("Redaction Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.mode == RedactionMode.AUTOMATIC_WATERMARK_REMOVAL,
                    onClick = { viewModel.updateMode(RedactionMode.AUTOMATIC_WATERMARK_REMOVAL) },
                    label = { Text("Auto Watermark") }
                )
                FilterChip(
                    selected = state.mode == RedactionMode.EMPLOYEE_ID_ASSISTED,
                    onClick = { viewModel.updateMode(RedactionMode.EMPLOYEE_ID_ASSISTED) },
                    label = { Text("Employee ID Mode") }
                )
                FilterChip(
                    selected = state.mode == RedactionMode.MANUAL_RECTANGLE_MASK,
                    onClick = { viewModel.updateMode(RedactionMode.MANUAL_RECTANGLE_MASK) },
                    label = { Text("Area Mask") }
                )
            }

            // Employee ID Input Field (User configurable, NOT hard-coded)
            if (state.mode == RedactionMode.EMPLOYEE_ID_ASSISTED || state.employeeId.isNotBlank()) {
                OutlinedTextField(
                    value = state.employeeId,
                    onValueChange = { viewModel.updateEmployeeId(it) },
                    label = { Text("Employee ID (Optional)") },
                    placeholder = { Text("e.g. 5135265") },
                    supportingText = { Text("Strips employee-specific portal download stamps accurately") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Status message
            state.statusMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, fontSize = 13.sp)
                    }
                }
            }

            if (state.isProcessing) {
                LinearProgressIndicator(
                    progress = { state.progressPercent },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action Button
            Button(
                onClick = { viewModel.processRedaction() },
                enabled = !state.isProcessing && state.selectedFilePaths.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RUN PERMANENT REDACTION", fontWeight = FontWeight.Bold)
            }
        }
    }
}
