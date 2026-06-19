package com.example.counterclicker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.clickerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clicker_prefs"
)

class CounterRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val Count = intPreferencesKey("count")
    }

    val countFlow: Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[Keys.Count] ?: 0
        }

    suspend fun setCount(value: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.Count] = value.coerceAtLeast(0)
        }
    }

    suspend fun increment() {
        dataStore.edit { preferences ->
            val current = preferences[Keys.Count] ?: 0
            preferences[Keys.Count] =
                if (current == Int.MAX_VALUE) Int.MAX_VALUE else current + 1
        }
    }

    suspend fun decrement() {
        dataStore.edit { preferences ->
            val current = preferences[Keys.Count] ?: 0
            preferences[Keys.Count] = maxOf(0, current - 1)
        }
    }
}
