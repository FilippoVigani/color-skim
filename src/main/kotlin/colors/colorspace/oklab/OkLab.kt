package colors.colorspace.oklab

import java.awt.color.ColorSpace
import kotlin.math.pow

class OkLab : ColorSpace(TYPE_Lab, 3) {
    override fun toRGB(colorvalue: FloatArray): FloatArray {
        val L = colorvalue[0]
        val a = colorvalue[1]
        val b = colorvalue[2]
        val l_ = L + 0.3963377774 * a + 0.2158037573 * b
        val m_ = L - 0.1055613458 * a - 0.0638541728 * b
        val s_ = L - 0.0894841775 * a - 1.2914855480 * b

        val l = l_.pow(3)
        val m = m_.pow(3)
        val s = s_.pow(3)

        return floatArrayOf(
            f(+4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s).toFloat(),
            f(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s).toFloat(),
            f(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s).toFloat(),
        )
    }

    override fun fromRGB(rgbvalue: FloatArray): FloatArray {
        val r = fInv(rgbvalue[0].toDouble())
        val g = fInv(rgbvalue[1].toDouble())
        val b = fInv(rgbvalue[2].toDouble())
        val l = +0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
        val m = +0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
        val s = +0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b

        val l_ = l.pow(1.0 / 3)
        val m_ = m.pow(1.0 / 3)
        val s_ = s.pow(1.0 / 3)

        return floatArrayOf(
            (+0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_).toFloat(),
            (+1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_).toFloat(),
            (+0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_).toFloat()
        )
    }

    override fun toCIEXYZ(colorvalue: FloatArray): FloatArray {
        val L = colorvalue[0]
        val a = colorvalue[1]
        val b = colorvalue[2]
        val l_: Float = +1 * L + 0.396338f * a +0.215804f * b
        val m_: Float = +1 * L -0.105561f * a -0.0638542f * b
        val s_: Float = +1 * L -0.0894842f * a -1.29149f * b

        val l = l_.pow(3)
        val m = m_.pow(3)
        val s = s_.pow(3)


        return floatArrayOf(
            +1.22701f * l -0.5578f * m +0.281256f * s,
            -0.0405802f * l + 1.11226f * m -0.0716767f * s,
            -0.0763813f * l -0.421482f * m +1.58616f * s,
        )

    }

    override fun fromCIEXYZ(colorvalue: FloatArray): FloatArray {
        val x = colorvalue[0]
        val y = colorvalue[1]
        val z = colorvalue[2]
        val l: Float = +0.8189330101f * x +0.3618667424f * y -0.1288597137f * z
        val m: Float = + 0.0329845436f * x + 0.9293118715f * y + 0.0361456387f * z
        val s: Float = + 0.0482003018f * x + 0.2643662691f * y + 0.6338517070f * z

        val l_: Float = l.pow(1f / 3f)
        val m_: Float = m.pow(1f / 3f)
        val s_: Float = s.pow(1f / 3f)

        return floatArrayOf(
            +0.2104542553f * l_ +0.7936177850f * m_ -0.0040720468f * s_,
            + 1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
            + 0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_,
        )
    }

    private fun f(x: Double): Double {
        if (x >= 0.0031308) {
            return 1.055 * x.pow(1.0 / 2.4) - 0.055
        }
        return x * 12.92
    }

    private fun fInv(x: Double): Double {
        if (x >= 0.04045) {
            return ((x + 0.055) / (1 + 0.055)).pow(2.4)
        }
        return x / 12.92
    }

    companion object {
        val instance by lazy { OkLab() }
    }
}