package com.rentmanager.app.ui.landlord.createproperty

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.draftDataStore by preferencesDataStore(name = "create_property_draft")

/** Gson-представление черновика для записи в DataStore. */
private data class StoredDraft(
    val propertyType: String = "Квартира",
    val rentType: String = "посуточно",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val step4: Map<String, Any?> = emptyMap()
)

/**
 * Персистентность черновика создания объекта (Preferences DataStore).
 * In-memory [CreateDraftHolder] остаётся источником правды для UI; сюда
 * снимок пишется на выходе из шагов флоу, а при старте приложения — восстанавливается.
 * Благодаря этому диалог «Продолжить создание?» работает и после перезапуска приложения.
 */
object CreateDraftPersistence {

    private val KEY = stringPreferencesKey("draft_json")
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null

    /** Вызывается один раз из MainActivity.onCreate. */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        scope.launch { restoreFromStore() }
    }

    /** Сохранить текущее состояние черновика (fire-and-forget). */
    fun save() {
        val ctx = appContext ?: return
        val json = gson.toJson(
            StoredDraft(
                propertyType = CreateDraftHolder.propertyType,
                rentType = CreateDraftHolder.rentType,
                address = CreateDraftHolder.address,
                latitude = CreateDraftHolder.latitude,
                longitude = CreateDraftHolder.longitude,
                step4 = CreateDraftHolder.snapshot()
            )
        )
        scope.launch {
            try {
                ctx.draftDataStore.edit { it[KEY] = json }
            } catch (_: Exception) {
            }
        }
    }

    /** Удалить сохранённый черновик. */
    fun clearStore() {
        val ctx = appContext ?: return
        scope.launch {
            try {
                ctx.draftDataStore.edit { it.remove(KEY) }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun restoreFromStore() {
        val ctx = appContext ?: return
        try {
            val json = ctx.draftDataStore.data.first()[KEY] ?: return
            val stored = gson.fromJson(json, StoredDraft::class.java) ?: return
            CreateDraftHolder.propertyType = stored.propertyType
            CreateDraftHolder.rentType = stored.rentType
            CreateDraftHolder.address = stored.address
            CreateDraftHolder.latitude = stored.latitude
            CreateDraftHolder.longitude = stored.longitude
            CreateDraftHolder.restore(stored.step4)
        } catch (_: Exception) {
        }
    }
}
