package io.openhealthpipeline.app.health

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HealthPermissions] permission logic.
 *
 * NOTE: HealthPermissions.STAGE_A_PERMISSIONS calls
 * HealthPermission.getReadPermission() which is an Android SDK method.
 * These tests do NOT call STAGE_A_PERMISSIONS directly to avoid requiring
 * Robolectric in the local JVM test suite.
 *
 * Instead they test the logic functions (missingPermissions, hasMinimumPermissions)
 * using the well-known permission string constant, which is a stable Android
 * platform value declared in AndroidManifest.xml.
 */
class HealthPermissionsTest {

    /**
     * The well-known permission string for StepsRecord.
     * This matches the value returned by
     * HealthPermission.getReadPermission(StepsRecord::class) and is also
     * declared as <uses-permission> in AndroidManifest.xml.
     */
    private val stepsPermission = "android.permission.health.READ_STEPS"

    @Test
    fun `missingPermissions returns empty when full permission set granted`() {
        val grantedFull = setOf(stepsPermission)
        val missing = HealthPermissions.missingPermissions(grantedFull)
        assertTrue("No permissions should be missing when Steps is granted", missing.isEmpty())
    }

    @Test
    fun `missingPermissions returns non-empty when nothing granted`() {
        val missing = HealthPermissions.missingPermissions(emptySet())
        assertFalse("At least one permission should be missing", missing.isEmpty())
    }

    @Test
    fun `missingPermissions returns empty when superset granted`() {
        val grantedPlus = setOf(stepsPermission, "android.permission.CAMERA")
        val missing = HealthPermissions.missingPermissions(grantedPlus)
        assertTrue("Superset should satisfy all requirements", missing.isEmpty())
    }

    @Test
    fun `hasMinimumPermissions returns false for empty set`() {
        assertFalse(
            "Empty granted set should not satisfy minimum permissions",
            HealthPermissions.hasMinimumPermissions(emptySet())
        )
    }

    @Test
    fun `hasMinimumPermissions returns false for unrelated permission only`() {
        assertFalse(
            "Unrelated permission should not satisfy Steps requirement",
            HealthPermissions.hasMinimumPermissions(setOf("android.permission.CAMERA"))
        )
    }

    @Test
    fun `hasMinimumPermissions returns true when Steps permission granted`() {
        assertTrue(
            "Steps permission should satisfy minimum requirement",
            HealthPermissions.hasMinimumPermissions(setOf(stepsPermission))
        )
    }

    @Test
    fun `hasMinimumPermissions returns true when Steps plus extras granted`() {
        val extra = setOf(stepsPermission, "android.permission.CAMERA", "android.permission.RECORD_AUDIO")
        assertTrue(
            "Superset including Steps should satisfy minimum requirement",
            HealthPermissions.hasMinimumPermissions(extra)
        )
    }
}