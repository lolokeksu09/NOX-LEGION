package studio.nox.renderer

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.util.Log
import studio.nox.model.SceneConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** OpenGL ES 3 context with a GLES2-compatible shader; works on 3.0 capable phones. */
internal class GlBackend private constructor(
    private val display: EGLDisplay, private val surface: EGLSurface,
    private val context: EGLContext, private var width: Int, private var height: Int
) {
    private val buffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f,-1f, 1f,-1f, -1f,1f, 1f,1f)); position(0)
    }
    private val program: Int
    private val uniforms: Map<String,Int>

    init {
        val vertex = compile(GLES20.GL_VERTEX_SHADER,VERTEX)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER,FRAGMENT)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program,vertex); GLES20.glAttachShader(program,fragment)
        GLES20.glLinkProgram(program)
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,linked,0)
        if (linked[0] == 0) throw IllegalStateException("Shader link: ${GLES20.glGetProgramInfoLog(program)}")
        GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(fragment)
        uniforms = listOf("uTime","uResolution","uMode","uColorA","uColorB","uGlow","uDensity","uMotion","uTouch","uSeed","uQuality")
            .associateWith { GLES20.glGetUniformLocation(program,it) }
    }

    fun resize(w: Int,h: Int) { width=w.coerceAtLeast(1);height=h.coerceAtLeast(1) }
    fun draw(config: SceneConfig, time: Float, tiltX: Float, tiltY: Float, touchX: Float, touchY: Float, quality:Int=1) {
        val c = config.normalized()
        GLES20.glViewport(0,0,width,height)
        GLES20.glClearColor(0f,0f,0f,1f); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uniforms.getValue("uTime"),time*c.speed)
        GLES20.glUniform2f(uniforms.getValue("uResolution"),width.toFloat(),height.toFloat())
        GLES20.glUniform1f(uniforms.getValue("uMode"),when(c.sceneId){"FLUX"->0f;"HALO"->1f;"NEBULA"->2f;"AURORA"->3f;"PULSE"->4f;"COMET"->5f;"VEIL"->6f;"GRID"->7f;"SHARDS"->8f;else->0f})
        GLES20.glUniform3f(uniforms.getValue("uColorA"),((c.primary shr 16) and 255)/255f,((c.primary shr 8) and 255)/255f,(c.primary and 255)/255f)
        GLES20.glUniform3f(uniforms.getValue("uColorB"),((c.secondary shr 16) and 255)/255f,((c.secondary shr 8) and 255)/255f,(c.secondary and 255)/255f)
        GLES20.glUniform1f(uniforms.getValue("uGlow"),c.glow)
        GLES20.glUniform1f(uniforms.getValue("uDensity"),c.density)
        GLES20.glUniform2f(uniforms.getValue("uMotion"),tiltX*c.motion,tiltY*c.motion)
        GLES20.glUniform2f(uniforms.getValue("uTouch"),touchX,touchY)
        GLES20.glUniform1f(uniforms.getValue("uSeed"),c.seed.toFloat())
        GLES20.glUniform1f(uniforms.getValue("uQuality"),quality.coerceIn(0,2).toFloat())
        val pos = GLES20.glGetAttribLocation(program,"aPosition")
        GLES20.glEnableVertexAttribArray(pos)
        buffer.position(0)
        GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,0,buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        GLES20.glDisableVertexAttribArray(pos)
    }
    fun swap() { if(!EGL14.eglSwapBuffers(display,surface)) Log.w("NOX","eglSwapBuffers failed: ${EGL14.eglGetError()}") }
    fun finish() {
        GLES20.glDeleteProgram(program)
        EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display,surface); EGL14.eglDestroyContext(display,context)
        EGL14.eglTerminate(display)
    }
    companion object {
        private const val EGL_OPENGL_ES3_BIT_KHR = 0x0040
        fun create(nativeSurface: Any?, w: Int, h: Int): GlBackend {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "EGL display unavailable" }
            check(EGL14.eglInitialize(display,IntArray(2),0,IntArray(2),0)) { "EGL initialize failed" }
            val attrs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE,EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,
                EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_NONE
            )
            val found = arrayOfNulls<EGLConfig>(1); val count=IntArray(1)
            check(EGL14.eglChooseConfig(display,attrs,0,found,0,1,count,0) && count[0]>0) { "GLES3 config unavailable" }
            val conf= requireNotNull(found[0]); val context=EGL14.eglCreateContext(display,conf,EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL14.EGL_NONE),0)
            check(context != EGL14.EGL_NO_CONTEXT) { "GLES3 context unavailable" }
            val surface=if(nativeSurface!=null) EGL14.eglCreateWindowSurface(display,conf,nativeSurface,intArrayOf(EGL14.EGL_NONE),0)
                else EGL14.eglCreatePbufferSurface(display,conf,intArrayOf(EGL14.EGL_WIDTH,w,EGL14.EGL_HEIGHT,h,EGL14.EGL_NONE),0)
            check(surface != EGL14.EGL_NO_SURFACE) { "EGL surface unavailable: ${EGL14.eglGetError()}" }
            check(EGL14.eglMakeCurrent(display,surface,surface,context)) { "EGL makeCurrent failed" }
            return GlBackend(display,surface,context,w.coerceAtLeast(1),h.coerceAtLeast(1))
        }
        private fun compile(type:Int, source:String):Int {
            val shader=GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader,source);GLES20.glCompileShader(shader)
            val compiled=IntArray(1); GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,compiled,0)
            if(compiled[0]==0) throw IllegalStateException("GLSL: ${GLES20.glGetShaderInfoLog(shader)}")
            return shader
        }
    }
}

private const val VERTEX = """
attribute vec2 aPosition;
varying vec2 vUv;
void main() { vUv = aPosition * 0.5 + 0.5; gl_Position = vec4(aPosition,0.0,1.0); }
"""

/** Three different procedural materials, designed for black AMOLED pixels. */
private const val FRAGMENT = """
precision highp float;
varying vec2 vUv;
uniform float uTime, uMode, uGlow, uDensity, uSeed, uQuality;
uniform vec2 uResolution, uMotion, uTouch;
uniform vec3 uColorA, uColorB;
// Bounded, trig-free lattice hash: avoids huge sin() inputs that can vary on mobile GPUs.
float hash(vec2 p) {
  vec3 q=fract(vec3(p.xyx)*0.1031+fract(uSeed*0.00013));
  q+=dot(q,q.yzx+33.33);
  return fract((q.x+q.y)*q.z);
}
float lightline(float dist, float thickness) { return exp(-abs(dist)*thickness); }
// Smooth bilinear noise prevents visible rectangular gas-cloud cell boundaries.
float smoothNoise(vec2 location) {
  vec2 cell=floor(location);
  vec2 fractional=fract(location);
  vec2 smoothFraction=fractional*fractional*(3.0-2.0*fractional);
  float a=hash(cell);
  float b=hash(cell+vec2(1.0,0.0));
  float c=hash(cell+vec2(0.0,1.0));
  float d=hash(cell+vec2(1.0,1.0));
  return mix(mix(a,b,smoothFraction.x),mix(c,d,smoothFraction.x),smoothFraction.y);
}
float cloudField(vec2 location) {
  return 0.56*smoothNoise(location)+
         0.29*smoothNoise(location*2.09+17.0)+
         0.15*smoothNoise(location*4.11-13.0);
}
void main() {
  vec2 uv = vUv;
  vec2 p = vec2((uv.x-0.5)*uResolution.x/uResolution.y, uv.y-0.5);
  p += uMotion*0.10;
  vec3 sum = vec3(0.0);
  if(uMode < 0.5) {
    // FLUX: staggered luminous ribbons, moving independently across the frame.
    for (int i=0; i<7; ++i) {
      float n=float(i);
      float shift=(n-3.0)*0.068;
      float wave=0.18*sin(p.y*5.8 + n*0.37 + uTime*0.28) + 0.055*sin(p.y*13.0 - uTime*0.41 + n);
      float d = p.x - wave - shift;
      float thin=lightline(d,310.0-90.0*uDensity);
      float halo=lightline(d,31.0)*(0.085 + uGlow*0.17);
      vec3 c=mix(uColorA,uColorB,n/6.0);
      float taper=0.3+0.7*pow(0.5+0.5*sin(p.y*4.2 + uTime*0.25 + n*0.83),2.0);
      sum += c*(thin*0.58+halo)*taper;
    }
  } else if (uMode < 1.5) {
    // HALO: depth-stacked elliptical orbits with bright corners.
    for (int i=0; i<5; ++i) {
      float n=float(i);
      float a=uTime*0.075 + n*1.21;
      mat2 rot=mat2(cos(a),-sin(a),sin(a),cos(a));
      vec2 e=rot*p;
      e.x *= 1.15+0.15*n;
      e.y *= 0.62+0.11*n;
      float radius=0.145+n*0.029;
      float dist=abs(length(e)-radius);
      float sharp=lightline(dist,340.0)*(0.58+0.2*sin(uTime*0.4+n));
      float halo=lightline(dist,33.0)*(0.07+0.13*uGlow);
      sum += mix(uColorB,uColorA,n/5.0)*(sharp+halo)*0.8;
    }
  } else if (uMode < 2.5) {
    // NEBULA: stable star positions, soft glints and independently drifting dust.
    vec2 grid=p*vec2(17.0,17.0);
    vec2 cell=floor(grid);
    for(int x=-1; x<=1; ++x) for(int y=-1; y<=1; ++y) {
      vec2 id=cell+vec2(float(x),float(y));
      vec2 rnd=vec2(hash(id),hash(id+17.1));
      vec2 star=(id+rnd+0.08*sin(uTime*0.13+id.yx))/17.0;
      float d=length((p-star)*vec2(1.0,1.0));
      float density=step(hash(id+37.0),0.25 + uDensity*0.65);
      float twinkle=0.65+0.35*sin(uTime*0.7 + hash(id+5.0)*6.28);
      float beam=exp(-d*520.0)*twinkle;
      float glow=exp(-d*75.0)*(0.04+uGlow*0.12);
      sum+=mix(uColorA,uColorB,hash(id+11.0))*(beam+glow)*density;
    }
    float cloud=0.5+0.5*sin(p.x*12.0 + p.y*7.0 + uTime*0.17)*sin(p.y*10.0-p.x*3.0);
    sum+=uColorB*pow(cloud,5.0)*0.022*uGlow;
  } else if (uMode < 3.5) {
    // AURORA 1.7.1: layered curtains with a quiet, silky inner structure.
    float seedPhase=uSeed*0.0021;
    float vertical=smoothstep(-0.59,-0.32,p.y)*
                   (1.0-smoothstep(0.27,0.54,p.y));
    for(int i=0;i<5;++i) {
      float n=float(i);
      float phase=uTime*(0.16+0.012*n)+n*1.22+seedPhase;
      float ribbon=(n-2.0)*0.115+
          0.114*sin(p.y*3.1+phase)+
          0.027*sin(p.y*9.4-phase*0.68);
      float distanceToRibbon=abs(p.x-ribbon);
      float curtainDepth=0.68+0.082*n;
      float width=0.032+0.027*uDensity+0.003*n;
      float broadCurtain=exp(-distanceToRibbon*distanceToRibbon/(width*width));
      float luminousCore=exp(-distanceToRibbon*(55.0+31.0*uDensity));
      float innerFilament=exp(-abs(p.x-ribbon-
          0.008*sin(p.y*11.8+phase*0.67))*252.0);
      float folding=0.48+0.52*pow(0.5+0.5*sin(p.y*6.2-phase*0.48),2.0);
      float hue=mix(n/4.0,0.5+0.32*sin(p.y*1.4+phase*0.19),0.34);
      vec3 spectral=mix(uColorA,uColorB,clamp(hue,0.0,1.0));
      float energy=(broadCurtain*(0.019+0.061*uGlow)+
          luminousCore*(0.033+0.092*uGlow)+
          innerFilament*(0.039+0.068*uGlow));
      sum+=spectral*energy*folding*curtainDepth*vertical;
    }
  } else if (uMode < 4.5) {
    // PULSE 1.7.1: traveling waves fade into and out of deep black.
    vec2 focus=vec2(0.0,0.015);
    if(uTouch.x>=0.0) {
      vec2 touchFocus=vec2((uTouch.x-0.5)*uResolution.x/uResolution.y,
                           uTouch.y-0.5);
      focus=mix(focus,touchFocus,0.78);
    }
    float radial=length((p-focus)*vec2(1.06,1.0));
    for(int i=0;i<6;++i) {
      float n=float(i);
      float life=fract(uTime*0.25+n/6.0+uSeed*0.00017);
      float radius=0.014+life*(0.30+0.14*uDensity);
      float delta=abs(radial-radius);
      float cycleFade=smoothstep(0.015,0.16,life)*
                      (1.0-smoothstep(0.78,1.0,life));
      float core=exp(-delta*(244.0-69.0*uDensity));
      float halo=exp(-delta*(28.0+9.0*uDensity));
      float glowStrength=0.88+0.12*sin(uTime*0.24+n*0.7);
      vec3 spectrum=mix(uColorA,uColorB,n/5.0);
      sum+=spectrum*(core*(0.22+0.32*uGlow)+
                     halo*(0.015+0.062*uGlow))*cycleFade*glowStrength;
    }
    sum+=mix(uColorA,uColorB,0.32)*exp(-radial*45.0)*0.018*uGlow;
  } else if (uMode < 5.5) {
    // COMET 1.7.1: depth-layered trails with independent drift.
    for(int i=0;i<7;++i) {
      float n=float(i);
      float randA=hash(vec2(n+2.0,18.0));
      float randB=hash(vec2(5.0,n+7.0));
      float depth=0.65+0.55*randB;
      vec2 direction=normalize(vec2(0.77+0.20*randA,
                                    -0.64+0.19*randB));
      vec2 perp=vec2(-direction.y,direction.x);
      float life=fract(randA+uTime*(0.059+0.020*randB));
      float travel=(life*2.0-1.0)*1.42;
      float sideways=(randB-0.5)*(0.85+0.18*uDensity);
      vec2 head=direction*travel+perp*sideways;
      vec2 delta=p-head;
      float along=dot(delta,direction);
      float perpendicularDistance=abs(dot(delta,perp));
      float width=(0.0014+0.0026*uDensity)*depth;
      float streak=exp(-perpendicularDistance*perpendicularDistance/
                       (width*width));
      float tailSpan=0.070+0.16*uDensity+0.035*randA;
      float tail=exp(min(0.0,along)/tailSpan);
      float behind=1.0-smoothstep(-0.005,0.009,along);
      float trailFade=smoothstep(0.025,0.13,life)*
                       (1.0-smoothstep(0.87,0.995,life));
      float visibilityMask=step(hash(vec2(n+31.0,53.0)),0.37+0.63*uDensity);
      float distanceToHead=length(delta);
      float headGlow=exp(-distanceToHead*(160.0-24.0*randB));
      float softHalo=exp(-distanceToHead*35.0);
      vec3 spectrum=mix(uColorA,uColorB,randB);
      sum+=spectrum*(streak*behind*tail*(0.13+0.28*uGlow)+
          headGlow*(0.41+0.37*uGlow)+
          softHalo*(0.010+0.024*uGlow))*depth*trailFade*visibilityMask;
    }
  } else if (uMode < 6.5) {
    // VEIL 1.8: slow, layered luminous mist with an absolute black field.
    vec2 warped=p+vec2(0.022*sin(p.y*7.1+uTime*0.19),
                       0.019*cos(p.x*6.2-uTime*0.12));
    for(int i=0;i<5;++i) {
      float n=float(i);
      float phase=uSeed*0.0017+n*1.41+uTime*(0.085+0.013*n);
      float bend=0.055*sin(warped.x*7.0+phase)+
                 0.030*cos(warped.x*13.0-phase*0.74);
      float center=(n-2.0)*0.082+bend;
      float width=0.040+0.032*uDensity+0.009*n;
      float dy=(warped.y-center)/width;
      float silky=exp(-dy*dy*1.75);
      float filament=exp(-abs(dy+0.20*sin(warped.x*17.0+phase))*7.0);
      float horizontal=pow(0.5+0.5*sin(warped.x*3.4-phase*0.36),2.0);
      float shape=smoothstep(-0.37,-0.07,warped.x)*
                  (1.0-smoothstep(0.09,0.44,warped.x));
      vec3 color=mix(uColorA,uColorB,clamp(0.20*n+0.11*horizontal,0.0,1.0));
      sum+=color*(silky*(0.030+0.084*uGlow)+
           filament*silky*(0.012+0.035*uGlow))*
           (0.45+0.55*horizontal)*shape;
    }
  } else if (uMode < 7.5) {
    // GRID 1.8: bounded, receding luminous perspective lattice.
    float ground=max(0.0,0.13-p.y);
    float horizonFade=smoothstep(0.018,0.17,ground);
    float distanceScale=1.0/(0.16+ground*2.6);
    float shift=uTime*0.018+uSeed*0.00017;
    float axisX=(p.x+0.045*sin(p.y*2.2+uTime*0.12))*distanceScale*3.0;
    float axisZ=distanceScale*1.55+shift;
    float cellX=abs(fract(axisX+0.5)-0.5);
    float cellZ=abs(fract(axisZ+0.5)-0.5);
    float linesX=exp(-cellX*(95.0-30.0*uDensity));
    float linesZ=exp(-cellZ*(98.0-28.0*uDensity));
    float lattice=max(linesX,linesZ);
    float depthFade=exp(-ground*0.72)*horizonFade;
    float horizonGlow=exp(-abs(ground-0.10)*35.0)*horizonFade;
    float brightness=(0.13+0.37*uGlow)*depthFade;
    sum+=mix(uColorA,uColorB,0.26+0.33*ground)*lattice*brightness;
    sum+=uColorB*horizonGlow*(0.008+0.028*uGlow);
  } else {
    // SHARDS 1.8: rotating faceted crystals with luminous edges and deep shadows.
    float aspect=uResolution.x/uResolution.y;
    for(int i=0;i<7;++i) {
      float n=float(i);
      float randA=hash(vec2(n+7.0,19.0));
      float randB=hash(vec2(29.0,n+13.0));
      float sizeFactor=0.65+0.50*randA;
      vec2 center=vec2((randA-0.5)*aspect*0.80,
                       (randB-0.5)*0.82);
      center+=vec2(0.013*sin(uTime*0.24+n),0.020*cos(uTime*0.18+n));
      float rotation=uTime*(0.058+0.034*randA)+n*1.71;
      mat2 turn=mat2(cos(rotation),-sin(rotation),
                     sin(rotation),cos(rotation));
      vec2 local=turn*(p-center);
      float radius=(0.046+0.049*uDensity)*sizeFactor;
      float diamond=abs(local.x)*1.12+abs(local.y)*0.91;
      float rim=exp(-abs(diamond-radius)*156.0);
      float interior=1.0-smoothstep(radius*0.16,radius*0.90,diamond);
      float facet=0.46+0.54*smoothstep(-radius*0.6,radius*0.5,local.x-local.y);
      float seam=exp(-abs(local.x+local.y*0.5)*115.0)*interior;
      float visibilityMask=step(hash(vec2(n+5.0,37.0)),0.33+0.67*uDensity);
      vec3 crystal=mix(uColorA,uColorB,randB);
      sum+=crystal*visibilityMask*(rim*(0.19+0.44*uGlow)+
           interior*facet*(0.009+0.032*uGlow)+
           seam*(0.013+0.031*uGlow));
    }
  }
  if(uTouch.x>=0.0) {
    vec2 tp=vec2((uTouch.x-0.5)*uResolution.x/uResolution.y,uTouch.y-0.5);
    float d=length(p-tp);
    sum += uColorA*exp(-d*25.0)*0.32;
  }
  // OVERDRIVE: rich localized accents, never a flat screen-wide gray lift.
  float qualityMix=clamp(uQuality*0.58,0.0,1.16);
  vec3 premiumDetail=vec3(0.0);
  if(uQuality>0.5) {
    if(uMode<0.5) {
      // OVERDRIVE FLUX: large draped ribbons, luminous cores and subtle tracer sparks.
      for(int i=0;i<5;++i) {
        float n=float(i);
        float w=0.15*sin(p.y*4.2+n*0.91+uTime*0.19)+0.042*sin(p.y*11.8-uTime*0.26+n);
        float d=abs(p.x-w-(n-2.0)*0.112);
        float broad=exp(-d*d/(0.012+0.018*uDensity));
        float filament=exp(-d*(86.0-19.0*uDensity));
        float shimmer=0.7+0.3*sin(p.y*10.0+n+uTime*0.23);
        premiumDetail+=mix(uColorA,uColorB,n/4.0)*
          (broad*(0.044+uGlow*0.085)+filament*(0.07+uGlow*0.09))*shimmer;
      }
    } else if (uMode<1.5) {
      // OVERDRIVE HALO: hero nucleus, stacked corona and orbit highlights.
      float r=length(p*vec2(1.05,0.82));
      float core=exp(-r*28.0);
      float corona=exp(-abs(r-0.23)*26.0)*(0.6+0.4*sin(atan(p.y,p.x)*4.0+uTime*0.1));
      float outer=exp(-abs(r-0.36)*41.0)*(0.56+0.44*cos(atan(p.y,p.x)*6.0-uTime*0.1));
      premiumDetail+=uColorA*core*(0.08+uGlow*0.17)+
         mix(uColorA,uColorB,0.55)*corona*(0.06+uGlow*0.16)+
         uColorB*outer*(0.028+uGlow*0.055);
    } else if (uMode<2.5) {
      // OVERDRIVE NEBULA: multi-depth gas clouds without cell discontinuities.
      vec2 nebula=p-vec2(0.038*sin(uTime*0.08),-0.045);
      nebula.x+=0.06*sin(nebula.y*7.0+uTime*0.12);
      float galaxy=exp(-dot(nebula/vec2(0.31,0.19),nebula/vec2(0.31,0.19))*1.45);
      float ridges=pow(0.5+0.5*sin(nebula.x*15.0+nebula.y*7.0+uTime*0.16)*
                     cos(nebula.y*13.0-nebula.x*5.0),2.2);
      float veil=exp(-dot((nebula-vec2(-0.09,0.08))/vec2(0.23,0.31),
                          (nebula-vec2(-0.09,0.08))/vec2(0.23,0.31))*2.0);
      premiumDetail+=uColorB*galaxy*ridges*(0.16+0.26*uGlow)+
                    uColorA*veil*(0.038+0.08*uGlow);
    } else if (uMode<3.5) {
      // OVERDRIVE AURORA: wide cinematic light curtains and fine plasma silk.
      for(int i=0;i<4;++i) {
        float n=float(i);
        float center=(n-1.5)*0.16+0.10*sin(p.y*4.0+n+uTime*0.12);
        float dx=abs(p.x-center);
        float wide=exp(-dx*dx/(0.008+uDensity*0.012));
        float veins=exp(-dx*(130.0+40.0*uDensity));
        float fade=smoothstep(-0.52,-0.22,p.y)*(1.0-smoothstep(0.23,0.55,p.y));
        premiumDetail+=mix(uColorA,uColorB,n/3.0)*
            (wide*0.13+veins*0.14)*fade*(0.65+0.35*sin(p.y*5.0+n+uTime*0.08));
      }
    } else if (uMode<4.5) {
      // OVERDRIVE PULSE: glowing energy nucleus and secondary shockwaves.
      vec2 focus=vec2(0.0,0.015);
      if(uTouch.x>=0.0) focus=mix(focus,vec2((uTouch.x-0.5)*uResolution.x/uResolution.y,uTouch.y-0.5),0.78);
      float radius=length(p-focus);
      float nucleus=exp(-radius*33.0);
      float shock=exp(-abs(radius-(0.16+0.03*sin(uTime*0.17)))*38.0);
      float ripple=exp(-abs(radius-0.30)*58.0);
      premiumDetail+=uColorA*nucleus*(0.17+0.29*uGlow)+
          mix(uColorA,uColorB,0.53)*shock*(0.045+0.10*uGlow)+
          uColorB*ripple*(0.03+0.09*uGlow);
    } else if (uMode<5.5) {
      // OVERDRIVE COMET: hero drifting streaks with brighter inner trails.
      vec2 origin=vec2(0.12*sin(uTime*0.14),-0.08+0.10*cos(uTime*0.11));
      vec2 difference=p-origin;
      vec2 dir=normalize(vec2(0.82,-0.57));
      float longitudinal=dot(difference,dir);
      float transverse=abs(dot(difference,vec2(-dir.y,dir.x)));
      float trail=exp(-transverse*58.0)*(1.0-smoothstep(-0.002,0.013,longitudinal))*
            exp(min(0.0,longitudinal)*5.0);
      float focal=exp(-length(difference)*25.0);
      premiumDetail+=mix(uColorA,uColorB,0.22)*(trail*(0.16+0.20*uGlow)+focal*(0.07+0.13*uGlow));
    } else if (uMode<6.5) {
      // OVERDRIVE VEIL: enveloping luminous fog strata with fine glowing veins.
      for(int i=0;i<4;++i) {
        float n=float(i);
        float center=(n-1.5)*0.12+0.035*sin(p.x*10.0+n+uTime*0.1);
        float distanceToLayer=abs(p.y-center);
        float fog=exp(-distanceToLayer*distanceToLayer/(0.007+0.010*uDensity));
        float vein=exp(-distanceToLayer*48.0);
        float taper=exp(-p.x*p.x*5.0);
        premiumDetail+=mix(uColorA,uColorB,n/3.0)*
           (fog*(0.083+uGlow*0.09)+vein*0.026)*taper;
      }
    } else if (uMode<7.5) {
      // OVERDRIVE GRID: electric horizon and light nodes at line intersections.
      float ground=max(0.0,0.13-p.y);
      float scale=1.0/(0.16+ground*2.6);
      vec2 lattice=vec2((p.x+0.045*sin(p.y*2.2+uTime*0.12))*scale*3.0,
           scale*1.55+uTime*0.018+uSeed*0.00017);
      vec2 edge=abs(fract(lattice+0.5)-0.5);
      float intersections=exp(-(edge.x+edge.y)*45.0);
      float perspective=smoothstep(0.018,0.17,ground);
      float lightHorizon=exp(-abs(p.y-0.115)*37.0);
      premiumDetail+=uColorA*intersections*perspective*(0.11+uGlow*0.20)+
        uColorB*lightHorizon*(0.025+uGlow*0.08);
    } else {
      // OVERDRIVE SHARDS: large centerpiece facet with refractive edges.
      vec2 shift=vec2(0.08*sin(uTime*0.075),0.05*cos(uTime*0.06));
      vec2 crystal=p-shift;
      float angle=uTime*0.06;
      mat2 rot=mat2(cos(angle),-sin(angle),sin(angle),cos(angle));
      crystal=rot*crystal;
      float diamond=abs(crystal.x)*0.92+abs(crystal.y)*1.11;
      float border=exp(-abs(diamond-0.235)*87.0);
      float inner=1.0-smoothstep(0.08,0.22,diamond);
      float facet=0.55+0.45*smoothstep(-0.21,0.21,crystal.x-crystal.y);
      float refraction=exp(-abs(crystal.x+crystal.y*0.6)*40.0)*inner;
      premiumDetail+=mix(uColorA,uColorB,facet)*
           (border*(0.10+0.23*uGlow)+inner*facet*0.055+refraction*0.082);
    }
    // CINEMATIC: foreground forms, middle glow and background structures.
    // Every scene receives a distinct composition rather than a brightness boost.
    vec3 cinematicDetail=vec3(0.0);
    vec3 shapedVolume=vec3(0.0);
    if(uMode<0.5) {
      // CINEMATIC FLUX: sculpted flowing wings with bright nested filaments.
      for(int i=0;i<4;++i) {
        float n=float(i);
        float ribbon=(n-1.5)*0.145+
            0.125*sin(p.y*3.9+n*1.08+uTime*0.16)+
            0.035*sin(p.y*11.0+n*0.9-uTime*0.26);
        float d=abs(p.x-ribbon);
        float deep=exp(-d*d/(0.0048+0.009*uDensity));
        float bright=exp(-d*(78.0+22.0*uDensity));
        float knot=0.70+0.30*pow(0.5+0.5*sin(p.y*14.0+n*2.4-uTime*0.29),3.0);
        cinematicDetail+=mix(uColorA,uColorB,n/3.0)*
            (deep*(0.09+0.12*uGlow)+bright*(0.05+0.11*uGlow))*knot;
      }
      // VOLUMETRIC FLUX: broad energy sheets with an asymmetric specular rim.
      float flowingAxis=p.x-0.16*sin(p.y*3.6+uTime*0.14);
      float flowingSheet=exp(-flowingAxis*flowingAxis/0.055);
      float bevelLight=exp(-abs(flowingAxis-0.14)*24.0);
      float angledLight=0.30+0.70*smoothstep(-0.21,0.16,flowingAxis);
      float heightMask=1.0-smoothstep(0.42,0.57,abs(p.y));
      shapedVolume+=mix(uColorA,uColorB,0.38)*
          (flowingSheet*angledLight*(0.06+0.10*uGlow)+bevelLight*0.05)*heightMask;
    } else if(uMode<1.5) {
      // CINEMATIC HALO: multiple giant elliptical arcs and orbit beacons.
      vec2 ellipsoid=p*vec2(1.05,0.72);
      float r=length(ellipsoid);
      float theta=atan(ellipsoid.y,ellipsoid.x);
      float inner=exp(-abs(r-0.15)*28.0);
      float outer=exp(-abs(r-0.34)*23.0);
      float arcs=0.52+0.48*cos(theta*5.0+uTime*0.12);
      vec2 beacon=vec2(0.31*cos(uTime*0.12),0.43*sin(uTime*0.12));
      float orbiter=exp(-length(p-beacon)*41.0);
      cinematicDetail+=uColorA*inner*(0.055+0.13*uGlow)+
          uColorB*outer*arcs*(0.085+0.14*uGlow)+
          mix(uColorA,uColorB,0.42)*orbiter*(0.09+0.22*uGlow);
      // VOLUMETRIC HALO: translucent toroidal wall, brighter on the near face.
      float ringCrossSection=r-0.275;
      float toroidWall=exp(-ringCrossSection*ringCrossSection/0.014);
      float nearFace=0.32+0.68*smoothstep(-0.23,0.22,ellipsoid.y);
      float innerRim=exp(-abs(r-0.18)*27.0);
      shapedVolume+=mix(uColorA,uColorB,0.57)*
          (toroidWall*nearFace*(0.07+0.10*uGlow)+innerRim*0.035);
    } else if(uMode<2.5) {
      // CINEMATIC NEBULA: continuous three-octave gas clouds and stellar ridges.
      vec2 drift=p+vec2(uTime*0.012,-uTime*0.007);
      vec2 gas=vec2(drift.x*5.9+1.2*sin(drift.y*6.0),
                     drift.y*4.7+0.5*sin(drift.x*7.1));
      float turbulence=cloudField(gas+uSeed*0.00013);
      vec2 centre=vec2(0.015,-0.015);
      float body=exp(-dot((p-centre)/vec2(0.31,0.36),
                          (p-centre)/vec2(0.31,0.36))*1.3);
      float ridge=smoothstep(0.33,0.72,turbulence);
      float shell=exp(-dot((p-vec2(-0.13,0.17))/vec2(0.19,0.24),
                          (p-vec2(-0.13,0.17))/vec2(0.19,0.24))*1.8);
      float brightGas=body*ridge;
      cinematicDetail+=uColorA*brightGas*(0.18+0.34*uGlow)+
          uColorB*shell*smoothstep(0.24,0.66,turbulence)*(0.11+0.24*uGlow);
      // VOLUMETRIC NEBULA: cloud body with directional light and denser knots.
      float nebulaRelief=smoothstep(0.39,0.70,turbulence);
      float nebulaFacing=0.36+0.64*smoothstep(-0.31,0.28,p.x*0.65-p.y*0.55);
      float cloudMass=body*nebulaRelief*nebulaFacing;
      float backScatter=shell*smoothstep(0.47,0.71,turbulence);
      shapedVolume+=uColorA*cloudMass*(0.085+0.16*uGlow)+
          uColorB*backScatter*(0.045+0.09*uGlow);
    } else if(uMode<3.5) {
      // CINEMATIC AURORA: taller folding curtains with luminous vein structure.
      float vertical=smoothstep(-0.52,-0.38,p.y)*
          (1.0-smoothstep(0.39,0.54,p.y));
      for(int i=0;i<4;++i) {
        float n=float(i);
        float curtain=(n-1.5)*0.135+
            0.077*sin(p.y*4.5+uTime*0.17+n*1.7);
        float dd=abs(p.x-curtain);
        float soft=exp(-dd*dd/(0.008+0.007*uDensity));
        float detail=exp(-abs(p.x-curtain-0.017*sin(p.y*14.0+n))*107.0);
        float folds=0.58+0.42*pow(0.5+0.5*sin(p.y*9.0-uTime*0.11+n),2.0);
        cinematicDetail+=mix(uColorA,uColorB,n/3.0)*
            (soft*(0.09+0.15*uGlow)+detail*(0.035+0.08*uGlow))*folds*vertical;
      }
      // VOLUMETRIC AURORA: directional inner fold lighting on the tall curtain.
      float curtainAxis=p.x-0.18*sin(p.y*3.25+uTime*0.12);
      float curtainBody=exp(-curtainAxis*curtainAxis/0.041)*vertical;
      float curtainFacing=0.39+0.61*smoothstep(-0.16,0.16,curtainAxis);
      float foldedRim=exp(-abs(curtainAxis+0.095)*17.0)*vertical;
      shapedVolume+=mix(uColorA,uColorB,0.31)*
          (curtainBody*curtainFacing*(0.055+0.13*uGlow)+foldedRim*0.045);
    } else if(uMode<4.5) {
      // CINEMATIC PULSE: an energy reactor, nested rings and moving plasma arcs.
      vec2 target=vec2(0.0,0.015);
      if(uTouch.x>=0.0) target=mix(target,
          vec2((uTouch.x-0.5)*uResolution.x/uResolution.y,uTouch.y-0.5),0.78);
      vec2 delta=p-target;
      float r=length(delta);
      float disc=exp(-r*r*104.0);
      float ring=exp(-abs(r-0.22)*31.0);
      float wave=exp(-abs(r-(0.34+0.025*sin(uTime*0.19)))*37.0);
      float angular=0.57+0.43*cos(atan(delta.y,delta.x)*8.0-uTime*0.15);
      cinematicDetail+=uColorA*disc*(0.09+0.22*uGlow)+
          mix(uColorA,uColorB,0.42)*ring*angular*(0.07+0.16*uGlow)+
          uColorB*wave*(0.028+0.095*uGlow);
      // VOLUMETRIC PULSE: sphere-like core and light-scattered outer shell.
      float reactorBody=exp(-r*r*24.0);
      float reactorShell=r-0.185;
      float shellLight=exp(-reactorShell*reactorShell/0.007);
      float reactorFacing=0.30+0.70*smoothstep(-0.22,0.24,delta.y+delta.x*0.45);
      shapedVolume+=uColorA*reactorBody*reactorFacing*(0.07+0.14*uGlow)+
          mix(uColorA,uColorB,0.55)*shellLight*(0.045+0.08*uGlow);
    } else if(uMode<5.5) {
      // CINEMATIC COMET: moving secondary streaks behind the hero comet.
      for(int i=0;i<3;++i) {
        float n=float(i);
        vec2 direction=normalize(vec2(0.76+0.08*n,-0.65+0.06*n));
        vec2 perpendicular=vec2(-direction.y,direction.x);
        vec2 head=vec2(0.15*sin(uTime*(0.10+n*0.03)+n*2.0),
                       (n-1.0)*0.17+0.08*cos(uTime*0.14+n));
        vec2 delta=p-head;
        float longitudinal=dot(delta,direction);
        float sideways=abs(dot(delta,perpendicular));
        float trail=exp(-sideways*(42.0+11.0*n))*
            (1.0-smoothstep(-0.008,0.006,longitudinal))*
            exp(min(0.0,longitudinal)*(4.0+2.0*n));
        float headLight=exp(-length(delta)*(30.0+6.0*n));
        cinematicDetail+=mix(uColorA,uColorB,n/2.0)*
            (trail*(0.07+0.13*uGlow)+headLight*(0.035+0.09*uGlow));
      }
      // VOLUMETRIC COMET: dense tapered plasma wake behind the hero streak.
      vec2 plasmaAxis=normalize(vec2(0.78,-0.62));
      vec2 plasmaOrigin=vec2(0.10*sin(uTime*0.1),-0.12+0.07*cos(uTime*0.13));
      vec2 plasmaDelta=p-plasmaOrigin;
      float plasmaLong=dot(plasmaDelta,plasmaAxis);
      float plasmaSide=dot(plasmaDelta,vec2(-plasmaAxis.y,plasmaAxis.x));
      float plasmaVolume=exp(-plasmaSide*plasmaSide/0.042)*
          (1.0-smoothstep(0.01,0.17,plasmaLong))*exp(min(0.0,plasmaLong)*1.7);
      float plasmaFacing=0.34+0.66*smoothstep(-0.15,0.14,plasmaSide);
      shapedVolume+=mix(uColorA,uColorB,0.40)*plasmaVolume*plasmaFacing*
          (0.08+0.13*uGlow);
    } else if(uMode<6.5) {
      // CINEMATIC VEIL: smooth volumetric silk smoke, glowing veins and folds.
      vec2 drift=vec2(p.x*5.0+uTime*0.045,p.y*4.8-uTime*0.018);
      float texture=cloudField(drift+vec2(3.3,-4.8)+uSeed*0.00012);
      float flow=0.06*sin(p.x*9.0+uTime*0.13);
      float centre=exp(-pow((p.y-flow)/0.35,2.0));
      float feathery=smoothstep(0.29,0.72,texture);
      float filaments=exp(-abs(p.y-flow-0.12*sin(p.x*8.1+uTime*0.09))*16.0);
      cinematicDetail+=mix(uColorA,uColorB,clamp(0.45+0.38*p.y,0.0,1.0))*
          centre*(feathery*(0.16+0.26*uGlow)+
                  filaments*(0.035+0.095*uGlow));
      // VOLUMETRIC VEIL: translucent fold density and directional silk sheen.
      float veilDensity=centre*smoothstep(0.34,0.73,texture);
      float veilFacing=0.42+0.58*(0.5+0.5*sin(p.x*5.0+p.y*3.0+uTime*0.035));
      float foldRidge=p.y-flow-0.13*sin(p.x*4.2);
      float veilSheen=exp(-foldRidge*foldRidge/0.017)*centre;
      shapedVolume+=mix(uColorA,uColorB,0.48)*
          (veilDensity*veilFacing*(0.09+0.17*uGlow)+veilSheen*0.06);
    } else if(uMode<7.5) {
      // CINEMATIC GRID: illuminated skyline columns over a glowing horizon.
      float sky=smoothstep(0.12,0.27,p.y)*
          (1.0-smoothstep(0.45,0.60,p.y));
      float column=exp(-abs(fract((p.x+0.31)*17.0)-0.5)*28.0);
      float verticalGlare=exp(-abs(p.x-0.11*sin(uTime*0.12))*18.0);
      float beam=exp(-abs(p.y-0.135)*26.0);
      cinematicDetail+=uColorA*sky*column*(0.06+0.14*uGlow)+
          uColorB*verticalGlare*sky*(0.025+0.08*uGlow)+
          mix(uColorA,uColorB,0.65)*beam*(0.05+0.11*uGlow);
      // VOLUMETRIC GRID: visible bodies and recessed faces of light pillars.
      float columnBody=exp(-abs(fract((p.x+0.31)*17.0)-0.5)*11.0)*sky;
      float pillarFacing=0.39+0.61*smoothstep(-0.4,0.4,sin(p.x*53.4));
      float distantFloor=exp(-abs(p.y-0.12)*14.0)*smoothstep(0.15,0.48,abs(p.x));
      shapedVolume+=uColorA*columnBody*pillarFacing*(0.045+0.095*uGlow)+
          uColorB*distantFloor*(0.025+0.065*uGlow);
    } else {
      // CINEMATIC SHARDS: several floating large facet silhouettes and highlights.
      for(int i=0;i<4;++i) {
        float n=float(i);
        vec2 centre=vec2((n-1.5)*0.13+0.025*sin(uTime*0.11+n),
                         0.20*cos(n*1.8+uTime*0.08));
        vec2 displaced=p-centre;
        float angle=0.13*n+uTime*(0.055+0.014*n);
        mat2 rotation=mat2(cos(angle),-sin(angle),sin(angle),cos(angle));
        vec2 facet=rotation*displaced;
        float distance=abs(facet.x)*0.93+abs(facet.y)*1.12;
        float radius=0.11+0.027*mod(n,2.0);
        float edge=exp(-abs(distance-radius)*56.0);
        float fill=1.0-smoothstep(radius*0.16,radius*0.85,distance);
        float bevel=exp(-abs(facet.x-facet.y*0.35)*49.0)*fill;
        cinematicDetail+=mix(uColorA,uColorB,n/3.0)*
            (edge*(0.07+0.16*uGlow)+fill*0.034+bevel*0.065);
      }
      // VOLUMETRIC SHARDS: a broad faceted crystal with angled plane highlights.
      vec2 prism=p-vec2(0.01,0.03);
      float crystalDistance=abs(prism.x)*0.8+abs(prism.y)*1.06;
      float crystalBody=1.0-smoothstep(0.09,0.36,crystalDistance);
      float crystalFacing=0.30+0.70*smoothstep(-0.23,0.20,prism.x-prism.y*0.45);
      float crystalRim=exp(-abs(crystalDistance-0.31)*31.0);
      shapedVolume+=mix(uColorA,uColorB,0.50)*
          (crystalBody*crystalFacing*(0.06+0.12*uGlow)+crystalRim*0.035);
    }
    // Hide almost-black fill and retain deep AMOLED negative space.
    float shapedEnergy=max(shapedVolume.r,max(shapedVolume.g,shapedVolume.b));
    shapedVolume*=smoothstep(0.006,0.024,shapedEnergy);
    sum+=shapedVolume*qualityMix;
    sum+=cinematicDetail*qualityMix;
    if(uQuality>1.5) {
      // Ultra: pinprick highlights live only inside already-illuminated shapes.
      vec2 microUV=p*vec2(72.0,88.0);
      vec2 microCell=floor(microUV);
      vec2 microPoint=fract(microUV)-vec2(hash(microCell+7.1),
                                          hash(microCell+23.4));
      float microSpark=exp(-dot(microPoint,microPoint)*83.0)*
          step(0.958,hash(microCell+54.0));
      float activeLight=smoothstep(0.032,0.19,max(sum.r,max(sum.g,sum.b)));
      sum+=mix(uColorA,uColorB,0.48)*microSpark*activeLight*
          (0.052+0.094*uGlow);
    }
    // Preserve rich local contrast without washing black pixels to charcoal.
    sum+=premiumDetail*qualityMix;
    sum*=1.0+qualityMix*0.22;
    // VIVID 1.9.3: compressed local spectral exposure, never a full-screen tint.
    // More readable neon midtones, softer bright peaks, stronger saturated cores.
    // The mask leaves low-energy haze and exact AMOLED black untouched.
    float luminousSignal=max(sum.r,max(sum.g,sum.b));
    float luminousMask=smoothstep(0.022,0.11,luminousSignal);
    vec3 spectralGrade=(sum*2.6)/(vec3(1.0)+sum*1.8);
    float spectralLuma=dot(spectralGrade,vec3(0.2126,0.7152,0.0722));
    spectralGrade=max(vec3(0.0),mix(vec3(spectralLuma),spectralGrade,1.23));
    sum=mix(sum,spectralGrade,luminousMask);
  }
  // Preserve the deep-black backdrop instead of adding synthetic gray noise.
  gl_FragColor=vec4(min(sum,vec3(1.0)),1.0);
}
"""
