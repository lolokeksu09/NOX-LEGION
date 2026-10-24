package studio.nox.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import studio.nox.data.NoxDatabase
import studio.nox.data.NoxSettings
import studio.nox.data.PresetRepository
import studio.nox.data.SettingsRepository
import studio.nox.model.SceneCatalog
import studio.nox.renderer.EglSession
import studio.nox.renderer.MotionSensor

class NoxWallpaperService: WallpaperService() {
    override fun onCreateEngine():Engine=NoxEngine()
    inner class NoxEngine:Engine() {
        private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        private val motion=MotionSensor(this@NoxWallpaperService) { x,y -> session?.setTilt(x,y) }
        private val repo=PresetRepository(NoxDatabase.get(applicationContext).presetDao())
        private val settings=SettingsRepository(applicationContext)
        private var session:EglSession?=null
        private var visible=false
        private var last=NoxSettings()
        private var collectJob:Job?=null
        override fun onCreate(surfaceHolder:SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            collectJob=scope.launch {
                combine(settings.settings,repo.presets) { prefs,list ->
                    Pair(prefs,list.firstOrNull { it.id==prefs.activeId }?.scene?:SceneCatalog.default("FLUX"))
                }.collect { (prefs,scene) ->
                    last=prefs
                    session?.setConfig(scene)
                    session?.setRequestedFps(prefs.fps)
                    session?.setQuality(prefs.quality)
                    updateSensor()
                }
            }
        }
        override fun onSurfaceCreated(holder:SurfaceHolder) {
            super.onSurfaceCreated(holder)
            val s=EglSession(this@NoxWallpaperService)
            session=s
            s.setQuality(last.quality)
            s.start(holder.surface,holder.surfaceFrame.width(),holder.surfaceFrame.height())
            s.setVisible(visible)
        }
        override fun onSurfaceChanged(holder:SurfaceHolder,format:Int,width:Int,height:Int) {
            super.onSurfaceChanged(holder,format,width,height)
            session?.resize(width,height)
        }
        override fun onVisibilityChanged(isVisible:Boolean) {
            super.onVisibilityChanged(isVisible)
            visible=isVisible
            session?.setVisible(isVisible)
            updateSensor()
        }
        private fun updateSensor() { if(visible&&last.gyroEnabled)motion.start() else motion.stop() }
        override fun onTouchEvent(event:MotionEvent) {
            super.onTouchEvent(event)
            val width=surfaceHolder.surfaceFrame.width().coerceAtLeast(1)
            val height=surfaceHolder.surfaceFrame.height().coerceAtLeast(1)
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE -> session?.setTouch(event.x/width,1f-event.y/height)
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> session?.setTouch(-1f,-1f)
            }
        }
        override fun onSurfaceDestroyed(holder:SurfaceHolder) {
            session?.close();session=null
            super.onSurfaceDestroyed(holder)
        }
        override fun onDestroy() {
            motion.stop(); collectJob?.cancel();scope.cancel()
            session?.close();session=null
            super.onDestroy()
        }
    }
}
