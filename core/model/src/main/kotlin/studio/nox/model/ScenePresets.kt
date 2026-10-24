package studio.nox.model

/** Named color compositions for new shader scenes; no automatic DB inserts. */
data class NamedScenePreset(val name: String, val config: SceneConfig)

object ScenePresets {
    private fun variation(id: String, name: String, primary: Int, secondary: Int,
                          seed: Int, glow: Float, density: Float): NamedScenePreset {
        val source=SceneCatalog.default(id)
        return NamedScenePreset(name,source.copy(
            primary=primary,secondary=secondary,seed=seed,glow=glow,density=density))
    }
    private val definitions: Map<String,List<NamedScenePreset>> = mapOf(
        "AURORA" to listOf(
            variation("AURORA","Polar",0x6DEBFF,0x59FFA9,117,0.65f,0.46f),
            variation("AURORA","Emerald",0x36F5B6,0x1476A9,247,0.72f,0.57f),
            variation("AURORA","Violet Mist",0xAE83FF,0x57DBFF,337,0.55f,0.41f)
        ),
        "PULSE" to listOf(
            variation("PULSE","Cyan Beat",0x30D9FF,0x315EFF,413,0.67f,0.45f),
            variation("PULSE","Crimson Core",0xFF648F,0xFF8E63,587,0.73f,0.54f),
            variation("PULSE","Neon Echo",0xA578FF,0x43FFC5,673,0.62f,0.39f)
        ),
        "COMET" to listOf(
            variation("COMET","Deep Space",0x589DFF,0xA6CFFF,757,0.66f,0.40f),
            variation("COMET","White Tail",0xF1F8FF,0x6FDFFF,839,0.54f,0.36f),
            variation("COMET","Blue Drift",0x30D9FF,0x3952E8,997,0.70f,0.49f)
        ),
        "VEIL" to listOf(
            variation("VEIL","Silk Blue",0x69DFFF,0x5D65EA,1113,0.56f,0.58f),
            variation("VEIL","Ether Rose",0xF58BCD,0x7455CE,1229,0.68f,0.47f),
            variation("VEIL","Jade Haze",0x66F7D1,0x177E9F,1307,0.59f,0.64f)
        ),
        "GRID" to listOf(
            variation("GRID","Cyan Matrix",0x29E6F7,0x335FE8,1423,0.65f,0.43f),
            variation("GRID","Neon Sunset",0xFF875E,0xC35CF5,1559,0.72f,0.54f),
            variation("GRID","Arctic Wire",0xD7F8FF,0x5BAAE9,1657,0.49f,0.37f)
        ),
        "SHARDS" to listOf(
            variation("SHARDS","Obsidian",0x7ACAF9,0x4D60E8,1777,0.62f,0.55f),
            variation("SHARDS","Amethyst",0xC39AFF,0x6685FF,1889,0.69f,0.63f),
            variation("SHARDS","Prism",0x66FCE2,0xFF82BD,1993,0.73f,0.51f)
        )
    )
    fun forScene(sceneId: String): List<NamedScenePreset> = definitions[sceneId].orEmpty()
}
