package com.shoop.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore

val Context.baselineDataStore: DataStore<Preferences> by preferencesDataStore(name = "shoop_baseline")
