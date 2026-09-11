@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ru.pdd.academy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.pdd.academy.domain.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun Onboarding(onDone: (Int) -> Unit) {
    var goal by rememberSaveable { mutableIntStateOf(20) }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { Spacer(Modifier.height(24.dp)); Icon(Icons.Rounded.DirectionsCar, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary) }
        item { PageTitle("Уверенно\nза рулём", "ПДД Академия · Россия · категория A/B") }
        item { Text("Понимайте правила, разбирайте ошибки и проверяйте себя. В удобном темпе, даже без интернета.", style = MaterialTheme.typography.bodyLarge) }
        item { ActionCard("Учитесь с объяснениями", "40 билетов и 800 вопросов", Icons.AutoMirrored.Rounded.MenuBook, {}) }
        item { ActionCard("Повторяйте сложное", "Ошибки и вопросы, которые пора освежить", Icons.Rounded.AutoAwesome, {}) }
        item { SectionTitle("Ваша цель на день") }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(10, 20, 40).forEach { n -> FilterChip(selected = goal == n, onClick = { goal = n }, label = { Text("$n вопросов") }) }
        } }
        item { PrimaryButton("Начать обучение", { onDone(goal) }) }
        item { Text("Учебный тренажёр, не сервис Госавтоинспекции. База из открытого источника; актуальность редакции требует проверки.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun HomeScreen(state: AppState, questions: List<Question>, start: (List<String>, String) -> Unit,
               resume: () -> Unit, learn: () -> Unit, exam: () -> Unit) {
    val today = state.dailyAnswers[LocalDate.now().toString()] ?: 0
    val mastered = state.progress.values.count { it.mastered }
    val practice = remember(state.progress, state.dailyGoal, today) { StudyEngine(questions).practiceIds(state, System.currentTimeMillis(), if (today < state.dailyGoal) state.dailyGoal - today else state.dailyGoal) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ПДД АКАДЕМИЯ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            SuggestionChip(onClick = {}, label = { Text("Россия · A/B") })
        } }
        item { PageTitle("Каждый день\nближе к цели", "Небольшое занятие — уверенный шаг вперёд.") }
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF164ECB), Color(0xFF2774ED))), RoundedCornerShape(28.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ПЛАН НА СЕГОДНЯ", color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Rounded.WbSunny, null, tint = Color.White)
                }
                Text(if (today >= state.dailyGoal) "Цель достигнута!" else "${(state.dailyGoal - today).coerceAtLeast(0)} вопросов\nдо вашей цели", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                LinearProgressIndicator(progress = { (today.toFloat()/state.dailyGoal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(), color = Color.White, trackColor = Color.White.copy(alpha = .24f))
                Text("Сегодня: $today / ${state.dailyGoal}", color = Color.White.copy(alpha = .9f))
                Button(onClick = { start(practice, "План на день") }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF164ECB)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (today >= state.dailyGoal) "Ещё немного практики" else "Начать занятие") }
            }
        }
        state.active?.let { s -> item {
            ActionCard(if (s.done) "Посмотреть результат" else "Продолжить занятие", "${s.title} · ${s.answers.size} из ${s.questionIds.size}", Icons.Rounded.PlayCircle, resume)
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("$mastered / 800", "Выучено вопросов", Modifier.weight(1f))
            StatCard("${studyStreak(state)}", "Дней подряд", Modifier.weight(1f))
        } }
        item { SectionTitle("Ваш следующий шаг") }
        item { ActionCard("Билеты и темы", "Изучайте правила с пояснениями", Icons.AutoMirrored.Rounded.MenuBook, learn) }
        item { ActionCard("Проверить себя", "Экзамен · 20 вопросов · 20 минут", Icons.Rounded.Timer, exam) }
        val errors = questions.filter { state.progress[it.id]?.lastWrong == true }.map { it.id }
        if (errors.isNotEmpty()) item { ActionCard("Разобрать ошибки", "Закрепите то, что пока не получилось", Icons.Rounded.Replay, { start(errors, "Работа над ошибками") }, trailing = "${errors.size}") }
        item { Text("Выученным считается вопрос с двумя правильными ответами подряд. Это показатель практики, а не гарантия сдачи экзамена.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

fun studyStreak(state: AppState): Int {
    var date = LocalDate.now()
    if ((state.dailyAnswers[date.toString()] ?: 0) == 0) date = date.minusDays(1)
    var count = 0
    while ((state.dailyAnswers[date.toString()] ?: 0) > 0) { count++; date = date.minusDays(1) }
    return count
}

@Composable
fun PracticeScreen(state: AppState, questions: List<Question>, start: (List<String>, String) -> Unit, exam: () -> Unit) {
    var explainExam by rememberSaveable { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val errors = questions.filter { state.progress[it.id]?.lastWrong == true }.map { it.id }
    val due = questions.filter { state.progress[it.id]?.let { p -> p.dueAt <= now } == true }.map { it.id }
    val starred = questions.filter { it.id in state.favorites }.map { it.id }
    if (explainExam) AlertDialog(onDismissRequest = { explainExam = false }, title = { Text("Готовы проверить себя?") },
        text = { Text("20 вопросов за 20 минут.\n\nОдна ошибка — 5 дополнительных вопросов за 5 минут. Две ошибки в разных блоках — 10 вопросов за 10 минут.\n\nТри ошибки, две в одном блоке или ошибка в дополнительном блоке завершают экзамен. Подсказки доступны после результата.\n\nПри сворачивании приложения таймер продолжает идти.", modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { explainExam = false; exam() }) { Text("Начать экзамен") } },
        dismissButton = { TextButton(onClick = { explainExam = false }) { Text("Позже") } })
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("Практика", "Выберите, над чем поработаем сегодня.") }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Rounded.Verified, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Репетиция экзамена", style = MaterialTheme.typography.headlineMedium)
                Text("Случайный билет, таймер и дополнительные блоки за ошибки.")
                PrimaryButton("Начать экзамен", { explainExam = true })
            }
        } }
        item { ActionCard("Быстрая тренировка", "20 случайных вопросов с объяснениями", Icons.Rounded.Bolt,
            { start(questions.shuffled().take(20).map { it.id }, "Быстрая тренировка") }) }
        item { ActionCard("Работа над ошибками", if (errors.isEmpty()) "Пока ошибок нет — начните тренировку" else "Вопросы с последним неверным ответом", Icons.Rounded.Replay,
            { if (errors.isNotEmpty()) start(errors, "Работа над ошибками") }, trailing = "${errors.size}") }
        item { ActionCard("Пора повторить", if (due.isEmpty()) "Все повторения на сегодня выполнены" else "Закрепляйте знания через интервалы", Icons.Rounded.Schedule,
            { if (due.isNotEmpty()) start(due.take(40), "Пора повторить") }, trailing = "${due.size}") }
        item { ActionCard("Избранное", if (starred.isEmpty()) "Добавляйте вопросы звёздочкой" else "Вопросы, которые вы сохранили", Icons.Rounded.Star,
            { if (starred.isNotEmpty()) start(starred, "Избранное") }, trailing = "${starred.size}") }
        item { ActionCard("Марафон", "Все 800 вопросов · можно продолжить позже", Icons.Rounded.Route,
            { start(questions.map { it.id }, "Марафон") }) }
    }
}

@Composable
fun ProgressScreen(state: AppState, questions: List<Question>, theme: (String) -> Unit, goal: (Int) -> Unit,
                   reset: () -> Unit, review: (Session) -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    val total = state.progress.values.sumOf { it.attempts }
    val correct = state.progress.values.sumOf { it.correct }
    val topics = remember(questions) { questions.flatMap { it.topics }.distinct().sorted() }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Сбросить прогресс?") },
        text = { Text("Будут удалены результаты, избранное и текущее занятие. Это действие нельзя отменить.") },
        confirmButton = { TextButton(onClick = { reset(); confirmReset = false }) { Text("Сбросить", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } })
    if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text("ПДД Академия · 0.1.0") },
        text = { Text("Учебное приложение для категории A/B.\n\nИсточник вопросов и знаков: github.com/etspring/pdd_russia. Загружено 07.09.2026. Актуальность всех материалов не подтверждена. Приложение не связано с Госавтоинспекцией.\n\nПрогресс хранится на устройстве. Регистрации и рекламы нет. Удаление приложения удаляет прогресс.") },
        confirmButton = { TextButton(onClick = { about = false }) { Text("Понятно") } })
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageTitle("Ваш прогресс", "Замечайте результат каждого занятия.") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(if (total == 0) "—" else "${correct * 100 / total}%", "Верных ответов", Modifier.weight(1f))
            StatCard("${state.progress.size}", "Из 800 изучали", Modifier.weight(1f))
        } }
        item { SectionTitle("Последние 7 дней") }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                (6 downTo 0).forEach { offset ->
                    val date = LocalDate.now().minusDays(offset.toLong())
                    val count = state.dailyAnswers[date.toString()] ?: 0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count.toString(), fontWeight = FontWeight.Bold, color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(7.dp))
                        Text(date.format(DateTimeFormatter.ofPattern("dd.MM")), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } }
        item { SectionTitle("Темы для внимания") }
        val weak = topics.map { name ->
            val group = questions.filter { name in it.topics }
            val mastered = group.count { state.progress[it.id]?.mastered == true }
            Triple(name, mastered, group.size)
        }.sortedBy { it.second.toFloat()/it.third }.take(5)
        items(weak) { (name, mastered, count) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$name · $mastered / $count", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { mastered.toFloat()/count }, modifier = Modifier.fillMaxWidth())
            }
        }
        item { SectionTitle("История занятий") }
        if (state.history.isEmpty()) item { Text("Здесь появятся завершённые занятия.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.history.take(15), key = { it.id }) { session ->
            val date = Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM · HH:mm"))
            ActionCard(session.title, "$date · ${session.answers.size} ответов", if (session.passed == true) Icons.Rounded.CheckCircle else Icons.Rounded.History, { review(session) })
        }
        item { SectionTitle("Настройки") }
        item { Text("Оформление", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "Авто", "light" to "Светлое", "dark" to "Тёмное").forEach { (key, label) ->
                    FilterChip(selected = state.theme == key, onClick = { theme(key) }, label = { Text(label) })
                }
            }
        }
        item { Text("Вопросов в день", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf(10,20,40).forEach { n ->
                FilterChip(selected = state.dailyGoal == n, onClick = { goal(n) }, label = { Text("$n") })
            } }
        }
        item { OutlinedButton(onClick = { about = true }, modifier = Modifier.fillMaxWidth()) { Text("О приложении и материалах") } }
        item { TextButton(onClick = { confirmReset = true }) { Text("Сбросить прогресс", color = MaterialTheme.colorScheme.error) } }
    }
}
