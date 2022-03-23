import java.awt.Color
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) {
    println("Hello World!")

    val easyClusters = (0 until 30).map { floatArrayOf(it / 10 + it * 0.1f, it / 10 + it * 0.1f) }.toTypedArray()

    val bufferedImage = ImageIO.read(FileInputStream(args.first()))

    val hsbs = measureTimedValue {
        (0 until bufferedImage.width step 5).flatMap { x ->
            (0 until bufferedImage.height step 5).map { y ->
                val rgb = bufferedImage.getRGB(x, y)
                val color = Color(rgb)
                val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
                hsb
            }
        }
    }
    println("Read pixels in ${hsbs.duration.inWholeMilliseconds} ms")
    val palette = measureTimedValue {
        val clusters = hartiganWong(k = 10, hsbs.value.toTypedArray())
        clusters.map {
            if (it.isNotEmpty()) {
                val centroid = centroid(it)
                val paletteColor = Color(Color.HSBtoRGB(centroid[0], centroid[1], centroid[2]))
                paletteColor
            } else {
                null
            }
        }
    }
    println("It took ${palette.duration.inWholeMilliseconds} ms")
    println(palette.value)
}

typealias Point = FloatArray

fun lloyd(k: Int, points: List<Point>): Collection<Collection<Point>> {
    TODO()
}

fun hartiganWong(
    k: Int,
    points: Array<Point>,
    selectClustersIndex: (k: Int, points: Array<Point>) -> Array<Int> = ::randomClusters
): Collection<Collection<Point>> {
    val clusterIndexes = selectClustersIndex(k, points)
    val (clusterSizes, centroids) = computeClustersSizesAndCentroids(k, points, clusterIndexes)
    var iterations = 0
    var lastConvergedPointIndex = -1
    var pointIndex = 0
    do {
        iterations++
        val clusterIndex = clusterIndexes[pointIndex]
        val currentClusterSize = clusterSizes[clusterIndex]
        val point = points[pointIndex]
        var maxImprovement: IndexedValue<Float>? = null
        for (targetClusterIndex in 0 until k) {
            if (targetClusterIndex != clusterIndex) {
                val sourceCentroid = centroids[clusterIndex]
                val targetCentroid = centroids[targetClusterIndex]
                if (sourceCentroid != null && targetCentroid != null) {
                    val improvement = costImprovement(point, sourceCentroid, currentClusterSize, targetCentroid, clusterSizes[targetClusterIndex])
                    if (improvement > (maxImprovement?.value ?: 0f)) {
                        maxImprovement = IndexedValue(targetClusterIndex, improvement)
                    }
                }
            }
        }
        val targetClusterIndex = maxImprovement?.index
        if (targetClusterIndex != null) {
            centroids[clusterIndex]?.let {
                if (currentClusterSize > 1) {
                    removePointFromCentroid(it, point, currentClusterSize)
                } else {
                    centroids[clusterIndex] = null
                }
            }
            centroids[targetClusterIndex]?.let {
                addPointToCentroid(it, point, clusterSizes[targetClusterIndex])
            }
            clusterIndexes[pointIndex] = targetClusterIndex
            clusterSizes[clusterIndex]--
            clusterSizes[targetClusterIndex]++

            lastConvergedPointIndex = pointIndex
        }
        if (lastConvergedPointIndex == -1 && pointIndex == points.size - 1) {
            lastConvergedPointIndex = 0
        }
        pointIndex = (pointIndex + 1) % points.size
    } while (pointIndex != lastConvergedPointIndex)
    println("iterations=$iterations")
    val clusters = List(k) { mutableListOf<Point>() }
    for (p in points.indices) {
        clusters[clusterIndexes[p]].add(points[p])
    }
    return clusters.sortedByDescending { it.size }
}

fun computeClustersSizesAndCentroids(
    k: Int,
    points: Array<Point>,
    clusterIndexes: Array<Int>
): Pair<Array<Int>, Array<Point?>> {
    val centroids = Array<Point?>(k) { Point(points.first().size) }
    val clusterSizes = Array(k) { 0 }
    for (i in points.indices) {
        val point = points[i]
        val clusterIndex = clusterIndexes[i]
        clusterSizes[clusterIndex]++
        for (d in centroids[clusterIndex]!!.indices) {
            centroids[clusterIndex]!![d] += point[d]
        }
    }
    for (j in centroids.indices) {
        for (d in centroids[j]!!.indices) {
            val size = clusterSizes[j]
            if (size > 0) {
                centroids[j]?.set(d, centroids[j]!![d] / size)
            } else {
                centroids[j] = null
            }
        }
    }
    return clusterSizes to centroids
}


fun randomClusters(k: Int, points: Array<Point>): Array<Int> {
    //Use a fixed seed so that it's easier to compare iterations count with the same input data
    val random = Random(0)
    return Array(points.size) { random.nextInt(k) }
}

fun costImprovement(point: Point, sourceCentroid: Point, sourceSize: Int, destinationCentroid: Point, destinationSize: Int): Float {
    return sourceSize * euclideanDistanceSquared(sourceCentroid, point) / (sourceSize + 1) -
            destinationSize * euclideanDistanceSquared(destinationCentroid, point) / (destinationSize + 1)
}

fun euclideanDistanceSquared(x: Point, y: Point): Float {
    var total = 0f
    for (i in x.indices) {
        total += (x[i] - y[i]).pow(2)
    }
    return total
}

fun centroid(points: Collection<Point>): Point {
    if (points.isEmpty()) {
        throw IllegalArgumentException("Can't calculate centroid of empty set")
    }
    val centroid = points.first().copyOf()
    points.drop(1).forEach { point ->
        for (d in centroid.indices) {
            centroid[d] += point[d]
        }
    }
    for (d in centroid.indices) {
        centroid[d] /= points.size.toFloat()
    }
    return centroid
}


fun addPointToCentroid(centroid: Point, point: Point, previousClusterSize: Int) {
    for (d in centroid.indices) {
        centroid[d] = (centroid[d] * previousClusterSize + point[d]) / (previousClusterSize + 1)
    }
}

fun removePointFromCentroid(centroid: Point, point: Point, previousClusterSize: Int) {
    if (previousClusterSize == 1) {
        throw IllegalArgumentException("Can't calculate centroid of empty set")
    }
    for (d in centroid.indices) {
        centroid[d] = (centroid[d] * previousClusterSize - point[d]) / (previousClusterSize - 1)
    }
}