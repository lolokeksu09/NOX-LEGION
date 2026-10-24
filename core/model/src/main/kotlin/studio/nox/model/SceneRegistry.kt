package studio.nox.model

/** Immutable scene metadata. Add new modes here and update the shared GPU renderer. */
enum class SceneCategory { SIGNATURE, AMBIENT, TECH, COSMIC }

data class SceneDefinition(val id: String, val category: SceneCategory, val isNew: Boolean = false)

object SceneRegistry {
    val entries: List<SceneDefinition> = listOf(
        SceneDefinition("FLUX",SceneCategory.SIGNATURE),
        SceneDefinition("HALO",SceneCategory.SIGNATURE),
        SceneDefinition("NEBULA",SceneCategory.COSMIC),
        SceneDefinition("AURORA",SceneCategory.AMBIENT,false),
        SceneDefinition("PULSE",SceneCategory.TECH,false),
        SceneDefinition("COMET",SceneCategory.COSMIC,false),
        SceneDefinition("VEIL",SceneCategory.AMBIENT,true),
        SceneDefinition("GRID",SceneCategory.TECH,true),
        SceneDefinition("SHARDS",SceneCategory.SIGNATURE,true)
    )
    fun get(id: String): SceneDefinition? = entries.firstOrNull { it.id==id }
}
