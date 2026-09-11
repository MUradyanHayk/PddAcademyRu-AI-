package ru.pdd.academy.domain

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: String, val sourceId: String, val ticket: Int, val number: Int,
    val text: String, val answers: List<String>, val correct: Int,
    val explanation: String, val topics: List<String>, val image: String? = null
) { val block: Int get() = (number - 1) / 5 }

@Serializable
data class RoadSign(val number: String, val title: String, val category: String,
                    val description: String, val image: String)

@Serializable
enum class Mode { LEARN, EXAM }
@Serializable
enum class Stage { BASE, EXTRA, DONE }
@Serializable
data class Clock(val wall: Long, val elapsed: Long, val boot: Int)

@Serializable
data class Session(
    val id: String, val title: String, val mode: Mode, val questionIds: List<String>,
    val answers: Map<String, Int> = emptyMap(), val stage: Stage = Stage.BASE,
    val index: Int = 0, val baseCount: Int = questionIds.size,
    val startedAt: Long, val deadlineWall: Long = 0, val deadlineElapsed: Long = 0,
    val boot: Int = 0, val passed: Boolean? = null, val reason: String = "",
    val endedAt: Long? = null
) {
    val done get() = stage == Stage.DONE
    fun remaining(clock: Clock): Long = if (mode != Mode.EXAM || done) 0 else
        ((if (clock.boot == boot) deadlineElapsed - clock.elapsed else deadlineWall - clock.wall)
            .coerceAtLeast(0))
}

@Serializable
data class QuestionProgress(
    val attempts: Int = 0, val correct: Int = 0, val streak: Int = 0,
    val lastWrong: Boolean = false, val dueAt: Long = 0
) {
    val mastered get() = streak >= 2
    fun answered(right: Boolean, now: Long): QuestionProgress {
        val nextStreak = if (right) streak + 1 else 0
        val days = listOf(0L, 1L, 3L, 7L, 14L, 30L)[nextStreak.coerceAtMost(5)]
        return copy(attempts = attempts + 1, correct = correct + if (right) 1 else 0,
            streak = nextStreak, lastWrong = !right, dueAt = now + days * 86_400_000L)
    }
}

@Serializable
data class AppState(
    val schema: Int = 1, val onboardingDone: Boolean = false,
    val theme: String = "system", val dailyGoal: Int = 20,
    val favorites: Set<String> = emptySet(),
    val progress: Map<String, QuestionProgress> = emptyMap(),
    val dailyAnswers: Map<String, Int> = emptyMap(),
    val active: Session? = null, val history: List<Session> = emptyList()
)
