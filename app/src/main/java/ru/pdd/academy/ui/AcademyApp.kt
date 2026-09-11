package ru.pdd.academy.ui

import android.app.Activity
import ru.pdd.academy.ads.*
import androidx.compose.ui.res.stringResource
import ru.pdd.academy.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.pdd.academy.StudyViewModel
import ru.pdd.academy.domain.*

@Composable
fun AcademyApp(vm: StudyViewModel, ads: AdConsentController? = null) {
    CompositionLocalProvider(LocalAdConsent provides ads) {
        val state by vm.state.collectAsStateWithLifecycle()
        val error by vm.error.collectAsStateWithLifecycle()
        AcademyTheme(state?.theme ?: "system") {
            val view = LocalView.current
            val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
            SideEffect {
                (view.context as? Activity)?.window?.let { window ->
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = light
                        isAppearanceLightNavigationBars = light
                    }
                }
            }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val current = state
                if (current == null) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("ПДД Академия", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(20.dp))
                        if (error == null) CircularProgressIndicator() else {
                            Text(error.orEmpty()); PrimaryButton("Повторить", { vm.load() })
                        }
                    }
                } else if (!current.onboardingDone) Onboarding { vm.onboarding(it) }
                else MainContent(vm, current)
                if (error != null && current != null) AlertDialog(onDismissRequest = vm::clearError,
                    title = { Text("Не удалось сохранить") }, text = { Text(error.orEmpty()) },
                    confirmButton = { TextButton(onClick = vm::clearError) { Text("Понятно") } })
            }
        }
    }
}

@Composable
private fun MainContent(vm: StudyViewModel, state: AppState) {
    var settings by rememberSaveable { mutableStateOf(false) }
    if (settings) AlertDialog(onDismissRequest = { settings = false },
        title = { Text(stringResource(R.string.settings_title)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppearanceSettings(state.theme, vm::theme)
            AdPrivacyControls()
        } },
        confirmButton = { TextButton(onClick = { settings = false }) { Text(stringResource(R.string.done)) } })
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSession by rememberSaveable { mutableStateOf(false) }
    var pendingIds by remember { mutableStateOf<List<String>?>(null) }
    var pendingTitle by remember { mutableStateOf("") }
    var pendingExam by remember { mutableStateOf(false) }
    var replaceDialog by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf<Session?>(null) }
    val start: (List<String>, String) -> Unit = { ids, title ->
        if (state.active?.done == false) {
            pendingIds = ids; pendingTitle = title; pendingExam = false; replaceDialog = true
        } else { vm.learn(ids, title); showSession = true }
    }
    val startExam: () -> Unit = {
        if (state.active?.done == false) { pendingExam = true; replaceDialog = true }
        else { vm.exam(); showSession = true }
    }
    if (replaceDialog) AlertDialog(onDismissRequest = { replaceDialog = false },
        title = { Text("Есть незавершённое занятие") },
        text = { Text("Продолжите его или завершите, чтобы начать новое. Ответы и прогресс сохранятся.") },
        confirmButton = { TextButton(onClick = { replaceDialog = false; showSession = true }) { Text("Продолжить") } },
        dismissButton = { TextButton(onClick = {
            vm.abandon()
            if (pendingExam) vm.exam() else pendingIds?.let { vm.learn(it, pendingTitle) }
            replaceDialog = false; showSession = true
        }) { Text("Начать новое") } })
    val shownReview = review
    val active = state.active
    when {
        shownReview != null -> ReviewScreen(shownReview, vm.engine.byId, state, vm::favorite) { review = null }
        showSession && active != null -> {
            if (active.done) ResultScreen(active, vm.engine.byId,
                onReview = { review = active }, onClose = { showSession = false; vm.closeResult() },
                onRetryErrors = { ids -> start(ids, "Повторение ошибок") })
            else StudyScreen(vm, state, active, onClose = { showSession = false })
        }
        else -> {
            if (!settings && !replaceDialog && !showSession) DashboardConsentEffect()
            BackHandler(tab != 0) { tab = 0 }
            Scaffold(containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                    if (tab == 0 && !showSession) AdaptiveBanner(Modifier.padding(horizontal = 20.dp))
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        val labels = listOf("Главная", "Учиться", "Практика", "Прогресс")
                        val icons = listOf(Icons.Rounded.Home, Icons.AutoMirrored.Rounded.MenuBook, Icons.Rounded.PlayCircle, Icons.Rounded.Insights)
                        labels.forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { tab = i },
                            icon = { Icon(icons[i], null) }, label = { Text(label) }) }
                    }
                    }
                }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                        when (tab) {
                            0 -> HomeScreen(state, vm.questions, start, { showSession = true }, { tab = 1 }, startExam, { settings = true })
                            1 -> LearnScreen(state, vm.questions, vm.signs, start, vm::favorite)
                            2 -> PracticeScreen(state, vm.questions, start, startExam)
                            3 -> ProgressScreen(state, vm.questions, vm::theme, vm::goal, vm::reset) { review = it }
                        }
                    }
                }
            }
        }
    }
}
