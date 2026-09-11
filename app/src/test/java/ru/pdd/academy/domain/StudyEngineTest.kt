package ru.pdd.academy.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class StudyEngineTest {
    private val bank = (1..8).flatMap { ticket -> (1..20).map { n ->
        Question("$ticket-$n", "$ticket-$n", ticket, n, "Question", listOf("yes", "no"), 0, "Explanation", listOf("Topic"))
    } }
    private val engine = StudyEngine(bank)
    private val clock = Clock(1_000_000, 2_000, 1)
    private fun exam() = engine.exam(clock, Random(2))
    private fun submitBase(s: Session, wrongPositions: Set<Int>): Session {
        var result = s
        s.questionIds.forEachIndexed { i, id ->
            result = engine.answer(result, id, if (i in wrongPositions) 1 else 0, clock, Random(3))
        }
        return result
    }
    @Test fun examHasFourCompleteGroups() {
        repeat(40) { seed ->
            val s = engine.exam(clock, Random(seed))
            assertEquals(20, s.questionIds.distinct().size)
            assertEquals(listOf(5,5,5,5), s.questionIds.groupingBy { engine.byId.getValue(it).block }.eachCount().toSortedMap().values.toList())
            for (block in 0..3) assertEquals(1, s.questionIds.map { engine.byId.getValue(it) }.filter { it.block == block }.map { it.ticket }.distinct().size)
        }
    }
    @Test fun perfectExamPasses() { val s = submitBase(exam(), emptySet()); assertTrue(s.done); assertEquals(true, s.passed) }
    @Test fun oneErrorAddsFiveQuestionsAndFreshFiveMinutes() {
        val s = submitBase(exam(), setOf(0))
        assertEquals(Stage.EXTRA, s.stage); assertEquals(25, s.questionIds.size)
        assertEquals(300_000L, s.remaining(clock)); assertEquals(20, s.index)
        assertTrue(s.questionIds.drop(20).all { engine.byId.getValue(it).block == 0 })
        assertEquals(25, s.questionIds.distinct().size)
    }
    @Test fun twoErrorsInDifferentGroupsAddTen() {
        val s = submitBase(exam(), setOf(0, 6))
        assertEquals(30, s.questionIds.size); assertEquals(600_000L, s.remaining(clock))
    }
    @Test fun twoErrorsInSameGroupFailImmediately() {
        val s = exam(); val a = engine.answer(s, s.questionIds[0], 1, clock)
        val b = engine.answer(a, s.questionIds[1], 1, clock)
        assertTrue(b.done); assertEquals(false, b.passed); assertEquals(2, b.answers.size)
    }
    @Test fun threeErrorsAcrossGroupsFail() {
        var s = exam()
        listOf(0, 5, 10).forEach { s = engine.answer(s, s.questionIds[it], 1, clock) }
        assertTrue(s.done); assertEquals(false, s.passed)
    }
    @Test fun extraErrorFails() {
        val s = submitBase(exam(), setOf(0))
        val failed = engine.answer(s, s.questionIds[20], 1, clock)
        assertTrue(failed.done); assertEquals(false, failed.passed)
    }
    @Test fun correctExtrasPass() {
        var s = submitBase(exam(), setOf(0, 7))
        s.questionIds.drop(20).forEach { s = engine.answer(s, it, 0, clock) }
        assertTrue(s.done); assertEquals(true, s.passed)
    }
    @Test fun doubleTapDoesNotChangeConfirmedAnswer() {
        val s = exam(); val a = engine.answer(s, s.questionIds[0], 0, clock)
        assertEquals(a, engine.answer(a, s.questionIds[0], 1, clock)); assertEquals(1, a.answers.size)
    }
    @Test fun invalidAnswerIsIgnored() {
        val s = exam(); assertEquals(s, engine.answer(s, s.questionIds[0], 99, clock))
        assertEquals(s, engine.answer(s, "unknown", 0, clock))
    }
    @Test fun timeoutBeforeAnswerDoesNotCreditIt() {
        val s = exam(); val late = clock.copy(elapsed = clock.elapsed + 1_200_000)
        val result = engine.answer(s, s.questionIds[0], 0, late)
        assertTrue(result.done); assertEquals(false, result.passed); assertTrue(result.answers.values.all { it == -1 })
    }
    @Test fun wallClockChangeCannotExtendExamWithinSameBoot() {
        val s = exam(); assertEquals(1_199_000L, s.remaining(clock.copy(wall = 0, elapsed = clock.elapsed + 1_000)))
    }
    @Test fun rebootFallsBackToWallDeadline() {
        assertEquals(1_000_000L, exam().remaining(clock.copy(boot = 2, elapsed = 1, wall = clock.wall + 200_000)))
    }
    @Test fun expiredExtraStageFails() {
        val s = submitBase(exam(), setOf(0)); assertEquals(false, engine.expire(s, clock.copy(elapsed = clock.elapsed + 300_000)).passed)
    }
    @Test fun practiceIsUntimedAndAllowsMistakes() {
        val s = engine.learn(bank.take(2).map { it.id }, "Learn", clock)
        val a = engine.answer(s, s.questionIds[0], 1, clock.copy(elapsed = Long.MAX_VALUE))
        val b = engine.answer(a, s.questionIds[1], 0, clock)
        assertTrue(b.done); assertNull(b.passed)
    }
    @Test fun progressRequiresConsecutiveCorrectAnswers() {
        val a = QuestionProgress().answered(true, 0).answered(true, 0)
        assertTrue(a.mastered); assertEquals(3 * 86_400_000L, a.dueAt)
        val b = a.answered(false, 2); assertFalse(b.mastered); assertTrue(b.lastWrong); assertEquals(2L, b.dueAt)
    }
    @Test fun stateRoundTripPreservesActiveExamAndFavorites() {
        val s = exam(); val state = AppState(favorites = setOf(s.questionIds[0]), active = engine.answer(s, s.questionIds[0], 0, clock))
        assertEquals(state, Json.decodeFromString<AppState>(Json.encodeToString(state)))
    }
    @Test fun finishedExamCannotBeChanged() {
        val s = submitBase(exam(), emptySet()); assertEquals(s, engine.answer(s, s.questionIds[0], 1, clock))
    }
    @Test fun oneUnansweredQuestionAtTimeoutGetsExtraBlock() {
        var s = exam()
        s.questionIds.drop(1).forEach { s = engine.answer(s, it, 0, clock) }
        val next = engine.expire(s, clock.copy(elapsed = clock.elapsed + 1_200_000))
        assertEquals(Stage.EXTRA, next.stage); assertEquals(-1, next.answers[next.questionIds[0]]); assertEquals(25, next.questionIds.size)
    }
    @Test fun bundledBankIsCompleteAndAllExtraBlocksAreAvailable() {
        val file = listOf(File("src/main/assets/questions.json"), File("app/src/main/assets/questions.json")).first { it.exists() }
        val actual = Json.decodeFromString<List<Question>>(file.readText())
        assertEquals(800, actual.size); assertEquals(40, actual.map { it.ticket }.distinct().size)
        assertTrue(actual.groupBy { it.ticket }.values.all { it.size == 20 })
        val real = StudyEngine(actual)
        repeat(40) { seed ->
            val base = real.exam(clock, Random(seed))
            for (block in 0..3) {
                var s = base
                base.questionIds.forEachIndexed { index, id ->
                    val q = real.byId.getValue(id)
                    val choice = if (index == block * 5) (q.correct + 1) % q.answers.size else q.correct
                    s = real.answer(s, id, choice, clock, Random(seed))
                }
                assertEquals(Stage.EXTRA, s.stage); assertEquals(25, s.questionIds.size)
                val old = base.questionIds.map { real.byId.getValue(it).sourceId }.toSet()
                assertTrue(s.questionIds.drop(20).none { real.byId.getValue(it).sourceId in old })
            }
        }
    }
}
