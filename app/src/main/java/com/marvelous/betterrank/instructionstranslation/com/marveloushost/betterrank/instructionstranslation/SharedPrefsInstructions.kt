package com.marvelous.betterrank.instructionstranslation

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsInstructions {

    private const val PREF_NAME = "instructions_translation_prefs"

    private const val KEY_IS_RUNNING = "instr_isRunning"
    private const val KEY_COUNTDOWN = "instr_countdown"
    private const val KEY_RAW = "instr_rawText"
    private const val KEY_TRANSLATED = "instr_translatedText"
    private const val KEY_LANG = "instr_targetLanguage"
    private const val KEY_GUEST_ID = "instr_guestId"
    private const val KEY_SELECTED_INDICES = "instr_selected_indices"
    private const val KEY_SELECTED_LANGUAGE = "instr_selected_language"
    private const val KEY_READY = "instr_ready_state"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ---------- PUT ----------
    fun putBool(ctx: Context, key: String, value: Boolean) =
        prefs(ctx).edit().putBoolean(key, value).apply()

    fun putInt(ctx: Context, key: String, value: Int) =
        prefs(ctx).edit().putInt(key, value).apply()

    fun putString(ctx: Context, key: String, value: String?) =
        prefs(ctx).edit().putString(key, value).apply()

    // ---------- GET ----------
    fun getBool(ctx: Context, key: String, def: Boolean = false): Boolean =
        prefs(ctx).getBoolean(key, def)

    fun getInt(ctx: Context, key: String, def: Int = 0): Int =
        prefs(ctx).getInt(key, def)

    fun getString(ctx: Context, key: String, def: String? = null): String? =
        prefs(ctx).getString(key, def)

    fun remove(ctx: Context, key: String) =
        prefs(ctx).edit().remove(key).apply()

    fun clearAll(ctx: Context) =
        prefs(ctx).edit().clear().apply()


    // ---------- SPECIAL ----------
    fun saveState(
        ctx: Context,
        running: Boolean,
        countdown: Int,
        raw: String?,
        translated: String?,
        lang: String?,
        guestId: String?,
        selectedIndices: List<Int>,
        selectedLanguage: String?
    ) {
        prefs(ctx).edit()
            .putBoolean(KEY_IS_RUNNING, running)
            .putInt(KEY_COUNTDOWN, countdown)
            .putString(KEY_RAW, raw)
            .putString(KEY_TRANSLATED, translated)
            .putString(KEY_LANG, lang)
            .putString(KEY_GUEST_ID, guestId)
            .putString(KEY_SELECTED_INDICES, selectedIndices.joinToString(","))
            .putString(KEY_SELECTED_LANGUAGE, selectedLanguage)
            .putBoolean(KEY_READY, translated != null)

            .apply()
    }

    fun readRunning(ctx: Context) = getBool(ctx, KEY_IS_RUNNING)
    fun readCountdown(ctx: Context) = getInt(ctx, KEY_COUNTDOWN, 240)
    fun readRaw(ctx: Context) = getString(ctx, KEY_RAW)
    fun readTranslated(ctx: Context) = getString(ctx, KEY_TRANSLATED)
    fun readLang(ctx: Context) = getString(ctx, KEY_LANG)
    fun readGuestId(ctx: Context) = getString(ctx, KEY_GUEST_ID)
    fun readSelectedIndices(ctx: Context): List<Int> =
        getString(ctx, KEY_SELECTED_INDICES)?.split(",")?.mapNotNull { it.toIntOrNull() }
            ?: emptyList()

    fun readSelectedLanguage(ctx: Context) =
        getString(ctx, KEY_SELECTED_LANGUAGE)

    fun readReady(ctx: Context) = getBool(ctx, KEY_READY)



    fun clearState(ctx: Context) {
        remove(ctx, KEY_IS_RUNNING)
        remove(ctx, KEY_COUNTDOWN)
        remove(ctx, KEY_RAW)
        remove(ctx, KEY_TRANSLATED)
        remove(ctx, KEY_LANG)
        remove(ctx, KEY_GUEST_ID)
        remove(ctx, KEY_SELECTED_INDICES)
        remove(ctx, KEY_SELECTED_LANGUAGE)
        remove(ctx, KEY_READY)
    }
}