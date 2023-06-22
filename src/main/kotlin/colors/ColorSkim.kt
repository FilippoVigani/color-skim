package colors

import colors.colorspace.getComponents
import colors.colorspace.toColor
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.colorkt.ucs.lab.Oklab
import kmeans.*
import kmeans.evaluation.estimateBestK
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

/**
 * Class used to generate a palette of colors from an input image.
 *
 * @property colorType The color type or color space used to represent the colors from the image
 * @property algorithm The algorithm used to compute the clusters of colors
 * @property random Random number generation implementation
 */
class ColorSkim(
    val colorType: KClass<out Color> = Oklab::class,
    val algorithm: Algorithm = Algorithm.HartiganWong,
    val random: Random = Random.Default,
) {
    /**
     * Algorithm used to compute the clusters of points. They are all variations of the k-means clustering algorithm.
     *
     * They are all heuristic algorithms and are prone to find only a locally optimal solution.
     * The main difference is how precise (and how computationally intensive) the clustering is, where [Algorithm.LLoyd] is the
     * fastest but least precise, and [Algorithm.HartiganWong] is the slowest but most precise. [Algorithm.MacQueen] is
     * in the middle.
     *
     * [Algorithm.LLoyd] and [Algorithm.MacQueen] can specify the algorithm to select the initial set of centers
     * from which they compute the subsequent clusters, with varying results in terms of accuracy and speed.
     *
     */
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


    /**
     * The color selection used to extract the final color of the palette from the clusters of colors.
     * [ColorSelection.Sampled] ensures that the selected color is the color of a pixel from the image, whereas
     * [ColorSelection.Average] takes an average of all the colors from a cluster.
     *
     */
    enum class ColorSelection {
        Average, Sampled
    }

    /**
     * Class representing the size of an output palette. [PaletteSize.Fixed] always uses a specific number of colors,
     * whereas [PaletteSize.Auto] finds the best matching size from a range.
     *
     */
    sealed class PaletteSize {
        data class Fixed(val size: Int) : PaletteSize()
        data class Auto(val range: IntRange) : PaletteSize()
    }

    /**
     * Computes a palette of colors from the input stream of an image
     * @param inputStream the input stream representing the image
     * @param paletteSize the size of the output palette
     * @param resolution the fraction of the pixels used to compute the palette. Must be between 0 and 1
     * @param maxResolution the upper limit on the number of pixels used to compute the palette. If the number of pixels
     * times the resolution is less than this value, this has no effect
     * @param colorSelection the color selection used to extract the final color of the palette from the clusters of
     * colors
     * @return a list of palette colors, where each element has a color in the specified color space and its prevalence
     * from the input image
     */
    fun computeSchemeFromImage(
        inputStream: InputStream,
        paletteSize: PaletteSize,
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

    private fun computeSchemeFromImage(
        inputStream: InputStream,
        paletteSize: PaletteSize,
        maxResolution: Int,
        resolution: Float,
        colorType: KClass<out Color>,
        algorithm: Algorithm,
        random: Random,
        colorSelection: ColorSelection,
    ): List<PaletteColor> {
        // Read the colors from the image, store them as an array of components (e.g. R,G,B)

        val colors = readColors(
            inputStream = inputStream,
            colorType = colorType,
            resolution = resolution,
            maxResolution = maxResolution
        )
        val kRange = when (paletteSize) {
            is PaletteSize.Auto -> IntRange(
                start = (paletteSize.range.first - 1)
                    .coerceAtLeast(1),
                endInclusive = paletteSize.range.last + 1
            )
            is PaletteSize.Fixed -> paletteSize.size..paletteSize.size
        }

        // Compute the k means clusters for each number of clusters (the palette size)

        val results = kRange.map {
            when (algorithm) {
                is Algorithm.LLoyd -> lloyd(
                    k = it,
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
                    k = it,
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
                    k = it,
                    points = colors,
                    selectClustersIndexes = { k, points ->
                        randomClusters(k, points, random)
                    }
                )
            }
        }

        // Find the best number of clusters from the results based on their elbow variance

        val result = when (paletteSize) {
            is PaletteSize.Fixed -> results.first()
            is PaletteSize.Auto -> {
                val minElbowVariance = results.minOf { it.elbowVariance }
                val maxElbowVariance = results.maxOf { it.elbowVariance }
                val normalizedMeanDistortions =
                    results.map { (it.elbowVariance - minElbowVariance) / (maxElbowVariance - minElbowVariance) }
                val bestK = estimateBestK(
                    paletteSize.range.first,
                    paletteSize.range.last,
                    normalizedMeanDistortions.toTypedArray()
                )
                results[bestK]
            }
        }

        // Return the sampled colors from the clusters of colors, along with its prevalence
        // (i.e. the size of each individual cluster)

        return result.clusters
            .map { cluster ->
                val color = when (colorSelection) {
                    ColorSelection.Average -> cluster.centroid.toColor(colorType)
                    ColorSelection.Sampled -> cluster.points
                        .minByOrNull { euclideanDistanceSquared(it, cluster.centroid) }!!
                        .toColor(colorType)
                }

                PaletteColor(
                    color = color,
                    prevalence = cluster.points.size.toDouble() / result.numberOfPoints
                )
            }
    }

    /**
     * Reads an array of colored pixels into memory from an input stream of an image, using the specified color space representation.
     *
     * @param inputStream the input stream of the image
     * @param colorType the color space the color is represented in
     * @param resolution the fraction of the pixels used
     * @param maxResolution an upper limit on the number of pixels extracted
     * @return an array of components of colors
     */
    private fun readColors(
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
            ((bufferedImage.width * bufferedImage.height) * actualResolution).roundToInt()
                .coerceAtMost(maxResolution)
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