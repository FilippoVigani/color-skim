package colors

import colors.colorspace.oklab.OkLab
import kmeans.hartiganWong
import kmeans.lloyd
import kmeans.macQueen
import java.awt.Color
import java.awt.color.ColorSpace
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt

abstract class ColorSkim {
    enum class Algorithm {
        LLoyd,
        MacQueen,
        HartiganWong,
    }

    companion object {
        fun computeSchemeFromImage(
            inputStream: InputStream,
            colorSpace: ColorSpace = OkLab.instance,
            paletteSize: Int = 5,
            resolution: Float = 0.3f,
            algorithm: Algorithm = Algorithm.HartiganWong
        ): List<PaletteColor> {
            val colors = readColors(inputStream, colorSpace, resolution)
            val clusters = when (algorithm) {
                Algorithm.LLoyd -> lloyd(k = paletteSize, points = colors)
                Algorithm.MacQueen -> macQueen(k = paletteSize, points = colors)
                Algorithm.HartiganWong -> hartiganWong(k = paletteSize, points = colors)
            }
            val totalPoints = clusters.sumOf { it.points.size }
            return clusters
                .map {
                    val color = Color(ColorSpace.getInstance(ColorSpace.CS_sRGB), colorSpace.toRGB(it.centroid), 1f)
                    PaletteColor(color, it.points.size.toFloat() / totalPoints.toFloat())
                }
        }

        internal fun readColors(
            inputStream: InputStream,
            colorSpace: ColorSpace,
            resolution: Float,
        ): Array<FloatArray> {
            require(resolution > 0f && resolution <= 1f) {
                "resolution must be between 0 and 1"
            }
            val bufferedImage = ImageIO.read(inputStream)
            val pixelCount = ((bufferedImage.width * bufferedImage.height) * resolution).roundToInt()
            val colorArray = Array(pixelCount) { FloatArray(colorSpace.numComponents) }
            for (i in 0 until pixelCount) {
                val index = ((i.toFloat() / pixelCount) * (bufferedImage.width * bufferedImage.height)).roundToInt()
                val x = (index) % bufferedImage.width
                val y = (index) / bufferedImage.width
                val rgb = bufferedImage.getRGB(x, y)
                val rgbF = floatArrayOf(
                    (rgb shr 16 and 0xFF) / 255f,
                    (rgb shr 8 and 0xFF) / 255f,
                    (rgb shr 0 and 0xFF) / 255f
                )
                colorArray[i] = colorSpace.fromRGB(rgbF)
            }
            return colorArray
        }
    }
}