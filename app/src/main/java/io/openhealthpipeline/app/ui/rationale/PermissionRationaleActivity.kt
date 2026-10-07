package io.openhealthpipeline.app.ui.rationale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.openhealthpipeline.app.ui.theme.OpenHealthPipelineTheme

/**
 * Health Connect opens this screen from its permission management UI.
 */
class PermissionRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenHealthPipelineTheme {
                PermissionRationaleScreen()
            }
        }
    }
}

@Composable
private fun PermissionRationaleScreen() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Health data permissions",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Open Health Pipeline asks for read access to your step records so it can verify that Health Connect data can be read on this device."
            )
            Text(
                text = "Stage A reads only Steps data from the last 30 days. The app does not upload, sell, or share health data in this stage."
            )
            Text(
                text = "You can revoke access at any time in Health Connect settings."
            )
        }
    }
}
