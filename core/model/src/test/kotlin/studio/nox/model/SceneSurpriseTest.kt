package studio.nox.model

import org.junit.Assert.*
import org.junit.Test

class SceneSurpriseTest {
    @Test fun alwaysSwitchesToADifferentValidScene() {
        SceneCatalog.ids.forEach { current ->
            for (entropy in listOf(Int.MIN_VALUE,-7,-1,0,1,7,1543,Int.MAX_VALUE)) {
                val next=SceneSurprise.choose(current,entropy)
                assertNotEquals(current,next.sceneId)
                assertTrue(next.sceneId in SceneCatalog.ids)
                assertEquals(next,next.normalized())
            }
        }
    }

    @Test fun resultIsStableForSameInputs() {
        assertEquals(SceneSurprise.choose("FLUX",31),SceneSurprise.choose("FLUX",31))
        assertEquals(SceneSurprise.choose("GRID",Int.MIN_VALUE),
                     SceneSurprise.choose("GRID",Int.MIN_VALUE))
    }

    @Test fun outputsCuratedVariantForNewScenes() {
        for(entropy in 0..500) {
            val config=SceneSurprise.choose("FLUX",entropy)
            if(config.sceneId in listOf("VEIL","GRID","SHARDS")) {
                assertTrue(ScenePresets.forScene(config.sceneId).any { it.config==config })
            }
        }
    }
}
