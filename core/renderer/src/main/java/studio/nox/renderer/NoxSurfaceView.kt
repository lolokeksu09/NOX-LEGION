package studio.nox.renderer

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import studio.nox.model.SceneConfig

class NoxSurfaceView(context:Context): SurfaceView(context),SurfaceHolder.Callback {
    private var session: EglSession?=null
    private var config=SceneConfig("FLUX")
    private var fps=60
    private var quality=1
    var onError:(String)->Unit={}
    init { holder.addCallback(this); setZOrderOnTop(false) }
    override fun surfaceCreated(holder:SurfaceHolder) {
        val s=EglSession(context){ message -> post { onError(message) } }
        session=s;s.setConfig(config);s.setRequestedFps(fps);s.setQuality(quality)
        s.start(holder.surface,width,height);s.setVisible(true)
    }
    override fun surfaceChanged(holder:SurfaceHolder,format:Int,width:Int,height:Int) { session?.resize(width,height) }
    override fun surfaceDestroyed(holder:SurfaceHolder) { session?.close();session=null }
    fun configure(c:SceneConfig,frames:Int,quality:Int) { config=c;fps=frames;this.quality=quality;session?.setConfig(c);session?.setRequestedFps(frames);session?.setQuality(quality) }
    fun pauseRender() { session?.setVisible(false) }
    fun resumeRender() { session?.setVisible(true) }
    fun tilt(x:Float,y:Float) { session?.setTilt(x,y) }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE -> session?.setTouch(event.x/width.coerceAtLeast(1),1f-event.y/height.coerceAtLeast(1))
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> session?.setTouch(-1f,-1f)
        }
        return true
    }
}
