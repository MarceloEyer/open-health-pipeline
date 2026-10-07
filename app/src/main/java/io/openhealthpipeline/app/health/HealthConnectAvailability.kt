package io.openhealthpipeline.app.health

/**
 * Represents the availability state of the Health Connect provider
 * as reported by [androidx.health.connect.client.HealthConnectClient.getSdkStatus].
 *
 * Maps directly to the three SDK status constants:
 *   SDK_AVAILABLE                           → AVAILABLE
 *   SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED → UPDATE_REQUIRED
 *   SDK_UNAVAILABLE                         → NOT_SUPPORTED
 */
enum class HealthConnectAvailability {

    /**
     * Health Connect is installed, up-to-date, and ready to use.
     * Health Connect operations may proceed.
     */
    AVAILABLE,

    /**
     * Health Connect is present but needs to be installed or updated.
     * The app should direct the user to the Play Store to install/update.
     */
    UPDATE_REQUIRED,

    /**
     * Health Connect is not supported on this device (API level too low or
     * device/OEM does not support it).
     * Health Connect-dependent functionality must be disabled.
     */
    NOT_SUPPORTED
}
