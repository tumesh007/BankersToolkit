package kpt.feature.longbookmerger.ui

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongbookMergerScreen(
    viewModel: LongbookMergerViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finacle / CBSL Longbook Merger", fontWeight = FontWeight.Bold) },
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
            // Input Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CBSL / Finacle Export Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.setSelectedFiles(
                                listOf(
                                    "/sdcard/Download/Finacle_CP_Branch_Longbook.csv",
                                    "/sdcard/Download/Finacle_NR_Branch_Longbook.csv"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SELECT CBSL / FINACLE FILES")
                    }

                    if (state.selectedFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        state.selectedFiles.forEach { path ->
                            Text("📁 ${path.substringAfterLast("/")}", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Raw Export / Preview Editor
            OutlinedTextField(
                value = state.rawCsvInput,
                onValueChange = { viewModel.updateRawInput(it) },
                label = { Text("Finacle CSV / HTML Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                maxLines = 8
            )

            // Parsed Summary Card
            state.parsedReport?.let { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Parsed Report Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Branch: ${report.branchName} | Date: ${report.reportDate}")
                        Text("Total Records: ${report.records.size}")
                        Text("Total Debit: ₹${report.totalDebit} | Total Credit: ₹${report.totalCredit}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
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
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, fontSize = 13.sp)
                    }
                }
            }

            // Generate Button
            Button(
                onClick = { viewModel.generateMergedPdf() },
                enabled = !state.isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MERGE & CREATE A4 LANDSCAPE PDF", fontWeight = FontWeight.Bold)
            }
        }
    }
}
