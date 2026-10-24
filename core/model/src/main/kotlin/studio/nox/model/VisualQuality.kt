package studio.nox.model

/** Visual complexity tiers; independent of persisted SceneConfig / preset exchange. */
object VisualQuality {
    const val BALANCED=0
    const val RICH=1
    const val ULTRA=2
    fun normalized(value:Int):Int=if(value in BALANCED..ULTRA)value else RICH
    /** Lower effects complexity when Android reports thermal/power pressure. */
    fun effective(requested:Int,powerSave:Boolean,thermalStatus:Int):Int =
        if(powerSave || thermalStatus>=2) BALANCED else normalized(requested)
}
