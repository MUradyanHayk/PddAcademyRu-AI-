package ru.pdd.academy

object AppConstants {
    /** Logging is enabled only in debug builds. */
    @JvmField
    val IS_DEBUG: Boolean = BuildConfig.DEBUG

    /** Set to false and rebuild to disable ads and ad consent UI in every build variant. */
    const val HAS_ADDS = false

    // The master switch never bypasses the release AdMob configuration gate.
    internal val adsEnabled: Boolean
        get() = HAS_ADDS && BuildConfig.ADS_ENABLED
}
