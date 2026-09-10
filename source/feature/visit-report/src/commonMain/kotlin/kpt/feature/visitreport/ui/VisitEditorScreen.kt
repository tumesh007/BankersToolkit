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
import kpt.core.model.visit.VisitStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitEditorScreen(
    viewModel: VisitEditorViewModel,
    onLaunchCameraForVisit: (String) -> Unit,
    onPreviewVisit: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id.isBlank()) "New Visit Report" else "Edit Visit Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveVisit(VisitStatus.COMPLETED)
                        onNavigateBack()
                    }) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Customer & Account Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            var formatDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = formatDropdownExpanded,
                onExpandedChange = { formatDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.reportFormat.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Report Format") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = formatDropdownExpanded,
                    onDismissRequest = { formatDropdownExpanded = false }
                ) {
                    kpt.core.model.visit.ReportFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format.displayName) },
                            onClick = {
                                viewModel.updateReportFormat(format)
                                formatDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.customerName,
                onValueChange = { viewModel.updateCustomerName(it) },
                label = { Text("Customer / Party Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.accountNumber,
                    onValueChange = { viewModel.updateAccountNumber(it) },
                    label = { Text("Account Number") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.loanAccountNumber,
                    onValueChange = { viewModel.updateLoanAccountNumber(it) },
                    label = { Text("Loan A/C Number") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = state.address,
                onValueChange = { viewModel.updateAddress(it) },
                label = { Text("Visited Address") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.visitDate,
                    onValueChange = { viewModel.updateVisitDate(it) },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.visitTime,
                    onValueChange = { viewModel.updateVisitTime(it) },
                    label = { Text("Time (HH:MM)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.officerName,
                    onValueChange = { viewModel.updateOfficerName(it) },
                    label = { Text("Officer Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.branch,
                    onValueChange = { viewModel.updateBranch(it) },
                    label = { Text("Branch") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = state.purpose,
                onValueChange = { viewModel.updatePurpose(it) },
                label = { Text("Visit Purpose") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Officer Observations & Remarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = state.observations,
                onValueChange = { viewModel.updateObservations(it) },
                label = { Text("Detailed Observations") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = state.recommendations,
                onValueChange = { viewModel.updateRecommendations(it) },
                label = { Text("Recommendations") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = state.remarks,
                onValueChange = { viewModel.updateRemarks(it) },
                label = { Text("Additional Remarks") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // GPS Photos Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GPS Photographs (${state.photos.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = {
                    // Persist the visit (DRAFT) first and use its real id so photos captured
                    // in the GPS camera attach to an existing visit row (FK constraint).
                    onLaunchCameraForVisit(viewModel.saveVisit())
                }) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD PHOTO")
                }
            }

            state.photos.forEach { photo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(photo.category.displayName, fontWeight = FontWeight.Bold)
                            if (photo.caption.isNotBlank()) {
                                Text(photo.caption, fontSize = 13.sp)
                            }
                            Text("GPS: ${photo.latitude}, ${photo.longitude}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.removePhoto(photo.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Photo", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.saveVisit(VisitStatus.COMPLETED)
                    onPreviewVisit(state.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PREVIEW & EXPORT REPORT", fontWeight = FontWeight.Bold)
            }
        }
    }
}
