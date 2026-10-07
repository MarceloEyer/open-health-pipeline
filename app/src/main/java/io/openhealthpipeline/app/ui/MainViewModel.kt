package io.openhealthpipeline.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.openhealthpipeline.app.health.HealthConnectAvailability
import io.openhealthpipeline.app.health.HealthConnectRepository
import io.openhealthpipeline.app.health.HealthPermissions
import io.openhealthpipeline.app.sync.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MainViewModel"

/**
 * ViewModel for the main screen.
 *
 * Bridges the Health Connect layer with the Compose UI without exposing
 * Health Connect SDK types or implementation details to the screen composable.
 *
 * Threading: all mutation of [_uiState] occurs on the ViewModelScope dispatcher,
 * which defaults to Main. [HealthConnectRepository.getGrantedPermissions] and
 * [SyncCoordinator.runSync] are suspend functions safe to call from any context.
 *
 * Logging policy: DEBUG-level logs include permission counts and state transitions.
 * Health values are never logged. Logs are safe to share in bug reports.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    /** Exposed for MainActivity to wire up the permission launcher. */
    val repository = HealthConnectRepository(application)
    private val coordinator = SyncCoordinator(repository)

    private val _uiState = MutableStateFlow(UiState())

    /** Immutable state snapshot observed by the Compose UI. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * The exact set of permissions to pass to the Health Connect permission launcher.
     * Derived from [HealthPermissions.STAGE_A_PERMISSIONS].
     */
    val permissionsToRequest: Set<String> = HealthPermissions.STAGE_A_PERMISSIONS

    private val displayFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        // Reflect availability changes from the repository immediately in the UI.
        viewModelScope.launch {
            repository.availability.collect { availability ->
                _uiState.update { it.copy(availability = availability) }
                // When Health Connect becomes available, recheck permission state
                // so the UI immediately shows the correct permission badge.
                if (availability == HealthConnectAvailability.AVAILABLE) {
                    refreshPermissionState()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public actions called by MainActivity
    // -------------------------------------------------------------------------

    /**
     * Triggers an availability check. Called by MainActivity on create and resume.
     * Updates [uiState].availability via the repository's StateFlow.
     */
    fun checkAvailability() {
        repository.checkAvailability()
    }

    /**
     * Processes the result returned by the Health Connect permission launcher.
     * Called by MainActivity after the permission request activity returns.
     *
     * @param grantedPermissions The set of permissions returned by the launcher.
     */
    fun onPermissionResult(grantedPermissions: Set<String>) {
        Log.d(TAG, "Permission launcher returned: ${grantedPermissions.size} granted")
        val missing = HealthPermissions.missingPermissions(grantedPermissions)
        if (missing.isNotEmpty()) {
            Log.w(TAG, "Permissions still missing after request: ${missing.size} denied")
        }
        // Re-query the PermissionController directly rather than trusting the
        // launcher callback, to handle edge cases where the launcher result
        // doesn't perfectly reflect the stored permission state.
        viewModelScope.launch { refreshPermissionState() }
    }

    /**
     * Re-queries the Health Connect PermissionController and updates [uiState].
     * Called on resume and after a permission result.
     */
    fun refreshPermissionState() {
        viewModelScope.launch {
            val granted = repository.getGrantedPermissions()
            val hasSteps = HealthPermissions.hasMinimumPermissions(granted)
            Log.d(TAG, "Permission state: stepsGranted=$hasSteps, total granted=${granted.size}")
            _uiState.update { it.copy(stepsPermissionGranted = hasSteps) }
        }
    }

    /**
     * Triggers a sync run. No-ops if a sync is already in progress.
     * Updates [uiState] with progress and the final [io.openhealthpipeline.app.sync.SyncResult].
     */
    fun runSync() {
        if (_uiState.value.isSyncing) {
            Log.d(TAG, "Sync already in progress — ignoring duplicate request")
            return
        }

        viewModelScope.launch {
            Log.i(TAG, "Sync triggered from UI")
            _uiState.update { it.copy(isSyncing = true, lastError = null) }

            try {
                val result = coordinator.runSync()
                val timestamp = displayFormatter.format(Date(result.completedAt))

                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncAt = timestamp,
                        lastSyncResult = result,
                        lastError = when {
                            result.failedTypes > 0 ->
                                "${result.failedTypes} type(s) failed — check logcat"
                            else -> null
                        }
                    )
                }

                Log.i(TAG, "Sync UI updated: ${result.totalRecordsRead} records, completed at $timestamp")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Should not normally reach here — SyncCoordinator catches exceptions per type.
                // This is a final safety net.
                Log.e(TAG, "Unexpected sync exception: ${e.javaClass.simpleName} — ${e.message}")
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        lastError = "Unexpected error: ${e.javaClass.simpleName}"
                    )
                }
            }
        }
    }
}
