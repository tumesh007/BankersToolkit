package kpt.feature.visitreport.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import kpt.feature.visitreport.export.PhotoBytesLoader
import kpt.feature.visitreport.export.ReportPublisher
import kpt.feature.visitreport.export.VisitDocxExporter
import kpt.feature.visitreport.export.VisitPdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitPreviewScreen(
    viewModel: VisitEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val visit = state.toVisit()

    var exportMessage by remember { mutableStateOf<String?>(null) }

    fun loadPhotoBytes(photo: kpt.core.model.visit.VisitPhoto): ByteArray? {
        val path = photo.originalPath.ifBlank { photo.renderedPath ?: "" }
        return PhotoBytesLoader.loadPhotoBytes(path)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Preview", fontWeight = FontWeight.Bold) },
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
            exportMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val safeName = visit.customerName.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").take(40)
                        val fileName = "${safeName}_Report.pdf"
                        val bytes = VisitPdfExporter.generatePdfBytes(visit) { photo -> loadPhotoBytes(photo) }
                        val result = ReportPublisher.publish(fileName, "application/pdf", bytes)
                        exportMessage = if (result != null) {
                            "PDF saved to Downloads/BankersToolkit/VisitReports"
                        } else {
                            "PDF export failed"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXPORT PDF")
                }

                Button(
                    onClick = {
                        val safeName = visit.customerName.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").take(40)
                        val fileName = "${safeName}_Report.docx"
                        val bytes = VisitDocxExporter.generateDocxBytes(visit) { photo -> loadPhotoBytes(photo) }
                        val result = ReportPublisher.publish(
                            fileName,
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            bytes,
                        )
                        exportMessage = if (result != null) {
                            "DOCX saved to Downloads/BankersToolkit/VisitReports"
                        } else {
                            "DOCX export failed"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXPORT DOCX")
                }
            }

            HorizontalDivider()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "BANKER'S TOOLKIT FIELD VISIT REPORT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text("Customer Name: ${visit.customerName.ifBlank { "N/A" }}", fontWeight = FontWeight.SemiBold)
                    Text("Account No: ${visit.accountNumber.ifBlank { "N/A" }} | Loan A/C: ${visit.loanAccountNumber.ifBlank { "N/A" }}")
                    Text("Date: ${visit.visitDate} | Time: ${visit.visitTime}")
                    Text("Officer: ${visit.officerName} | Branch: ${visit.branch}")
                    Text("Address: ${visit.address}")

                    HorizontalDivider()

                    Text("Observations", fontWeight = FontWeight.Bold)
                    Text(visit.observations.ifBlank { "No observations recorded." })

                    Text("Recommendations", fontWeight = FontWeight.Bold)
                    Text(visit.recommendations.ifBlank { "No recommendations recorded." })

                    Text("Remarks", fontWeight = FontWeight.Bold)
                    Text(visit.remarks.ifBlank { "None." })

                    HorizontalDivider()

                    Text("Attached Photographs (${visit.photos.size})", fontWeight = FontWeight.Bold)
                    visit.photos.forEachIndexed { idx, photo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("#${idx + 1} - ${photo.category.displayName}", fontWeight = FontWeight.Bold)
                                Text("Caption: ${photo.caption}")
                                Text("GPS: ${photo.latitude}, ${photo.longitude} (±${photo.accuracy}m)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
