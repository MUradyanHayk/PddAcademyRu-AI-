package ru.pdd.academy.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import ru.pdd.academy.AppConstants

/** Activity-scoped consent state. Never retains an Activity in a process singleton. */
class AdConsentController(context: Context) {
    private val app = context.applicationContext
    private val information by lazy { UserMessagingPlatform.getConsentInformation(app) }
    private val mutableState = MutableStateFlow(ConsentState())
    val state = mutableState.asStateFlow()
    val initialized = sdkInitialized.asStateFlow()
    private var pendingForm: ConsentForm? = null
    private var disposed = false
    private var refreshing = false

    fun refresh(activity: Activity) {
        if (!AppConstants.adsEnabled || disposed || refreshing || state.value.busy || !activity.usable()) return
        refreshing = true
        mutableState.value = state.value.copy(error = false)
        information.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            if (!AppConstants.adsEnabled || disposed || !activity.usable()) return@requestConsentInfoUpdate
            refreshing = false
            publish()
            if (information.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                UserMessagingPlatform.loadConsentForm(app, { form ->
                    if (!disposed) {
                        pendingForm = form
                        mutableState.value = state.value.copy(formReady = true)
                    }
                }, {
                    if (!disposed) publish(error = true)
                })
            }
        }, {
            refreshing = false
            if (!disposed) publish(error = true)
        })
    }

    /** Called only from the dashboard, never from an active question or exam. */
    fun presentPendingForm(activity: Activity) {
        if (!AppConstants.adsEnabled || disposed || state.value.busy || !activity.usable()) return
        val form = pendingForm ?: return
        pendingForm = null
        mutableState.value = state.value.copy(busy = true, formReady = false, canRequestAds = false)
        form.show(activity) { error -> if (!disposed) publish(error != null) }
    }

    fun showPrivacyOptions(activity: Activity) {
        if (!AppConstants.adsEnabled || disposed || state.value.busy || !activity.usable()) return
        if (!state.value.privacyRequired) { refresh(activity); return }
        pendingForm = null
        // Unmount existing banners before changing consent; a subsequent banner uses a fresh request.
        mutableState.value = state.value.copy(busy = true, canRequestAds = false, error = false, formReady = false)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (!disposed) publish(error != null)
        }
    }

    private fun publish(error: Boolean = false) {
        if (!AppConstants.adsEnabled) return
        val allowed = information.canRequestAds()
        mutableState.value = ConsentState(
            requestGeneration = state.value.requestGeneration + 1,
            canRequestAds = allowed,
            privacyRequired = information.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            formReady = pendingForm != null,
            error = error
        )
        if (allowed && initializing.compareAndSet(false, true)) {
            sdkScope.launch {
                try {
                    MobileAds.initialize(app) { sdkInitialized.value = true }
                } catch (_: Exception) {
                    initializing.set(false)
                }
            }
        }
    }

    fun dispose() { disposed = true; pendingForm = null }
    private fun Activity.usable() = !isFinishing && !isDestroyed

    companion object {
        private val initializing = AtomicBoolean(false)
        private val sdkInitialized = MutableStateFlow(false)
        private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

data class ConsentState(
    val requestGeneration: Int = 0,
    val canRequestAds: Boolean = false,
    val privacyRequired: Boolean = false,
    val busy: Boolean = false,
    val formReady: Boolean = false,
    val error: Boolean = false
)
