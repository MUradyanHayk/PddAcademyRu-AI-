package ru.pdd.academy.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelStore
import ru.pdd.academy.StudyViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.pdd.academy.domain.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun questions(): List<Question> {
        val file = listOf(File("src/main/assets/questions.json"), File("app/src/main/assets/questions.json")).first { it.exists() }
        return Json.decodeFromString(file.readText())
    }
    private fun capture(name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    private fun awaitState(predicate: () -> Boolean) {
        compose.waitUntil(20_000) { org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle(); predicate() }
    }
    @Test fun realViewModelLoadsAndConfirmedAnswerSurvivesRepositoryReload() {
        val store = ViewModelStore()
        lateinit var vm: StudyViewModel
        compose.runOnIdle { vm = StudyViewModel(ApplicationProvider.getApplicationContext<Application>()); store.put("study", vm) }
        try {
            compose.setContent { AcademyApp(vm) }
            awaitState { check(vm.error.value == null) { vm.error.value.orEmpty() }; vm.state.value != null }
            compose.runOnIdle { vm.onboarding(20) }
            awaitState { vm.state.value?.onboardingDone == true }
            compose.onNodeWithText("Начать занятие").assertIsDisplayed()
            capture("app-home")
            compose.onNodeWithText("Начать занятие").performClick()
            awaitState { vm.state.value?.active != null }
            val id = vm.state.value!!.active!!.questionIds.first()
            val q = vm.engine.byId.getValue(id)
            compose.onNodeWithText(q.answers[q.correct]).performScrollTo().performClick()
            compose.onNodeWithText("Подтвердить ответ").performClick()
            awaitState { vm.state.value?.active?.answers?.containsKey(id) == true }
            assertEquals(1, vm.state.value!!.progress.getValue(id).attempts)
            val disk = kotlinx.coroutines.runBlocking {
                ru.pdd.academy.data.StudyRepository(ApplicationProvider.getApplicationContext()).loadState()
            }
            assertEquals(q.correct, disk.active!!.answers[id])
            capture("question-answered")
        } finally { store.clear() }
    }
    @Test fun appearanceShortcutChangesThemeAndPersistsEveryMode() {
        val store = ViewModelStore()
        lateinit var vm: StudyViewModel
        compose.runOnIdle { vm = StudyViewModel(ApplicationProvider.getApplicationContext<Application>()); store.put("theme", vm) }
        try {
            compose.setContent { AcademyApp(vm) }
            awaitState { vm.state.value != null }
            compose.runOnIdle { vm.onboarding(20) }
            awaitState { vm.state.value?.onboardingDone == true }
            compose.onNodeWithContentDescription("Настройки").performClick()
            listOf("dark" to "Тёмная тема", "light" to "Светлая тема", "system" to "Как на устройстве").forEach { (key, label) ->
                compose.onNodeWithText(label).performScrollTo().performClick()
                awaitState { vm.state.value?.theme == key }
                val disk = kotlinx.coroutines.runBlocking {
                    ru.pdd.academy.data.StudyRepository(ApplicationProvider.getApplicationContext()).loadState()
                }
                assertEquals(key, disk.theme)
                if (key == "dark") capture("appearance-dark")
            }
            compose.onNodeWithText("Готово").performClick()
            compose.onNodeWithText("Начать занятие").assertIsDisplayed()
        } finally { store.clear() }
    }
    @Test fun homeRendersAndStartsDailyPlan() {
        val bank = questions(); var selected = emptyList<String>()
        compose.setContent { AcademyTheme("light") { Surface(Modifier.fillMaxSize()) {
            HomeScreen(AppState(onboardingDone = true), bank, { ids, _ -> selected = ids }, {}, {}, {})
        } } }
        compose.onNodeWithText("Начать занятие").assertIsDisplayed(); capture("home-light")
        compose.onNodeWithText("Начать занятие").performClick(); assertEquals(20, selected.size)
    }
    @Test fun darkHomeRenders() {
        compose.setContent { AcademyTheme("dark") { Surface(Modifier.fillMaxSize()) {
            HomeScreen(AppState(onboardingDone = true), questions(), { _, _ -> }, {}, {}, {})
        } } }
        compose.onNodeWithText("Начать занятие").assertIsDisplayed(); capture("home-dark")
    }
    @Test fun searchFindsQuestionsAndTicketStartsTwenty() {
        val bank = questions(); var ids = emptyList<String>()
        compose.setContent { AcademyTheme("light") { Surface(Modifier.fillMaxSize()) {
            LearnScreen(AppState(), bank, emptyList(), { selected, _ -> ids = selected }, {})
        } } }
        compose.onNodeWithText("Билет 1").performClick(); assertEquals(20, ids.size); capture("tickets")
        compose.onNode(hasSetTextAction()).performTextInput("несуществующийвопрос")
        compose.onNodeWithText("Ничего не найдено").assertExists()
    }
    @Test fun unansweredTimeoutReviewDoesNotCrash() {
        val bank = questions(); val first = bank.first()
        val s = Session("review", "Экзамен", Mode.EXAM, listOf(first.id),
            answers = mapOf(first.id to -1), stage = Stage.DONE, startedAt = 0, passed = false)
        compose.setContent { AcademyTheme("light") { ReviewScreen(s, bank.associateBy { it.id }, AppState(), {}) {} } }
        compose.onNodeWithText("Вы пропустили этот вопрос").assertExists(); capture("review")
    }
}
