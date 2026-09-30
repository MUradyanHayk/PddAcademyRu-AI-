package ru.pdd.academy.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.*
import ru.pdd.academy.AppConstants
import ru.pdd.academy.BuildConfig
import ru.pdd.academy.R

val LocalAdConsent = staticCompositionLocalOf<AdConsentController?> { null }

internal fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Mount only on the dashboard. Form loading can complete during a quiz; presentation waits. */
@Composable
fun DashboardConsentEffect() {
    if (!AppConstants.adsEnabled) return
    val controller = LocalAdConsent.current ?: return
    val state by controller.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current.activity() ?: return
    val owner = LocalLifecycleOwner.current
    val lifecycle by owner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(state.formReady, lifecycle) {
        if (state.formReady && lifecycle.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.presentPendingForm(activity)
        }
    }
}

@Composable
fun AdPrivacyControls() {
    if (!AppConstants.adsEnabled) return
    val controller = LocalAdConsent.current ?: return
    val state by controller.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current.activity() ?: return
    if (state.privacyRequired) {
        OutlinedButton(onClick = { controller.showPrivacyOptions(activity) }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ads_privacy_choices)) }
    }
    if (state.error) {
        Text(stringResource(R.string.ads_consent_error), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { controller.refresh(activity) }, enabled = !state.busy) {
            Text(stringResource(R.string.ads_retry))
        }
    }
}

/** One instance per mounted placement. Failed requests collapse without blocking learning. */
@Composable
fun AdaptiveBanner(modifier: Modifier = Modifier) {
    if (!AppConstants.adsEnabled) return
    val controller = LocalAdConsent.current ?: return
    val consent by controller.state.collectAsStateWithLifecycle()
    val initialized by controller.initialized.collectAsStateWithLifecycle()
    if (!consent.canRequestAds || consent.busy || !initialized) return
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val width = maxWidth.value.toInt().coerceAtMost(728)
        val orientation = LocalConfiguration.current.orientation
        if (width >= 120) key(width, orientation, consent.requestGeneration) { BannerView(width) }
    }
}

@Composable
private fun BannerView(width: Int) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val size = remember(context, width) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, width) }
    if (size == AdSize.INVALID || size.height <= 0) return
    var loaded by remember { mutableStateOf(false) }
    val ad = remember(context, width) { AdView(context).apply {
        adUnitId = BuildConfig.ADMOB_BANNER_ID
        setAdSize(size)
    } }
    DisposableEffect(ad, lifecycle) {
        var released = false
        ad.adListener = object : AdListener() {
            override fun onAdLoaded() { if (!released) loaded = true }
            override fun onAdFailedToLoad(error: LoadAdError) { if (!released) loaded = false }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> ad.resume()
                Lifecycle.Event.ON_PAUSE -> ad.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        ad.loadAd(AdRequest.Builder().build())
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) ad.pause()
        onDispose {
            released = true
            lifecycle.removeObserver(observer)
            (ad.parent as? ViewGroup)?.removeView(ad)
            ad.destroy()
        }
    }
    Column(Modifier.width(width.dp).clipToBounds(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (loaded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(R.string.ads_label), Modifier.padding(top = 12.dp, bottom = 6.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AndroidView(factory = { ad }, modifier = Modifier.width(width.dp).height(if (loaded) size.height.dp else 0.dp))
        if (loaded) Spacer(Modifier.height(12.dp))
    }
}
