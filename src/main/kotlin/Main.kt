import kmeans.centroid
import kmeans.hartiganWong
import kmeans.lloyd
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
        }.toTypedArray()
    }
    println("Read pixels in ${hsbs.duration.inWholeMilliseconds} ms")
    runLloyd(4, hsbs.value)
    runHartiganWong(4, hsbs.value)
}

fun runLloyd(k: Int, hsbs: Array<FloatArray>) {
    val clusters = lloyd(k = k, hsbs)
    val palette = clusters.map {
        if (it.isNotEmpty()) {
            val centroid = centroid(it)
            val paletteColor = Color(Color.HSBtoRGB(centroid[0], centroid[1], centroid[2]))
            paletteColor
        } else {
            null
        }
    }
    println(palette)
}


fun runHartiganWong(k: Int, hsbs: Array<FloatArray>) {
    val clusters = hartiganWong(k = k, hsbs)
    val palette = clusters.map {
        if (it.isNotEmpty()) {
            val centroid = centroid(it)
            val paletteColor = Color(Color.HSBtoRGB(centroid[0], centroid[1], centroid[2]))
            paletteColor
        } else {
            null
        }
    }
    println(palette)
}

