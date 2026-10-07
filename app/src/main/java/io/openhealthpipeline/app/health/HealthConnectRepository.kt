package io.openhealthpipeline.app.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "HealthConnectRepository"

/**
 * Single access point for Health Connect operations.
 *
 * Responsibilities:
 *  - Availability detection via [HealthConnectClient.getSdkStatus].
 *  - Managing the [HealthConnectClient] lifecycle (created when available, nulled when not).
 *  - Exposing current availability as a [StateFlow] so the ViewModel can react.
 *  - Querying granted permissions on demand.
 *
 * Thread safety: [checkAvailability] should be called from the main thread
 * (it is cheap and synchronous). [getGrantedPermissions] is a suspend function
 * safe to call from any coroutine context.
 *
 * Logging policy: never logs health values, tokens, or auth material.
 */
class HealthConnectRepository(private val context: Context) {

    private val _availability = MutableStateFlow(HealthConnectAvailability.NOT_SUPPORTED)

    /** Current Health Connect availability state. Updated by [checkAvailability]. */
    val availability: StateFlow<HealthConnectAvailability> = _availability.asStateFlow()

    private var _client: HealthConnectClient? = null

    /**
     * The active [HealthConnectClient], or null if Health Connect is unavailable.
     * Check this before calling any Health Connect API.
     */
    val client: HealthConnectClient?
        get() = _client

    /**
     * Checks Health Connect availability using the official SDK status API and
     * updates [availability] accordingly.
     *
     * Must be called:
     *  - on application start (from MainActivity.onCreate / ViewModel.init)
     *  - whenever the app returns to the foreground (from MainActivity.onResume)
     *
     * This covers the cases where:
     *  - the user installs/updates Health Connect while the app is backgrounded;
     *  - the user downgrades or uninstalls Health Connect.
     */
    fun checkAvailability() {
        Log.d(TAG, "Checking Health Connect SDK status")
        val status = HealthConnectClient.getSdkStatus(context)
        val newAvailability = when (status) {
            HealthConnectClient.SDK_AVAILABLE -> {
                if (_client == null) {
                    _client = HealthConnectClient.getOrCreate(context)
                    Log.i(TAG, "Health Connect SDK_AVAILABLE — client created")
                } else {
                    Log.d(TAG, "Health Connect SDK_AVAILABLE — client already exists")
                }
                HealthConnectAvailability.AVAILABLE
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                _client = null
                Log.w(TAG, "Health Connect SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED")
                HealthConnectAvailability.UPDATE_REQUIRED
            }
            else -> {
                _client = null
                Log.w(TAG, "Health Connect SDK_UNAVAILABLE (raw status=$status)")
                HealthConnectAvailability.NOT_SUPPORTED
            }
        }
        _availability.value = newAvailability
    }

    /**
     * Returns the set of Health Connect permissions currently granted to this app.
     *
     * Returns an empty set if Health Connect is not available or the query fails.
     * Never throws — failures are logged and an empty set is returned.
     */
    suspend fun getGrantedPermissions(): Set<String> {
        val c = _client ?: return emptySet()
        return try {
            c.permissionController.getGrantedPermissions()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query granted permissions: ${e.javaClass.simpleName}")
            emptySet()
        }
    }
}
