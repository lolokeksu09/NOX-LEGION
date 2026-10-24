package studio.nox.model

/** Colors are 24-bit RRGGBB, without alpha. All numbers are persisted with their scene. */
data class SceneConfig(
    val sceneId: String,
    val primary: Int = 0x30D9FF,
    val secondary: Int = 0x5C57FF,
    val speed: Float = 0.42f,
    val glow: Float = 0.68f,
    val density: Float = 0.52f,
    val motion: Float = 0.20f,
    val seed: Int = 137
) {
    fun normalized(): SceneConfig = copy(
        sceneId = if (sceneId in SceneCatalog.ids) sceneId else "FLUX",
        primary = primary and 0xFFFFFF,
        secondary = secondary and 0xFFFFFF,
        speed = speed.coerceFinite(0.42f),
        glow = glow.coerceFinite(0.68f),
        density = density.coerceFinite(0.52f),
        motion = motion.coerceFinite(0.20f)
    )
}

private fun Float.coerceFinite(fallback: Float): Float = if (isFinite()) coerceIn(0f, 1f) else fallback

object SceneCatalog {
    val ids = SceneRegistry.entries.map { it.id }
    fun default(id: String): SceneConfig = when (id) {
        "HALO" -> SceneConfig("HALO", 0x45C6FF, 0x9962FF, speed=0.32f, glow=0.78f, density=0.45f)
        "NEBULA" -> SceneConfig("NEBULA", 0x64DAFF, 0x3256EF, speed=0.20f, glow=0.50f, density=0.72f)
        "AURORA" -> SceneConfig("AURORA", 0x6DEBFF, 0x59FFA9, speed=0.18f, glow=0.63f, density=0.44f, motion=0.16f)
        "PULSE" -> SceneConfig("PULSE", 0x30D9FF, 0x315EFF, speed=0.30f, glow=0.66f, density=0.39f, motion=0.15f)
        "COMET" -> SceneConfig("COMET", 0x589DFF, 0xA6CFFF, speed=0.27f, glow=0.60f, density=0.40f, motion=0.12f)
        "VEIL" -> SceneConfig("VEIL",0x69DFFF,0x5D65EA,speed=0.15f,glow=0.56f,density=0.58f,motion=0.16f)
        "GRID" -> SceneConfig("GRID",0x29E6F7,0x335FE8,speed=0.25f,glow=0.65f,density=0.43f,motion=0.20f)
        "SHARDS" -> SceneConfig("SHARDS",0x7ACAF9,0x4D60E8,speed=0.17f,glow=0.62f,density=0.55f,motion=0.18f)
        else -> SceneConfig("FLUX")
    }
    fun validName(value: String): String? = value.trim().takeIf { it.isNotBlank() && it.length <= 48 }
    fun fromHex(value: String): Int? {
        val clean = value.trim().removePrefix("#")
        if (!Regex("[0-9a-fA-F]{6}").matches(clean)) return null
        return clean.toIntOrNull(16)
    }
}

object FrameBudget {
    /** Thermal constants: 2 moderate, 3 severe, 4 critical. */
    fun allowed(requested: Int, powerSave: Boolean, thermalStatus: Int): Int {
        val base = when (requested) { 30, 60, 120 -> requested; else -> 60 }
        return when {
            thermalStatus >= 3 -> minOf(base, 15)
            powerSave || thermalStatus >= 2 -> minOf(base, 30)
            else -> base
        }
    }
}
