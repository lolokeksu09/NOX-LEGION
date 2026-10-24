package studio.nox.model

import org.junit.Assert.*
import org.junit.Test

class SceneRegistryTest {
    @Test fun nineUniqueScenesWithStableLegacyOrder() {
        assertEquals(listOf("FLUX","HALO","NEBULA","AURORA","PULSE","COMET","VEIL","GRID","SHARDS"),SceneCatalog.ids)
        assertEquals(SceneCatalog.ids,SceneRegistry.entries.map { it.id })
        assertEquals(9,SceneRegistry.entries.map { it.id }.toSet().size)
    }

    @Test fun categoryAndNewStatusAreCorrect() {
        assertEquals(SceneCategory.SIGNATURE,SceneRegistry.get("FLUX")?.category)
        assertEquals(SceneCategory.SIGNATURE,SceneRegistry.get("HALO")?.category)
        assertEquals(SceneCategory.COSMIC,SceneRegistry.get("NEBULA")?.category)
        assertEquals(SceneCategory.AMBIENT,SceneRegistry.get("AURORA")?.category)
        assertEquals(SceneCategory.TECH,SceneRegistry.get("PULSE")?.category)
        assertEquals(SceneCategory.COSMIC,SceneRegistry.get("COMET")?.category)
        assertEquals(setOf("VEIL","GRID","SHARDS"),SceneRegistry.entries.filter { it.isNew }.map { it.id }.toSet())
        assertNull(SceneRegistry.get("UNKNOWN"))
    }

    @Test fun legacyDefaultsStayUnchanged() {
        assertEquals(SceneConfig("FLUX"),SceneCatalog.default("FLUX"))
        assertEquals(SceneConfig("HALO",0x45C6FF,0x9962FF,speed=0.32f,glow=0.78f,density=0.45f),SceneCatalog.default("HALO"))
        assertEquals(SceneConfig("NEBULA",0x64DAFF,0x3256EF,speed=0.20f,glow=0.50f,density=0.72f),SceneCatalog.default("NEBULA"))
    }

    @Test fun newScenesNormalizeWithoutFallingBackToFlux() {
        listOf("AURORA","PULSE","COMET","VEIL","GRID","SHARDS").forEach { id ->
            val c=SceneCatalog.default(id)
            assertEquals(id,c.sceneId)
            assertEquals(id,c.normalized().sceneId)
            assertTrue(c.speed in 0f..1f)
            assertTrue(c.glow in 0f..1f)
            assertTrue(c.density in 0f..1f)
        }
        assertEquals("FLUX",SceneConfig("INVALID").normalized().sceneId)
    }
}
