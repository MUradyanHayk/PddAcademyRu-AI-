package ru.pdd.academy.domain

import java.util.UUID
import kotlin.random.Random

/** Pure, deterministic transitions except for explicit random selection. No Android dependency. */
class StudyEngine(val questions: List<Question>) {
    val byId = questions.associateBy { it.id }
    private val tickets = questions.groupBy { it.ticket }
    init {
        require(byId.size == questions.size)
        require(questions.all { it.answers.size >= 2 && it.correct in it.answers.indices && it.block in 0..3 })
    }

    fun learn(ids: List<String>, title: String, clock: Clock): Session {
        require(ids.isNotEmpty() && ids.distinct().size == ids.size && ids.all { it in byId })
        return Session(UUID.randomUUID().toString(), title, Mode.LEARN, ids, startedAt = clock.wall)
    }

    fun exam(clock: Clock, random: Random = Random.Default): Session {
        val ids = (0..3).flatMap { block ->
            tickets.values.filter { list -> list.count { it.block == block } == 5 }
                .random(random).filter { it.block == block }.sortedBy { it.number }.map { it.id }
        }
        return Session(UUID.randomUUID().toString(), "Экзамен", Mode.EXAM, ids,
            startedAt = clock.wall, deadlineWall = clock.wall + 1_200_000,
            deadlineElapsed = clock.elapsed + 1_200_000, boot = clock.boot)
    }

    fun expire(s: Session, clock: Clock, random: Random = Random.Default): Session {
        if (s.mode != Mode.EXAM || s.done || s.remaining(clock) > 0) return s
        if (s.stage == Stage.EXTRA) return finish(s, false, "Время вышло", clock)
        val unanswered = s.questionIds.filter { it !in s.answers }.associateWith { -1 }
        return evaluate(s.copy(answers = s.answers + unanswered), clock, random)
    }

    fun answer(session: Session, id: String, choice: Int, clock: Clock,
               random: Random = Random.Default): Session {
        val s = expire(session, clock, random)
        if (s != session) return s
        if (s.done || id !in s.questionIds || id in s.answers) return s
        val q = byId.getValue(id)
        if (choice !in q.answers.indices) return s
        val position = s.questionIds.indexOf(id)
        if (s.stage == Stage.EXTRA && position < s.baseCount) return s
        var next = s.copy(answers = s.answers + (id to choice))
        if (s.mode == Mode.LEARN) {
            if (next.answers.size == next.questionIds.size)
                next = finish(next, null, "Занятие завершено", clock)
            return next
        }
        if (s.stage == Stage.EXTRA && choice != q.correct)
            return finish(next, false, "Ошибка в дополнительном блоке", clock)
        return evaluate(next, clock, random)
    }

    private fun evaluate(next: Session, clock: Clock, random: Random): Session {
        val wrong = next.questionIds.take(next.baseCount).filter { key ->
            next.answers[key]?.let { it != byId.getValue(key).correct } == true
        }
        if (wrong.size >= 3) return finish(next, false, "Три и более ошибки в основном билете", clock)
        if (wrong.groupingBy { byId.getValue(it).block }.eachCount().any { it.value >= 2 })
            return finish(next, false, "Две ошибки в одном блоке", clock)
        if (next.answers.size != next.questionIds.size) return next
        if (wrong.isEmpty() || next.stage == Stage.EXTRA)
            return finish(next, true, "Экзамен сдан", clock)
        val extras = wrong.flatMap { key ->
            val block = byId.getValue(key).block
            val usedSourceIds = next.questionIds.map { byId.getValue(it).sourceId }.toSet()
            val candidates = tickets.values.map { list -> list.filter { it.block == block }.sortedBy { it.number } }
                .filter { list -> list.size == 5 && list.none { it.id in next.questionIds || it.sourceId in usedSourceIds } }
            require(candidates.isNotEmpty()) { "No unused additional block for group $block" }
            candidates.random(random).map { it.id }
        }
        val duration = wrong.size * 300_000L
        return next.copy(stage = Stage.EXTRA, questionIds = next.questionIds + extras,
            index = next.baseCount, deadlineWall = clock.wall + duration,
            deadlineElapsed = clock.elapsed + duration, boot = clock.boot)
    }

    fun abandon(s: Session, clock: Clock) = finish(s, if (s.mode == Mode.EXAM) false else null,
        "Занятие завершено досрочно", clock)

    private fun finish(s: Session, passed: Boolean?, reason: String, clock: Clock) =
        s.copy(stage = Stage.DONE, passed = passed, reason = reason, endedAt = clock.wall)

    fun practiceIds(state: AppState, now: Long, limit: Int = 20): List<String> = questions
        .sortedWith(compareBy<Question> {
            val p = state.progress[it.id]
            when { p?.lastWrong == true -> 0; p != null && p.dueAt <= now -> 1; p == null -> 2; else -> 3 }
        }.thenBy { state.progress[it.id]?.dueAt ?: 0L }.thenBy { it.ticket }.thenBy { it.number })
        .take(limit).map { it.id }
}
