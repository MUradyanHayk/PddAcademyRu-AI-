package ru.pdd.academy

import android.app.Application
import android.os.SystemClock
import android.util.Log
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.pdd.academy.data.StudyRepository
import ru.pdd.academy.domain.*
import java.time.LocalDate

class StudyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StudyRepository(app)
    private val lock = Mutex()
    private val mutable = MutableStateFlow<AppState?>(null)
    val state = mutable.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val mutableClock = MutableStateFlow(clock())
    val time = mutableClock.asStateFlow()
    var questions: List<Question> = emptyList(); private set
    var signs: List<RoadSign> = emptyList(); private set
    lateinit var engine: StudyEngine; private set
    init {
        load()
        viewModelScope.launch {
            while (true) {
                delay(500)
                mutableClock.value = clock()
                val s = mutable.value?.active
                if (s != null && !s.done && s.mode == Mode.EXAM && s.remaining(clock()) == 0L)
                    change { current -> current.active?.let { active -> withSession(current, engine.expire(active, clock())) } ?: current }
            }
        }
    }
    fun load() = viewModelScope.launch {
        lock.withLock {
            try {
                questions = repo.loadQuestions()
                signs = repo.loadSigns()
                engine = StudyEngine(questions)
                val initial = repo.loadState()
                require(initial.active?.questionIds?.all { it in engine.byId } != false)
                val checked = initial.active?.let { withSession(initial, engine.expire(it, clock())) } ?: initial
                val next = finishIfNeeded(checked)
                repo.save(next)
                mutable.value = next
                mutableError.value = null
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { Log.e("PddAcademy", "Loading failed", e); mutableError.value = "Не удалось загрузить данные. Повторите попытку. Ваш прогресс не удалён." }
        }
    }
    fun clock() = Clock(System.currentTimeMillis(), SystemClock.elapsedRealtime(),
        Settings.Global.getInt(getApplication<Application>().contentResolver, Settings.Global.BOOT_COUNT, -1))

    private fun change(transform: (AppState) -> AppState) = viewModelScope.launch {
        lock.withLock {
            val old = mutable.value ?: return@withLock
            try {
                val next = transform(old)
                if (next != old) { repo.save(next); mutable.value = next }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { Log.e("PddAcademy", "Saving failed", e); mutableError.value = "Не удалось сохранить изменение. Попробуйте ещё раз." }
        }
    }
    fun clearError() { mutableError.value = null }
    fun onboarding(goal: Int) = change { it.copy(onboardingDone = true, dailyGoal = goal) }
    fun theme(value: String) = change { it.copy(theme = value) }
    fun goal(value: Int) = change { it.copy(dailyGoal = value.coerceIn(10, 40)) }
    fun favorite(id: String) = change { it.copy(favorites = if (id in it.favorites) it.favorites - id else it.favorites + id) }
    fun reset() = change { AppState(onboardingDone = true, theme = it.theme, dailyGoal = it.dailyGoal) }
    fun learn(ids: List<String>, title: String) = change {
        if (ids.isEmpty() || it.active?.done == false) it
        else it.copy(active = engine.learn(ids.distinct(), title, clock()))
    }
    fun exam() = change { if (it.active?.done == false) it else it.copy(active = engine.exam(clock())) }
    fun closeResult() = change { if (it.active?.done == true) it.copy(active = null) else it }
    fun abandon() = change { s -> s.active?.let { finishIfNeeded(s.copy(active = engine.abandon(it, clock()))) } ?: s }
    fun move(index: Int) = change { s ->
        val active = s.active ?: return@change s
        if (index !in active.questionIds.indices || active.done || (active.stage == Stage.EXTRA && index < active.baseCount)) s
        else s.copy(active = active.copy(index = index))
    }
    fun answer(id: String, choice: Int) = change { state ->
        val current = state.active ?: return@change state
        val next = engine.answer(current, id, choice, clock())
        if (next == current) return@change state
        withSession(state, next)
    }
    private fun withSession(state: AppState, next: Session): AppState {
        var progress = state.progress
        var daily = state.dailyAnswers
        val oldAnswers = state.active?.answers.orEmpty()
        val day = LocalDate.now().toString()
        next.answers.filterKeys { it !in oldAnswers }.forEach { (id, choice) ->
            progress = progress + (id to (progress[id] ?: QuestionProgress()).answered(
                choice == engine.byId.getValue(id).correct, clock().wall))
            if (choice >= 0) daily = daily + (day to ((daily[day] ?: 0) + 1))
        }
        return finishIfNeeded(state.copy(active = next, progress = progress, dailyAnswers = daily))
    }
    private fun finishIfNeeded(state: AppState): AppState {
        val s = state.active ?: return state
        if (!s.done || state.history.any { it.id == s.id }) return state
        return state.copy(history = (listOf(s) + state.history).take(100))
    }
}
