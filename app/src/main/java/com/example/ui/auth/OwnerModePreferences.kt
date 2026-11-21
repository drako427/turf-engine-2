package com.example.ui.auth

import android.content.Context

object OwnerModePreferences {
  private const val PREFS_NAME = "football_hub_owner_mode"
  private const val KEY_ENABLED = "owner_mode_enabled"
  private const val KEY_VERIFIED_CODE = "owner_verified_code"

  fun getSavedOwnerCode(context: Context): String? {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (!prefs.getBoolean(KEY_ENABLED, false)) return null
    return prefs.getString(KEY_VERIFIED_CODE, null)?.takeIf { it.isNotBlank() }
  }

  fun saveOwnerMode(context: Context, verifiedCode: String) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs
      .edit()
      .putBoolean(KEY_ENABLED, true)
      .putString(KEY_VERIFIED_CODE, verifiedCode.trim())
      .apply()
  }

  fun clearOwnerMode(context: Context) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().remove(KEY_ENABLED).remove(KEY_VERIFIED_CODE).apply()
  }
}
