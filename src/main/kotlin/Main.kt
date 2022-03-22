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
        val sets = hartiganWong(k = 4, hsbs.value.toTypedArray())
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
    selectClusters: (k: Int, points: Array<Point>) -> Collection<Collection<Point>> = ::randomClusters
): Collection<Collection<Point>> {
    val clusters = selectClusters(k, points).map { it.toMutableList() }
    val centroids: MutableList<Point?> = clusters.map {
        centroid(it)
    }.toMutableList()
    do {
        var converging = false
        for (i in clusters.indices) {
            val ci = clusters[i]
            val iterator = ci.iterator()
            while (iterator.hasNext()) {
                val xi = iterator.next()
                var maxImprovement: IndexedValue<Float>? = null
                for (j in clusters.indices) {
                    if (j != i) {
                        val cj = clusters[j]
                        val cti = centroids[i]
                        val ctj = centroids[j]
                        if (cti != null && ctj != null) {
                            val fi = costImprovement(xi, cti, ci.size, ctj, cj.size)
                            if (fi > (maxImprovement?.value ?: 0f)) {
                                maxImprovement = IndexedValue(j, fi)
                            }
                        }
                    }
                }
                if (maxImprovement != null) {
                    centroids[i]?.let {
                        if (ci.size > 1) {
                            removePointFromCentroid(it, xi, ci.size)
                        } else {
                            centroids[i] = null
                        }
                    }
                    iterator.remove()
                    val ct = clusters[maxImprovement.index]!!
                    centroids[maxImprovement.index]?.let {
                        addPointToCentroid(it, xi, ct.size)
                    }
                    ct.add(xi)
                    converging = true
                }
            }
        }
    } while (converging)
    return clusters.map { it }
}


fun randomClusters(k: Int, points: Array<Point>): Collection<Collection<Point>> {
    return points.groupBy { Random.nextInt(k) }.values
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