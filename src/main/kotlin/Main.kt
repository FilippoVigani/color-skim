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
        (0 until bufferedImage.width step 20).flatMap { x ->
            (0 until bufferedImage.height step 20).map { y ->
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
    startingClusters: (k: Int, points: Array<Point>) -> Map<Int, List<Point>> = ::randomClusters
): Collection<Collection<Point>> {
    val clusters = startingClusters(k, points).mapValues { it.value.toMutableList() }
    val centroids: MutableMap<Int, Point?> = clusters.mapValues {
        centroid(it.value)
    }.toMutableMap()
    do {
        var converging = false
        for (ci in clusters) {
            val iterator = ci.value.iterator()
            while (iterator.hasNext()) {
                val xi = iterator.next()
                //val ci = clusters.filter { it.value.contains(xi) }.entries.first()
                var maxImprovement: IndexedValue<Float>? = null
                for (cj in clusters) {
                    if (cj.key != ci.key) {
                        val cti = centroids[ci.key]
                        val ctj = centroids[cj.key]
                        if (cti != null && ctj != null) {
                            val fi = costImprovement(xi, cti, ci.value.size, ctj, cj.value.size)
                            if (fi > (maxImprovement?.value ?: 0f)) {
                                maxImprovement = IndexedValue(cj.key, fi)
                            }
                        }
                    }
                }
                if (maxImprovement != null) {
                    iterator.remove()
                    val ct = clusters[maxImprovement.index]!!
                    ct.add(xi)
                    centroids[ci.key] = if (ci.value.isNotEmpty()) centroid(ci.value) else null
                    centroids[maxImprovement.index] = centroid(ct)
                    converging = true
                }
            }
        }
    } while (converging)
    return clusters.map { it.value }
}


fun randomClusters(k: Int, points: Array<Point>): Map<Int, List<Point>> {
    return points.groupBy { Random.nextInt(k) }
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
    points.forEach { point ->
        for (d in centroid.indices) {
            centroid[d] += point[d]
        }
    }
    for (d in centroid.indices) {
        centroid[d] /= points.size.toFloat()
    }
    return centroid
}