package colors

import colors.colorspace.getComponents
import colors.colorspace.toColor
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.rgb.Srgb
import kmeans.InitialPointsSelector
import kmeans.hartiganWong
import kmeans.initialization.kmeansPlusPlus
import kmeans.lloyd
import kmeans.macQueen
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.reflect.KClass

abstract class ColorSkim {
    sealed class Algorithm {
        data class LLoyd(val initialPointsSelector: InitialPointsSelector = ::kmeansPlusPlus) : Algorithm()
        data class MacQueen(val initialPointsSelector: InitialPointsSelector = ::kmeansPlusPlus) : Algorithm()
        object HartiganWong : Algorithm()
    }

    companion object {
        fun computeSchemeFromImage(
            inputStream: InputStream,
            colorType: KClass<out Color>,
            paletteSize: Int = 5,
            resolution: Float = 0.2f,
            algorithm: Algorithm = Algorithm.HartiganWong
        ): List<PaletteColor> {
            val colors = readColors(inputStream, colorType, resolution)
            val clusters = when (algorithm) {
                is Algorithm.LLoyd -> lloyd(
                    k = paletteSize,
                    points = colors,
                    selectIndexes = algorithm.initialPointsSelector
                )
                is Algorithm.MacQueen -> macQueen(
                    k = paletteSize,
                    points = colors,
                    selectIndexes = algorithm.initialPointsSelector
                )
                Algorithm.HartiganWong -> hartiganWong(k = paletteSize, points = colors)
            }
            val totalPoints = clusters.sumOf { it.points.size }
            return clusters
                .map {
                    val color = it.centroid.toColor(colorType)
                    PaletteColor(color, it.points.size.toDouble() / totalPoints)
                }
        }

        internal fun readColors(
            inputStream: InputStream,
            colorType: KClass<out Color>,
            resolution: Float,
        ): Array<DoubleArray> {
            require(resolution > 0f && resolution <= 1f) {
                "resolution must be between 0 and 1"
            }
            val actualResolution = resolution.pow(2)
            val bufferedImage = ImageIO.read(inputStream)
            val pixelCount = ((bufferedImage.width * bufferedImage.height) * actualResolution).roundToInt()
            val colorArray = Array(pixelCount) { DoubleArray(3) }
            for (i in 0 until pixelCount) {
                val index = ((i.toFloat() / pixelCount) * (bufferedImage.width * bufferedImage.height)).roundToInt()
                val x = (index) % bufferedImage.width
                val y = (index) / bufferedImage.width
                val rgb = bufferedImage.getRGB(x, y)
                val srgb = Srgb(rgb)
                val color = convert(srgb, colorType)
                colorArray[i] = color!!.getComponents()
            }
            return colorArray
        }
    }
}