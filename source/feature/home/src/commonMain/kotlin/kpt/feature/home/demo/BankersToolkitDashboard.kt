package kpt.feature.home.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BankersToolkitDashboard(
    onNavigateToVisitReport: () -> Unit = {},
    onNavigateToGpsCamera: () -> Unit = {},
    onNavigateToEmiCalculator: () -> Unit = {},
    onNavigateToAmortization: () -> Unit = {},
    onNavigateToIdCardPrinter: () -> Unit = {},
    onNavigateToPdfRedactor: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Title Banner
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "BANKER'S TOOLKIT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Field Banking, Document Processing & Financial Tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Section 1: FIELD TOOLS
        DashboardSectionTitle("FIELD TOOLS")
        ToolCard(
            title = "Visit Reports",
            subtitle = "Field visit reporting, offline photo capture, PDF/DOCX export",
            icon = Icons.Default.Assignment,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onNavigateToVisitReport
        )
        ToolCard(
            title = "GPS Camera",
            subtitle = "Standalone GPS photo capture & structured metadata",
            icon = Icons.Default.CameraAlt,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onNavigateToGpsCamera
        )

        // Section 2: DOCUMENT & KYC TOOLS
        DashboardSectionTitle("DOCUMENT & KYC TOOLS")
        ToolCard(
            title = "ID Card Scanner & Printer",
            subtitle = "1:1 wallet & KYC A4 sheets, 2.5x loupe crop, direct Wi-Fi printing",
            icon = Icons.Default.Badge,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            onClick = onNavigateToIdCardPrinter
        )
        ToolCard(
            title = "PDF Watermark Redactor",
            subtitle = "Batch remove PNB Saksham & CBS download watermarks",
            icon = Icons.Default.LayersClear,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onNavigateToPdfRedactor
        )

        // Section 3: FINANCIAL TOOLS
        DashboardSectionTitle("FINANCIAL TOOLS")
        ToolCard(
            title = "EMI Calculator",
            subtitle = "Equated Monthly Installment calculation with zero-interest support",
            icon = Icons.Default.Calculate,
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            onClick = onNavigateToEmiCalculator
        )
        ToolCard(
            title = "Amortization",
            subtitle = "Month-by-month principal/interest breakdown table",
            icon = Icons.Default.TableChart,
            iconContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            onClick = onNavigateToAmortization
        )
    }
}

@Composable
private fun DashboardSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
