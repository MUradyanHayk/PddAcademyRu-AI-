package ru.pdd.academy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.pdd.academy.domain.*

@Composable
fun LearnScreen(state: AppState, questions: List<Question>, signs: List<RoadSign>,
                start: (List<String>, String) -> Unit, favorite: (String) -> Unit) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val topics = remember(questions) { questions.flatMap { it.topics }.distinct().sorted() }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PageTitle("Учиться", "Разбирайтесь в правилах, а не только в ответах.")
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text(if (section == 2) "Найти знак" else "Найти вопрос или тему") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Очистить поиск") } },
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Билеты", "Темы", "Знаки").forEachIndexed { i, name ->
                    FilterChip(selected = section == i, onClick = { section = i; query = "" }, label = { Text(name) })
                }
            }
        }
        val search = query.trim()
        when {
            section == 2 -> {
                val found = signs.filter { search.isEmpty() || "${it.number} ${it.title} ${it.category}".contains(search, true) }
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (found.isEmpty()) item { EmptyState("Ничего не найдено", "Попробуйте другое название или номер.", Icons.Rounded.SearchOff) }
                    items(found, key = { it.number }) { sign ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (sign.image.isNotBlank()) AsyncImage("file:///android_asset/${sign.image}", "Знак ${sign.number}: ${sign.title}",
                                    Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Fit)
                                else Text("Изображение отсутствует в исходной базе", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${sign.number} · ${sign.title}", style = MaterialTheme.typography.titleMedium)
                                Text(sign.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                if (sign.description.isNotBlank()) Text(sign.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            search.isNotEmpty() -> {
                val found = questions.filter { q -> (q.text + " " + q.topics.joinToString()).contains(search, true) }
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("Найдено: ${found.size}", style = MaterialTheme.typography.labelLarge) }
                    if (found.isEmpty()) item { EmptyState("Ничего не найдено", "Попробуйте более короткий запрос.", Icons.Rounded.SearchOff) }
                    items(found, key = { it.id }) { q ->
                        Card(onClick = { start(listOf(q.id), "Билет ${q.ticket} · вопрос ${q.number}") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Билет ${q.ticket} · ${q.number}", color = MaterialTheme.colorScheme.primary)
                                    IconButton(onClick = { favorite(q.id) }, modifier = Modifier.size(48.dp)) {
                                        Icon(if (q.id in state.favorites) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                            if (q.id in state.favorites) "Убрать из избранного" else "В избранное")
                                    }
                                }
                                Text(q.text, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            section == 0 -> LazyVerticalGrid(columns = GridCells.Adaptive(145.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items((1..40).toList()) { ticket ->
                    val group = questions.filter { it.ticket == ticket }
                    val done = group.count { state.progress[it.id]?.mastered == true }
                    Card(onClick = { start(group.map { it.id }, "Билет $ticket") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(if (done == 20) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.MenuBook,
                                null, tint = MaterialTheme.colorScheme.primary)
                            Text("Билет $ticket", style = MaterialTheme.typography.titleMedium)
                            LinearProgressIndicator(progress = { done / 20f }, modifier = Modifier.fillMaxWidth())
                            Text("Выучено $done / 20", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(topics) { topic ->
                    val group = questions.filter { topic in it.topics }
                    val done = group.count { state.progress[it.id]?.mastered == true }
                    ActionCard(topic, "Выучено $done из ${group.size}", Icons.AutoMirrored.Rounded.MenuBook,
                        { start(group.map { it.id }, topic) })
                }
            }
        }
    }
}
