package studio.nox.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import studio.nox.data.NoxDatabase
import studio.nox.data.NoxSettings
import studio.nox.data.NoxPreset
import studio.nox.data.PresetRepository
import studio.nox.data.SettingsRepository
import studio.nox.model.SceneConfig
import studio.nox.model.PortablePreset

class NoxViewModel(application: Application): AndroidViewModel(application) {
    private val presetsRepo = PresetRepository(NoxDatabase.get(application).presetDao())
    private val settingsRepo = SettingsRepository(application)
    val presets = presetsRepo.presets.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val settings = settingsRepo.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),NoxSettings())
    var message: String? by androidx.compose.runtime.mutableStateOf(null)
        private set
    init { viewModelScope.launch { runCatching { settingsRepo.initializeDefaults(presetsRepo) }.onFailure {message=it.message} } }
    fun notify(value:String?) {message=value}
    fun save(name:String,scene:SceneConfig,id:Long=0,onSaved:(Long)->Unit={}) = viewModelScope.launch {
        runCatching { presetsRepo.save(name,scene,id) }
            .onSuccess { settingsRepo.setActive(it);onSaved(it);message="Пресет сохранён" }
            .onFailure {message=it.message?:"Не удалось сохранить"}
    }
    fun saveAndApply(name:String,scene:SceneConfig,id:Long=0,onSaved:(Long)->Unit={},onReady:()->Unit) = viewModelScope.launch {
        runCatching {
            val saved=presetsRepo.save(name,scene,id)
            settingsRepo.setActive(saved)
            saved
        }.onSuccess {onSaved(it);onReady()}.onFailure {message=it.message?:"Не удалось применить сцену"}
    }
    fun use(preset:NoxPreset) = viewModelScope.launch { settingsRepo.setActive(preset.id); message="Выбрано: ${preset.name}" }
    fun toggleFavorite(preset:NoxPreset) = viewModelScope.launch {settingsRepo.toggleFavorite(preset.id)}
    fun duplicate(preset:NoxPreset) = viewModelScope.launch {
        runCatching {presetsRepo.duplicate(preset)}.onSuccess {message="Создана копия"}.onFailure {message=it.message}
    }
    fun delete(preset:NoxPreset) = viewModelScope.launch {
        if(settings.value.activeId==preset.id)settingsRepo.setActive(null)
        presetsRepo.delete(preset.id)
        settingsRepo.setFavorite(preset.id,false)
        message="Пресет удалён"
    }
    fun importPortable(items:List<PortablePreset>)=viewModelScope.launch {
        runCatching {
            require(items.isNotEmpty()) { "Файл не содержит пресетов" }
            require(items.size<=100) { "Слишком много пресетов" }
            items.forEach { item -> presetsRepo.save(item.name,item.scene) }
        }.onSuccess {
            message="Импортировано пресетов: ${items.size}"
        }.onFailure { message=it.message?:"Не удалось импортировать пресеты" }
    }
    fun setQuality(value:Int)=viewModelScope.launch {settingsRepo.setQuality(value)}
    fun setFps(value:Int)=viewModelScope.launch {settingsRepo.setFps(value)}
    fun setGyro(enabled:Boolean)=viewModelScope.launch {settingsRepo.setGyro(enabled)}
}
