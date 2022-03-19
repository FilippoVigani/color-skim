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

    val bufferedImage = ImageIO.read(FileInputStream(args.first()))

    val hsbs = measureTimedValue {
        (0 until bufferedImage.width step 50).flatMap { x ->
            (0 until bufferedImage.height step 50).map { y ->
                val rgb = bufferedImage.getRGB(x, y)
                val color = Color(rgb)
                val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
                hsb
            }
        }
    }
    println("Read pixels in ${hsbs.duration.inWholeMilliseconds} ms")
    val palette = measureTimedValue {
        val sets = hartiganWong(k = 10, hsbs.value)
        sets.map {
            val centroid = centroid(it)
            val paletteColor = Color(Color.HSBtoRGB(centroid[0], centroid[1], centroid[2]))
            paletteColor
        }
    }
    println("It took ${palette.duration.inWholeMilliseconds} ms")
    println(palette.value)
}

typealias Point = FloatArray

fun hartiganWong(k: Int = 4, points: List<Point>): Set<Set<Point>> {
    val n = points.size
    val x = points
    val y = (points.indices).map { Random.nextInt(k) }.toIntArray()
    val C: (Int) -> Set<Point> = {
        points.filterIndexed { i, v ->
            y[i] == it
        }.toSet()
    }
    var wasUpdated = true
    while (wasUpdated) {
        wasUpdated = false
        for (i in 0 until n) {
            val Cyi = C(y[i])
            val costImprovements = (0 until k)
                .filter { it != y[i] }
                .mapNotNull { j ->
                    val Cj = C(j)
                    if (Cj.isNotEmpty()) {
                        val fi = costImprovement(x[i], Cyi, Cj)
                        IndexedValue(j, fi)
                    } else null

                }
            val maxCostImprovement = costImprovements.maxByOrNull { it.value }
            if (maxCostImprovement != null && maxCostImprovement.value > 0f) {
                y[i] = maxCostImprovement.index
                wasUpdated = true
            }
        }
    }
    return points.withIndex().groupBy {
        y[it.index]
    }.map { it.value.map { it.value }.toSet() }.toSet()
}

fun costImprovement(x: Point, S: Set<Point>, T: Set<Point>): Float {
    if (S == T) {
        return 0f
    }
    return S.size * euclideanDistanceSquared(centroid(S), x) / (S.size + 1) -
            T.size * euclideanDistanceSquared(centroid(T), x) / (T.size + 1)
}

fun euclideanDistanceSquared(x: Point, y: Point): Float {
    var total = 0f
    for (i in x.indices) {
        total += (x[i] - y[i]).pow(2)
    }
    return total
}

fun centroid(points: Set<Point>): Point {
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