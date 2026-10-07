package io.openhealthpipeline.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.openhealthpipeline.app.health.HealthConnectAvailability
import io.openhealthpipeline.app.health.model.SyncRecordStatus

/**
 * Main screen of Open Health Pipeline.
 *
 * Displays:
 *  - Health Connect availability status
 *  - Steps permission state
 *  - Last sync timestamp
 *  - Per-type record counts from the most recent sync
 *  - Non-sensitive error messages
 *  - Request Permissions and Read Health Connect buttons
 *
 * Does NOT display individual health measurement values.
 * Visual polish is not a Stage A priority per the design.
 */
@Composable
fun MainScreen(
    uiState: UiState,
    onRequestPermissions: () -> Unit,
    onSync: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Open Health Pipeline",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Stage A — Health Connect Proof",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // ── Health Connect availability ───────────────────────────────────
            StatusRow(
                label = "Health Connect",
                value = when (uiState.availability) {
                    HealthConnectAvailability.AVAILABLE -> "Available"
                    HealthConnectAvailability.UPDATE_REQUIRED -> "Update Required"
                    HealthConnectAvailability.NOT_SUPPORTED -> "Not Supported"
                },
                isError = uiState.availability != HealthConnectAvailability.AVAILABLE
            )

            // ── Steps permission ──────────────────────────────────────────────
            StatusRow(
                label = "Steps Permission",
                value = if (uiState.stepsPermissionGranted) "Granted" else "Not Granted",
                isError = !uiState.stepsPermissionGranted
            )

            // ── Last sync ─────────────────────────────────────────────────────
            StatusRow(
                label = "Last Sync",
                value = uiState.lastSyncAt ?: "Never"
            )

            // ── Per-type results from last sync ───────────────────────────────
            uiState.lastSyncResult?.let { result ->
                HorizontalDivider()

                Text(
                    text = "Last Sync Results",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                result.typeResults.forEach { typeResult ->
                    StatusRow(
                        label = typeResult.recordType,
                        value = when (typeResult.status) {
                            SyncRecordStatus.SUCCESS ->
                                "${typeResult.recordsRead} records (${typeResult.pagesRead} pages)"
                            SyncRecordStatus.PERMISSION_DENIED ->
                                "Permission denied"
                            SyncRecordStatus.FEATURE_UNAVAILABLE ->
                                "Feature unavailable"
                            SyncRecordStatus.HEALTH_CONNECT_ERROR ->
                                "HC error: ${typeResult.errorCategory}"
                            SyncRecordStatus.MAPPING_ERROR ->
                                "Mapping error"
                            SyncRecordStatus.PARTIAL ->
                                "Partial: ${typeResult.recordsRead} records"
                        },
                        isError = typeResult.status != SyncRecordStatus.SUCCESS
                    )
                }

                Text(
                    text = "Total records: ${result.totalRecordsRead}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Duration: ${result.durationMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Error banner ──────────────────────────────────────────────────
            uiState.lastError?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider()

            // ── Request Permissions button ────────────────────────────────────
            // Shown when permission is missing and Health Connect is available.
            if (!uiState.stepsPermissionGranted) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.availability == HealthConnectAvailability.AVAILABLE &&
                              !uiState.isSyncing
                ) {
                    Text("Request Permissions")
                }
            }

            // ── Read Health Connect / Sync Now button ─────────────────────────
            Button(
                onClick = onSync,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.availability == HealthConnectAvailability.AVAILABLE &&
                          uiState.stepsPermissionGranted &&
                          !uiState.isSyncing
            ) {
                if (uiState.isSyncing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Syncing…")
                    }
                } else {
                    Text("Read Health Connect")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Reusable components ──────────────────────────────────────────────────────

/**
 * A label/value pair displayed as a horizontal row.
 * Used for all status items on the main screen.
 */
@Composable
private fun StatusRow(
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
