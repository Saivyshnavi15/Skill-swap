package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "skillswap_prefs")

class UserPreferencesDataStore(private val context: Context) {

  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val COMM_LANGUAGE = stringPreferencesKey("comm_language")
  }

  val onboardingCompleted: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

  val appLanguage: Flow<AppLanguage> =
    context.dataStore.data.map { prefs -> AppLanguage.fromCode(prefs[Keys.APP_LANGUAGE] ?: "en") }

  val commLanguage: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[Keys.COMM_LANGUAGE] ?: "English" }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETED] = completed }
  }

  suspend fun setAppLanguage(language: AppLanguage) {
    context.dataStore.edit { prefs -> prefs[Keys.APP_LANGUAGE] = language.code }
  }

  suspend fun setCommLanguage(languageName: String) {
    context.dataStore.edit { prefs -> prefs[Keys.COMM_LANGUAGE] = languageName }
  }
}
