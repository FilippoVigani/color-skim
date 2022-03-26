import colors.colorspace.cielab.CIELab
import colors.colorspace.oklab.OkLab
import kmeans.centroid
import kmeans.hartiganWong
import kmeans.lloyd
import kmeans.macQueen
import java.awt.Color
import java.awt.color.ColorSpace
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
    val colorSpace = OkLab.instance
    val colorPixels = measureTimedValue {
        (0 until bufferedImage.width step 5).flatMap { x ->
            (0 until bufferedImage.height step 5).map { y ->
                val rgb = bufferedImage.getRGB(x, y)
                val color = Color(rgb).getColorComponents(colorSpace, null)
                color
            }
        }.toTypedArray()
    }
    println("Read pixels in ${colorPixels.duration.inWholeMilliseconds} ms")
    runLloyd(4, colorPixels.value, colorSpace)
    runMacQueen(4, colorPixels.value, colorSpace)
    runHartiganWong(6, colorPixels.value, colorSpace)
}

fun runLloyd(k: Int, colorPoints: Array<FloatArray>, colorSpace: ColorSpace) {
    val clusters = lloyd(k = k, points = colorPoints)
    val palette = clusters.map {
        if (it.isNotEmpty()) {
            val centroid = centroid(it)
            val paletteColor = colorSpace.toRGB(centroid)
            paletteColor
        } else {
            null
        }
    }
    println(palette)
}


fun runMacQueen(k: Int, colorPoints: Array<FloatArray>, colorSpace: ColorSpace) {
    val clusters = macQueen(k = k, points = colorPoints)
    val palette = clusters.map {
        if (it.isNotEmpty()) {
            val centroid = centroid(it)
            val paletteColor = colorSpace.toRGB(centroid)
            paletteColor
        } else {
            null
        }
    }
    println(palette)
}


fun runHartiganWong(k: Int, colorPoints: Array<FloatArray>, colorSpace: ColorSpace) {
    val clusters = hartiganWong(k = k, points = colorPoints)
    val palette = clusters.map {
        if (it.isNotEmpty()) {
            val centroid = centroid(it)
            val colorFloats = colorSpace.toRGB(centroid)
            val paletteColor = Color(colorFloats[0], colorFloats[1], colorFloats[2])
            paletteColor
        } else {
            null
        }
    }
    println(palette)
}

