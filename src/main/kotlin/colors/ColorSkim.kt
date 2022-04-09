package colors

import colors.colorspace.getComponents
import colors.colorspace.toColor
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.colorkt.ucs.lab.Oklab
import kmeans.*
import kmeans.hartiganWong
import kmeans.initialization.kmeansPlusPlus
import kmeans.initialization.randomPointsIndexes
import kmeans.initialization.scalableKMeans
import kmeans.lloyd
import kmeans.macQueen
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.KClass

class ColorSkim(
    val colorType: KClass<out Color> = Oklab::class,
    val algorithm: Algorithm = Algorithm.HartiganWong,
    val random: Random = Random.Default,
) {
    sealed class Algorithm {
        data class LLoyd(val initialPointsSelection: InitialSelection = InitialSelection.KmeansPlusPlus) : Algorithm() {
            enum class InitialSelection {
                Random,
                KmeansPlusPlus,
                ScalableKmeans
            }
        }

        data class MacQueen(val initialPointsSelection: InitialSelection = InitialSelection.KmeansPlusPlus) :
            Algorithm() {
            enum class InitialSelection {
                Random,
                KmeansPlusPlus,
                ScalableKmeans
            }
        }

        object HartiganWong : Algorithm()
    }

    enum class ColorSelection {
        Average, Sampled
    }

    fun computeSchemeFromImage(
        inputStream: InputStream,
        paletteSize: Int,
        resolution: Float = 1f,
        maxResolution: Int = Int.MAX_VALUE,
        colorSelection: ColorSelection = ColorSelection.Average
    ): List<PaletteColor> {
        return computeSchemeFromImage(
            inputStream = inputStream,
            paletteSize = paletteSize,
            resolution = resolution,
            maxResolution = maxResolution,
            colorType = this.colorType,
            algorithm = this.algorithm,
            colorSelection = colorSelection,
            random = this.random,
        )
    }

    internal fun computeSchemeFromImage(
        inputStream: InputStream,
        paletteSize: Int,
        maxResolution: Int,
        resolution: Float,
        colorType: KClass<out Color>,
        algorithm: Algorithm,
        random: Random,
        colorSelection: ColorSelection,
    ): List<PaletteColor> {
        val colors = readColors(
            inputStream = inputStream,
            colorType = colorType,
            resolution = resolution,
            maxResolution = maxResolution
        )
        val clusters = when (algorithm) {
            is Algorithm.LLoyd -> lloyd(
                k = paletteSize,
                points = colors,
                selectIndexes = when (algorithm.initialPointsSelection) {
                    Algorithm.LLoyd.InitialSelection.Random -> { k, points ->
                        randomPointsIndexes(k = k, points = points, random = random)
                    }
                    Algorithm.LLoyd.InitialSelection.KmeansPlusPlus -> { k, points ->
                        kmeansPlusPlus(k = k, points = points, random = random)
                    }
                    Algorithm.LLoyd.InitialSelection.ScalableKmeans -> { k, points ->
                        scalableKMeans(k = k, points = points, l = 5f, random = random)
                    }
                }
            )
            is Algorithm.MacQueen -> macQueen(
                k = paletteSize,
                points = colors,
                selectIndexes = when (algorithm.initialPointsSelection) {
                    Algorithm.MacQueen.InitialSelection.Random -> { k, points ->
                        randomPointsIndexes(k = k, points = points, random = random)
                    }
                    Algorithm.MacQueen.InitialSelection.KmeansPlusPlus -> { k, points ->
                        kmeansPlusPlus(k = k, points = points, random = random)
                    }
                    Algorithm.MacQueen.InitialSelection.ScalableKmeans -> { k, points ->
                        scalableKMeans(k = k, points = points, l = 5f, random = random)
                    }
                }
            )
            Algorithm.HartiganWong -> hartiganWong(
                k = paletteSize,
                points = colors,
                selectClustersIndexes = { k, points ->
                    randomClusters(k, points, random)
                }
            )
        }
        val totalPoints = clusters.sumOf { it.points.size }
        return clusters
            .map { cluster ->
                val color = when (colorSelection) {
                    ColorSelection.Average -> cluster.centroid.toColor(colorType)
                    ColorSelection.Sampled -> cluster.points
                        .minByOrNull { euclideanDistanceSquared(it, cluster.centroid) }!!
                        .toColor(colorType)

                }

                PaletteColor(
                    color = color,
                    prevalence = cluster.points.size.toDouble() / totalPoints
                )
            }
    }

    internal fun readColors(
        inputStream: InputStream,
        colorType: KClass<out Color>,
        resolution: Float,
        maxResolution: Int,
    ): Array<DoubleArray> {
        require(resolution > 0f && resolution <= 1f) {
            "resolution must be between 0 and 1"
        }
        val actualResolution = resolution.pow(2)
        val bufferedImage = ImageIO.read(inputStream)
        val pixelCount =
            ((bufferedImage.width * bufferedImage.height) * actualResolution).roundToInt().coerceAtMost(maxResolution)
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