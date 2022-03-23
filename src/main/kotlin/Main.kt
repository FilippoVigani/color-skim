import java.awt.Color
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) {
    println("Hello World!")

    val easyClusters = (0 until 30).map { floatArrayOf(it / 10 + it * 0.1f, it / 10 + it * 0.1f) }.toTypedArray()

    val bufferedImage = ImageIO.read(FileInputStream(args.first()))

    val hsbs = measureTimedValue {
        (0 until bufferedImage.width step 10).flatMap { x ->
            (0 until bufferedImage.height step 10).map { y ->
                val rgb = bufferedImage.getRGB(x, y)
                val color = Color(rgb)
                val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
                hsb
            }
        }
    }
    println("Read pixels in ${hsbs.duration.inWholeMilliseconds} ms")
    val palette = measureTimedValue {
        val sets = hartiganWong(k = 10, hsbs.value.toTypedArray())
        sets.map {
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
    val clusterIndex = selectClustersIndex(k, points)
    val clusterSize = Array(k) { 0 }
    for (i in points.indices) {
        clusterSize[clusterIndex[i]]++
    }
    val centroids: Array<Point?> = computeCentroids(k, points, clusterIndex)
    var iterations = 0
    var lastConvergedIndex = -1
    var p = 0
    do {
        iterations++
        val i = clusterIndex[p]
        val currentClusterSize = clusterSize[i]
        val x = points[p]
        var maxImprovement: IndexedValue<Float>? = null
        for (j in 0 until k) {
            if (j != i) {
                val targetClusterSize = clusterSize[j]
                val cti = centroids[i]
                val ctj = centroids[j]
                if (cti != null && ctj != null) {
                    val fi = costImprovement(x, cti, currentClusterSize, ctj, targetClusterSize)
                    if (fi > (maxImprovement?.value ?: 0f)) {
                        maxImprovement = IndexedValue(j, fi)
                    }
                }
            }
        }
        if (maxImprovement != null) {
            centroids[i]?.let {
                if (currentClusterSize > 1) {
                    removePointFromCentroid(it, x, currentClusterSize)
                } else {
                    centroids[i] = null
                }
            }
            centroids[maxImprovement.index]?.let {
                addPointToCentroid(it, x, clusterSize[maxImprovement.index])
            }
            clusterIndex[p] = maxImprovement.index
            clusterSize[i]--
            clusterSize[maxImprovement.index]++

            lastConvergedIndex = p
        }
        if (lastConvergedIndex == -1 && p == points.size - 1) {
            lastConvergedIndex = 0
        }
        p = (p + 1) % points.size
    } while (p != lastConvergedIndex)
    println("iterations=$iterations")
    val clusters = List(k) { mutableListOf<Point>() }
    for (p in points.indices) {
        clusters[clusterIndex[p]].add(points[p])
    }
    return clusters
}

fun computeCentroids(k: Int, points: Array<Point>, clusterIndexes: Array<Int>): Array<Point?> {
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
    return centroids
}


fun randomClusters(k: Int, points: Array<Point>): Array<Int> {
    return Array(points.size) { Random.nextInt(k) }
}

fun costImprovement(x: Point, centroidS: Point, sizeS: Int, centroidT: Point, sizeT: Int): Float {
    return sizeS * euclideanDistanceSquared(centroidS, x) / (sizeS + 1) -
            sizeT * euclideanDistanceSquared(centroidT, x) / (sizeT + 1)
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