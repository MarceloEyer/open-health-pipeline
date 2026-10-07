package io.openhealthpipeline.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.health.connect.client.PermissionController
import io.openhealthpipeline.app.ui.MainScreen
import io.openhealthpipeline.app.ui.MainViewModel
import io.openhealthpipeline.app.ui.theme.OpenHealthPipelineTheme

/**
 * Single-activity entry point for Open Health Pipeline.
 *
 * Responsibilities:
 *  - Hosting the Compose content tree.
 *  - Registering the Health Connect permission launcher before [onCreate] returns.
 *  - Re-checking Health Connect availability and permission state on [onResume].
 *  - Delegating all business logic to [MainViewModel].
 *
 * The permission launcher uses the official
 * [PermissionController.createRequestPermissionResultContract] as required by
 * the Health Connect SDK. This contract must be registered during [onCreate] via
 * [registerForActivityResult].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /**
     * Health Connect permission launcher.
     *
     * Registered here (not lazily) because [registerForActivityResult] must be
     * called before the activity is started.
     */
    private val permissionLauncher =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions: Set<String> ->
            viewModel.onPermissionResult(grantedPermissions)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OpenHealthPipelineTheme {
                val uiState by viewModel.uiState.collectAsState()
                MainScreen(
                    uiState = uiState,
                    onRequestPermissions = {
                        permissionLauncher.launch(viewModel.permissionsToRequest)
                    },
                    onSync = {
                        viewModel.runSync()
                    }
                )
            }
        }

        // Initial availability check on launch
        viewModel.checkAvailability()
    }

    override fun onResume() {
        super.onResume()
        // Re-check every time the app comes to the foreground so that:
        //  - Installing/updating Health Connect while the app is backgrounded is detected.
        //  - Permissions revoked from Settings are reflected immediately.
        viewModel.checkAvailability()
        viewModel.refreshPermissionState()
    }
}
