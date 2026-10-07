package io.openhealthpipeline.app.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord

/**
 * Central registry of Health Connect permissions for Open Health Pipeline.
 *
 * Every record type the app intends to read must have its read permission
 * registered here AND declared in AndroidManifest.xml. The runtime permission
 * request is built from this object's permission sets.
 *
 * Architecture note: Stage B will expand STAGE_A_PERMISSIONS into a broader
 * set. Adding a new record type requires:
 *  1. Adding its permission constant to the appropriate set below.
 *  2. Declaring <uses-permission> in AndroidManifest.xml.
 *  3. Adding a reader/mapper for the type.
 *
 * The string constants (READ_STEPS etc.) are defined here so that JVM unit
 * tests can verify the permission logic without requiring the Android SDK.
 * The runtime sets are built from the SDK's HealthPermission factory, which
 * returns the same stable string values.
 */
object HealthPermissions {

    // -------------------------------------------------------------------------
    // Permission string constants
    // These are stable Android platform values also declared in AndroidManifest.xml.
    // -------------------------------------------------------------------------

    /** Read permission string for StepsRecord. */
    const val READ_STEPS = "android.permission.health.READ_STEPS"

    // -------------------------------------------------------------------------
    // Permission sets used for launcher requests
    // -------------------------------------------------------------------------

    /**
     * All read permissions required for Stage A.
     * Passed directly to the PermissionController launcher in MainActivity.
     *
     * Uses the SDK factory method to ensure correctness at runtime; the returned
     * string is identical to [READ_STEPS].
     */
    val STAGE_A_PERMISSIONS: Set<String> by lazy {
        setOf(HealthPermission.getReadPermission(StepsRecord::class))
    }

    // -------------------------------------------------------------------------
    // Permission utility functions
    // -------------------------------------------------------------------------

    /**
     * Returns the subset of [STAGE_A_PERMISSIONS] that are not present
     * in [grantedPermissions]. An empty set means all permissions are granted.
     */
    fun missingPermissions(grantedPermissions: Set<String>): Set<String> {
        val required = setOf(READ_STEPS)
        return required - grantedPermissions
    }

    /**
     * Returns true if the Steps read permission is present in [grantedPermissions].
     * This is the minimum requirement for Stage A to function.
     */
    fun hasMinimumPermissions(grantedPermissions: Set<String>): Boolean {
        return grantedPermissions.contains(READ_STEPS)
    }
}