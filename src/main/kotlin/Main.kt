import colors.ColorSkim
import colors.colorspace.cielab.CIELab
import colors.colorspace.oklab.OkLab
import kmeans.centroid
import kmeans.hartiganWong
import kmeans.lloyd
import kmeans.macQueen
import java.awt.Color
import java.awt.color.ColorSpace
import java.io.FileInputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) {
    println("Hello World!")

    val cieLab = CIELab.instance
    val okLab = OkLab.instance

    args.forEach { imagePath ->
        ColorSkim.Algorithm.values().forEach { algorithm ->
            val palette = ColorSkim.computeSchemeFromImage(
                inputStream = FileInputStream(imagePath),
                colorSpace = okLab,
                paletteSize = 9,
                resolution = 0.1f,
                algorithm = algorithm
            )
            println(palette)
        }
    }
}

