package studio.nox.model

import org.junit.Assert.*
import org.junit.Test

class VisualQualityTest {
    @Test fun qualityClampsUnexpectedStoredValues() {
        assertEquals(1,VisualQuality.normalized(-1))
        assertEquals(0,VisualQuality.normalized(0))
        assertEquals(1,VisualQuality.normalized(1))
        assertEquals(2,VisualQuality.normalized(2))
        assertEquals(1,VisualQuality.normalized(100))
    }
    @Test fun qualityFallsBackWhenDeviceIsStressed() {
        assertEquals(2,VisualQuality.effective(2,false,0))
        assertEquals(0,VisualQuality.effective(2,true,0))
        assertEquals(0,VisualQuality.effective(2,false,2))
        assertEquals(0,VisualQuality.effective(1,false,3))
        assertEquals(0,VisualQuality.effective(0,false,0))
    }
}
