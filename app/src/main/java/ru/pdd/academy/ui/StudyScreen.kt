package ru.pdd.academy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ru.pdd.academy.StudyViewModel
import ru.pdd.academy.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(vm: StudyViewModel, state: AppState, session: Session, onClose: () -> Unit) {
    val time by vm.time.collectAsStateWithLifecycle()
    val q = vm.engine.byId.getValue(session.questionIds[session.index])
    val answered = session.answers[q.id]
    var selection by rememberSaveable(session.id, q.id) { mutableStateOf<Int?>(null) }
    var leave by rememberSaveable { mutableStateOf(false) }
    var zoom by rememberSaveable(q.id) { mutableStateOf(false) }
    val exam = session.mode == Mode.EXAM
    val listState = rememberLazyListState()
    val numberState = rememberLazyListState()
    LaunchedEffect(q.id) { listState.scrollToItem(0); numberState.animateScrollToItem(session.index) }
    BackHandler { leave = true }
    if (leave) AlertDialog(onDismissRequest = { leave = false }, title = { Text(if (exam) "Выйти из экзамена?" else "Продолжить позже?") },
        text = { Text(if (exam) "Ответы сохранятся, но таймер продолжит идти. Вы сможете вернуться с главного экрана." else "Ответы и текущий вопрос сохранятся. Вернитесь к занятию с главного экрана.") },
        confirmButton = { TextButton(onClick = { leave = false; onClose() }) { Text("На главную") } },
        dismissButton = { TextButton(onClick = { leave = false }) { Text("Остаться") } })
    if (zoom && q.image != null) ZoomImage(q.image) { zoom = false }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Column {
            Text(if (exam && session.stage == Stage.EXTRA) "Дополнительные вопросы" else session.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text("Вопрос ${session.index + 1} из ${session.questionIds.size}", style = MaterialTheme.typography.labelMedium)
        } }, navigationIcon = { IconButton(onClick = { leave = true }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } },
            actions = { if (exam) {
                val seconds = (session.remaining(time) + 999) / 1000
                Text("%02d:%02d".format(seconds / 60, seconds % 60), Modifier.padding(end = 16.dp),
                    color = if (seconds < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium)
            } }) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    if (answered == null) PrimaryButton("Подтвердить ответ", { selection?.let { vm.answer(q.id, it) } }, enabled = selection != null)
                    else {
                        val next = session.questionIds.indices.firstOrNull { it > session.index && session.questionIds[it] !in session.answers }
                            ?: session.questionIds.indices.firstOrNull { session.questionIds[it] !in session.answers }
                        PrimaryButton(if (next != null) "Следующий вопрос" else "Ответы сохранены", { next?.let { vm.move(it) } }, enabled = next != null)
                    }
                }
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(state = listState, modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item { LazyRow(state = numberState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(session.questionIds, key = { _, id -> id }) { i, id ->
                        val submitted = id in session.answers
                        val canSelect = session.stage != Stage.EXTRA || i >= session.baseCount
                        FilterChip(selected = i == session.index, onClick = { vm.move(i) }, enabled = canSelect,
                            label = { Text("${i + 1}${if (submitted) " •" else ""}") }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                } }
                item { Text(q.topics.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                q.image?.let { path -> item {
                    Card(onClick = { zoom = true }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        AsyncImage("file:///android_asset/$path", "Иллюстрация к вопросу. Нажмите, чтобы увеличить.",
                            Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 260.dp), contentScale = ContentScale.Fit)
                    }
                    Text("Нажмите на изображение, чтобы увеличить", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                item { Row(verticalAlignment = Alignment.Top) {
                    Text(q.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.favorite(q.id) }) { Icon(if (q.id in state.favorites) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        if (q.id in state.favorites) "Убрать из избранного" else "В избранное", tint = MaterialTheme.colorScheme.primary) }
                } }
                itemsIndexed(q.answers) { i, answer ->
                    val reveal = answered != null && !exam
                    val correct = reveal && i == q.correct
                    val wrong = reveal && i == answered && answered != q.correct
                    val selected = i == (answered ?: selection)
                    val bg = when { correct -> MaterialTheme.colorScheme.secondaryContainer; wrong -> MaterialTheme.colorScheme.errorContainer;
                        selected -> MaterialTheme.colorScheme.primaryContainer; else -> MaterialTheme.colorScheme.surface }
                    val fg = when { correct -> MaterialTheme.colorScheme.onSecondaryContainer; wrong -> MaterialTheme.colorScheme.onErrorContainer;
                        selected -> MaterialTheme.colorScheme.onPrimaryContainer; else -> MaterialTheme.colorScheme.onSurface }
                    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().semantics { stateDescription = when { correct -> "Правильный ответ"; wrong -> "Ваш неверный ответ"; selected -> "Выбрано"; else -> "Не выбрано" } }
                            .clickable(enabled = answered == null, role = Role.RadioButton) { selection = i }) {
                        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}.", style = MaterialTheme.typography.titleMedium)
                            Text(answer, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            if (correct) Icon(Icons.Rounded.CheckCircle, "Правильный ответ")
                            else if (wrong) Icon(Icons.Rounded.Cancel, "Неверный ответ")
                            else if (selected) Icon(Icons.Rounded.RadioButtonChecked, "Выбрано")
                        }
                    }
                }
                if (answered != null) item {
                    if (exam) Text("Ответ принят. Разбор будет доступен после завершения экзамена.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else Explanation(q, answered == q.correct)
                }
            }
        }
    }
}

@Composable
fun Explanation(q: Question, correct: Boolean? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(when (correct) { true -> "Верно! Разберём почему"; false -> "Запомним на следующий раз"; null -> "Объяснение" }, style = MaterialTheme.typography.titleMedium)
            Text(q.explanation, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ZoomImage(path: String, onClose: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF101722)) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                AsyncImage("file:///android_asset/$path", "Увеличенная иллюстрация", contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else Offset(
                                (offset.x + pan.x).coerceIn(-size.width * (scale - 1) / 2, size.width * (scale - 1) / 2),
                                (offset.y + pan.y).coerceIn(-size.height * (scale - 1) / 2, size.height * (scale - 1) / 2))
                        }
                    }.graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y })
                Row(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    FilledTonalIconButton(onClick = { scale = (scale + 1f).coerceAtMost(5f) }) { Icon(Icons.Rounded.ZoomIn, "Увеличить") }
                    Spacer(Modifier.width(10.dp))
                    FilledTonalIconButton(onClick = { scale = 1f; offset = Offset.Zero }) { Icon(Icons.Rounded.ZoomOutMap, "Сбросить масштаб") }
                    Spacer(Modifier.width(10.dp))
                    FilledTonalIconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Закрыть") }
                }
            }
        }
    }
}
