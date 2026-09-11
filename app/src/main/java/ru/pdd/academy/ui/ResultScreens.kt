package ru.pdd.academy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.pdd.academy.domain.*

@Composable
fun ResultScreen(s: Session, questions: Map<String, Question>, onReview: () -> Unit,
                 onClose: () -> Unit, onRetryErrors: (List<String>) -> Unit) {
    BackHandler(onBack = onClose)
    val correct = s.answers.count { (id, choice) -> questions[id]?.correct == choice }
    val errors = s.answers.filter { (id, choice) -> questions[id]?.correct != choice }.keys.toList()
    val successful = s.passed == true || (s.mode == Mode.LEARN && errors.isEmpty() && s.answers.size == s.questionIds.size)
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { Spacer(Modifier.height(22.dp)) }
        item { Icon(if (successful) Icons.Rounded.EmojiEvents else Icons.Rounded.Insights, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary) }
        item { PageTitle(if (s.mode == Mode.EXAM) { if (s.passed == true) "Экзамен сдан!" else "Попробуем ещё" } else "Шаг вперёд!", s.reason) }
        item { Text(if (successful) "Отличная работа. Регулярное повторение поможет сохранить знания." else "Ошибки показывают, что стоит повторить. Разберите объяснения и закрепите результат.", style = MaterialTheme.typography.bodyLarge) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("$correct", "Правильно", Modifier.weight(1f))
            StatCard("${errors.size}", "Ошибок", Modifier.weight(1f))
        } }
        item { Text("Отвечено ${s.answers.values.count { it >= 0 }} из ${s.questionIds.size}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { PrimaryButton("Разобрать ответы", onReview) }
        if (errors.isNotEmpty()) item { OutlinedButton(onClick = { onRetryErrors(errors) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Повторить ошибки") } }
        item { TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("На главную") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(s: Session, questions: Map<String, Question>, state: AppState,
                 favorite: (String) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var onlyErrors by rememberSaveable(s.id) { mutableStateOf(false) }
    var image by remember { mutableStateOf<String?>(null) }
    image?.let { ZoomImage(it) { image = null } }
    val ids = s.questionIds.filter { !onlyErrors || s.answers[it] != questions[it]?.correct }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Разбор ответов") }, navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
        }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { FilterChip(selected = onlyErrors, onClick = { onlyErrors = !onlyErrors }, label = { Text("Ошибки и пропущенные") }) }
            if (ids.isEmpty()) item { EmptyState("Всё правильно", "В этом занятии нет ошибок.", Icons.Rounded.CheckCircle) }
            items(ids, key = { it }) { id ->
                val q = questions.getValue(id)
                val chosen = s.answers[id]
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Билет ${q.ticket} · вопрос ${q.number}", Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { favorite(id) }) { Icon(if (id in state.favorites) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                if (id in state.favorites) "Убрать из избранного" else "В избранное") }
                        }
                        q.image?.let { path -> Card(onClick = { image = path }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            AsyncImage("file:///android_asset/$path", "Иллюстрация к вопросу. Увеличить", Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
                        } }
                        Text(q.text, style = MaterialTheme.typography.titleMedium)
                        Text(if (chosen == null || chosen < 0) "Вы пропустили этот вопрос" else "Ваш ответ: ${q.answers[chosen]}",
                            color = if (chosen == q.correct) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                        Text("Правильный ответ: ${q.answers[q.correct]}", style = MaterialTheme.typography.bodyLarge)
                        Explanation(q)
                    }
                }
            }
        }
    }
}
