package studio.nox.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import studio.nox.model.SceneCatalog
import studio.nox.model.VisualQuality

private val Context.dataStore by preferencesDataStore(name="nox_settings")
data class NoxSettings(val activeId:Long?=null,val fps:Int=60,val gyroEnabled:Boolean=true,val favoriteIds:Set<Long> = emptySet(),val quality:Int=1)
class SettingsRepository(context:Context) {
    private val store=context.applicationContext.dataStore
    private val active=longPreferencesKey("active_id")
    private val fpsKey=intPreferencesKey("fps")
    private val gyro=booleanPreferencesKey("gyro_enabled")
    private val seeded=booleanPreferencesKey("seeded")
    private val favorites=stringSetPreferencesKey("favorite_ids")
    private val qualityKey=intPreferencesKey("visual_quality")
    val settings:Flow<NoxSettings> = store.data.map {
        NoxSettings(it[active],it[fpsKey]?.takeIf { f-> f==30 || f==60 || f==120 }?:60,it[gyro]?:true,
            it[favorites]?.mapNotNull(String::toLongOrNull)?.toSet()?:emptySet(),
            VisualQuality.normalized(it[qualityKey]?:VisualQuality.RICH))
    }
    suspend fun setActive(id:Long?) {store.edit { if(id==null)it.remove(active) else it[active]=id } }
    suspend fun setQuality(value:Int) {require(value in 0..2);store.edit {it[qualityKey]=value} }
    suspend fun setFps(value:Int) { require(value in listOf(30,60,120));store.edit {it[fpsKey]=value} }
    suspend fun setGyro(value:Boolean) {store.edit {it[gyro]=value} }
    suspend fun setFavorite(id:Long,enabled:Boolean) {
        require(id>0)
        store.edit { prefs ->
            val ids=(prefs[favorites]?:emptySet()).toMutableSet()
            if(enabled) ids.add(id.toString()) else ids.remove(id.toString())
            prefs[favorites]=ids
        }
    }
    suspend fun toggleFavorite(id:Long) {
        require(id>0)
        store.edit { prefs ->
            val ids=(prefs[favorites]?:emptySet()).toMutableSet()
            if(!ids.add(id.toString())) ids.remove(id.toString())
            prefs[favorites]=ids
        }
    }
    suspend fun initializeDefaults(presets:PresetRepository) {
        if(store.data.first()[seeded]==true) return
        // Mark completion AFTER inserts, allowing recovery from interrupted initialization.
        val first=presets.save("Blue Current",SceneCatalog.default("FLUX"))
        presets.save("Orbit Violet",SceneCatalog.default("HALO"))
        presets.save("Silent Nebula",SceneCatalog.default("NEBULA"))
        store.edit {it[active]=first;it[seeded]=true}
    }
}
