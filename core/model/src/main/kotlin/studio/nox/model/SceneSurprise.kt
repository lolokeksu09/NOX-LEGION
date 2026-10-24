package studio.nox.model

/** One-tap cross-scene discovery. Pure, deterministic, never rewrites stored presets. */
object SceneSurprise {
    fun choose(currentSceneId:String, entropy:Int):SceneConfig {
        val candidates=SceneCatalog.ids.filter {it!=currentSceneId}
        val index=(entropy.toLong() and 0x7FFFFFFFL).rem(candidates.size).toInt()
        val id=candidates[index]
        val variants=ScenePresets.forScene(id)
        if(variants.isNotEmpty()) {
            val lookIndex=((entropy.toLong() ushr 5) and 0x7FFFFFFFL).rem(variants.size).toInt()
            return variants[lookIndex].config
        }
        return SceneLooks.shuffle(SceneCatalog.default(id))
    }
}
