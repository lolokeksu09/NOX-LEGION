package studio.nox.model

import org.junit.Assert.*
import org.junit.Test

class SpectraCatalogTest {
    @Test fun nineScenesKeepOriginalSixStable() {
        assertEquals(listOf("FLUX","HALO","NEBULA","AURORA","PULSE","COMET","VEIL","GRID","SHARDS"),SceneCatalog.ids)
        assertEquals(9,SceneCatalog.ids.toSet().size)
        assertEquals(setOf("VEIL","GRID","SHARDS"),SceneRegistry.entries.filter { it.isNew }.map { it.id }.toSet())
    }

    @Test fun newScenesHaveCategoriesAndValidDefaults() {
        assertEquals(SceneCategory.AMBIENT,SceneRegistry.get("VEIL")?.category)
        assertEquals(SceneCategory.TECH,SceneRegistry.get("GRID")?.category)
        assertEquals(SceneCategory.SIGNATURE,SceneRegistry.get("SHARDS")?.category)
        listOf("VEIL","GRID","SHARDS").forEach { id ->
            val config=SceneCatalog.default(id)
            assertEquals(id,config.sceneId)
            assertEquals(id,config.normalized().sceneId)
            assertTrue(listOf(config.speed,config.glow,config.density,config.motion).all {it in 0f..1f})
        }
    }

    @Test fun nineNamedStylesAndColorsMatchContract() {
        val expected=mapOf(
            "VEIL" to listOf("Silk Blue" to (0x69DFFF to 0x5D65EA),
                             "Ether Rose" to (0xF58BCD to 0x7455CE),
                             "Jade Haze" to (0x66F7D1 to 0x177E9F)),
            "GRID" to listOf("Cyan Matrix" to (0x29E6F7 to 0x335FE8),
                             "Neon Sunset" to (0xFF875E to 0xC35CF5),
                             "Arctic Wire" to (0xD7F8FF to 0x5BAAE9)),
            "SHARDS" to listOf("Obsidian" to (0x7ACAF9 to 0x4D60E8),
                               "Amethyst" to (0xC39AFF to 0x6685FF),
                               "Prism" to (0x66FCE2 to 0xFF82BD))
        )
        expected.forEach { (scene,variants) ->
            val found=ScenePresets.forScene(scene)
            assertEquals(3,found.size)
            assertEquals(variants,found.map { it.name to (it.config.primary to it.config.secondary) })
            assertTrue(found.all{ it.config.sceneId==scene && it.config.normalized()==it.config })
            assertEquals(found,ScenePresets.forScene(scene))
        }
    }

    @Test fun portableArchiveRetainsNineOriginalConfigurations() {
        val presets=listOf("VEIL","GRID","SHARDS").flatMap(ScenePresets::forScene)
            .map { PortablePreset(it.name,it.config) }
        assertEquals(presets,PresetTransfer.decode(PresetTransfer.encode(presets)))
    }

    @Test fun existingDefaultPresetsRemainUnmodified() {
        assertEquals(SceneConfig("FLUX"),SceneCatalog.default("FLUX"))
        assertEquals(SceneConfig("HALO",0x45C6FF,0x9962FF,speed=.32f,glow=.78f,density=.45f),SceneCatalog.default("HALO"))
        assertEquals(SceneConfig("NEBULA",0x64DAFF,0x3256EF,speed=.20f,glow=.50f,density=.72f),SceneCatalog.default("NEBULA"))
    }
}
