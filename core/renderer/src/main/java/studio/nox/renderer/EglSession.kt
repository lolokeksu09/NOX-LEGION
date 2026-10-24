package studio.nox.renderer

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import studio.nox.model.FrameBudget
import studio.nox.model.SceneConfig
import studio.nox.model.VisualQuality
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** All EGL operations are confined to the dedicated HandlerThread. */
class EglSession(private val context: Context, private val onError: (String)->Unit = {}) {
    private val thread=HandlerThread("NOX EGL").apply { start() }
    private val handler=Handler(thread.looper)
    private var backend: GlBackend?=null
    private var cfg=SceneConfig("FLUX")
    private var fps=60
    private var quality=VisualQuality.RICH
    private var effectiveQuality=VisualQuality.RICH
    private var visible=false
    private var active=true
    private var tiltX=0f; private var tiltY=0f
    private var touchX=-1f; private var touchY=-1f
    private var start=SystemClock.elapsedRealtimeNanos()
    private var lastPowerCheck=0L
    private var actualFps=60
    private val powerManager=context.getSystemService(PowerManager::class.java)
    private val frame=object:Runnable {
        override fun run() {
            if(!active || !visible || backend==null) return
            try {
                val now=SystemClock.uptimeMillis()
                if(now-lastPowerCheck>2000L) {
                    actualFps=FrameBudget.allowed(fps,powerManager?.isPowerSaveMode==true,if(android.os.Build.VERSION.SDK_INT>=29)powerManager?.currentThermalStatus?:0 else 0)
                    effectiveQuality=VisualQuality.effective(quality,powerManager?.isPowerSaveMode==true,if(android.os.Build.VERSION.SDK_INT>=29)powerManager?.currentThermalStatus?:0 else 0)
                    lastPowerCheck=now
                }
                backend?.draw(cfg,(SystemClock.elapsedRealtimeNanos()-start)/1e9f,tiltX,tiltY,touchX,touchY,effectiveQuality)
                backend?.swap()
            } catch(t:Throwable) {
                Log.e("NOX","GL frame failed",t)
                onError(t.message?:"Ошибка рендеринга")
                visible=false
                return
            }
            handler.postDelayed(this,((1000L + actualFps.coerceAtLeast(1) - 1) / actualFps.coerceAtLeast(1)))
        }
    }
    fun start(surface: Surface,w:Int,h:Int) = handler.post {
        if(!active)return@post
        try { backend?.finish();backend=GlBackend.create(surface,w,h);start=SystemClock.elapsedRealtimeNanos();schedule() }
        catch(t:Throwable){ Log.e("NOX","Cannot create GLES",t);onError(t.message?:"Ошибка OpenGL") }
    }
    fun resize(w:Int,h:Int)=handler.post { backend?.resize(w,h) }
    fun setConfig(config:SceneConfig)=handler.post { cfg=config.normalized() }
    fun setRequestedFps(value:Int)=handler.post { fps=value;lastPowerCheck=0L }
    fun setQuality(value:Int)=handler.post { quality=VisualQuality.normalized(value);lastPowerCheck=0L }
    fun setTilt(x:Float,y:Float)=handler.post { tiltX=x.coerceIn(-1f,1f);tiltY=y.coerceIn(-1f,1f) }
    fun setTouch(x:Float,y:Float)=handler.post { touchX=x;touchY=y }
    fun setVisible(value:Boolean)=handler.post { visible=value;if(value){lastPowerCheck=0L;schedule()} else handler.removeCallbacks(frame) }
    private fun schedule() { handler.removeCallbacks(frame);if(active&&visible&&backend!=null)handler.post(frame) }
    fun close() {
        val done=CountDownLatch(1)
        handler.post {
            visible=false;active=false;handler.removeCallbacks(frame)
            try { backend?.finish();backend=null } catch(t:Throwable){Log.e("NOX","EGL cleanup",t)}
            finally { done.countDown();thread.quitSafely() }
        }
        // Surface can be destroyed immediately after callback; attempt synchronous EGL release.
        if(android.os.Looper.myLooper()!=thread.looper) done.await(400,TimeUnit.MILLISECONDS)
    }
}
