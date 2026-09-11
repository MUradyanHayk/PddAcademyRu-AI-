package ru.pdd.academy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pdd.academy.domain.*

private val Context.studyDataStore by preferencesDataStore(name = "study_v1")

class StudyRepository(private val context: Context) {
    private val key = stringPreferencesKey("state")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    suspend fun loadQuestions(): List<Question> = withContext(Dispatchers.IO) {
        context.assets.open("questions.json").bufferedReader().use { json.decodeFromString(it.readText()) }
    }
    suspend fun loadSigns(): List<RoadSign> = withContext(Dispatchers.IO) {
        context.assets.open("signs.json").bufferedReader().use { json.decodeFromString(it.readText()) }
    }
    suspend fun loadState(): AppState = context.studyDataStore.data.first()[key]?.let {
        json.decodeFromString<AppState>(it).also { state -> require(state.schema == 1) }
    } ?: AppState()
    suspend fun save(state: AppState) {
        val encoded = withContext(Dispatchers.Default) { json.encodeToString(state) }
        context.studyDataStore.edit { it[key] = encoded }
    }
}
