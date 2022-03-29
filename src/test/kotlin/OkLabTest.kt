import colors.ColorSkim
import colors.colorspace.oklab.OkLab
import java.awt.Color
import java.awt.color.ColorSpace
import kotlin.test.Test
import kotlin.test.assertEquals

class OkLabTest {

    @Test
    fun testCIEXYZColorConversion(){
        val okLab = OkLab.instance
        val okLabColor = okLab.fromCIEXYZ(floatArrayOf(0.950f, 1.000f, 1.089f))

        assertEquals(1.000f, okLabColor[0], 0.001f)
        assertEquals(0.000f, okLabColor[1], 0.001f)
        assertEquals(0.000f, okLabColor[2], 0.001f)
    }


    @Test
    fun testRGBColorConversion(){
        val okLab = OkLab.instance
        val rgb = Color(255, 99, 183)
        val rgbF = rgb.getRGBColorComponents(null)
        val okLabColor = okLab.fromRGB(rgbF)
        assertEquals(0.800f, okLabColor[0], 0.001f)
        assertEquals(0.250f, okLabColor[1], 0.001f)
        assertEquals(0.000f, okLabColor[2], 0.001f)
    }
}