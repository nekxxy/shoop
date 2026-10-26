package com.shoop.app.data

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Local storage for manual activities and the habit journal. Offline JSON files. */
class ActivityStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val activitiesFile = File(context.filesDir, "activities.json").apply { parentFile?.mkdirs() }
    private val journalFile = File(context.filesDir, "journal.json")

    fun listActivities(): List<ManualActivity> =
        if (!activitiesFile.exists()) emptyList()
        else runCatching {
            json.decodeFromString(ListSerializer(ManualActivity.serializer()), activitiesFile.readText())
        }.getOrDefault(emptyList())

    fun saveActivities(list: List<ManualActivity>) {
        activitiesFile.writeText(json.encodeToString(ListSerializer(ManualActivity.serializer()), list))
    }

    fun loadJournal(): HabitJournal =
        if (!journalFile.exists()) HabitJournal()
        else runCatching { json.decodeFromString(HabitJournal.serializer(), journalFile.readText()) }
            .getOrDefault(HabitJournal())

    fun saveJournal(journal: HabitJournal) {
        journalFile.writeText(json.encodeToString(HabitJournal.serializer(), journal))
    }
}
