import colors.ColorSkim
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.colorkt.ucs.lab.Oklab
import kmeans.initialization.kmeansPlusPlus
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) {
    println("Hello World!")

    args.forEach { path ->
        val file = File(path)
        val files = if (file.isDirectory) {
            file.listFiles(FileFilter { it.isFile && !it.isHidden })
        } else arrayOf(file)
        files.forEach {
            val palette = ColorSkim.computeSchemeFromImage(
                inputStream = FileInputStream(it),
                colorType = Oklab::class,
                paletteSize = 5,
                resolution = 0.5f,
                algorithm = ColorSkim.Algorithm.HartiganWong
            )
            val palette2 = ColorSkim.computeSchemeFromImage(
                inputStream = FileInputStream(it),
                colorType = Oklab::class,
                paletteSize = 5,
                resolution = 0.5f,
                algorithm = ColorSkim.Algorithm.LLoyd()
            )
            val palette3 = ColorSkim.computeSchemeFromImage(
                inputStream = FileInputStream(it),
                colorType = Oklab::class,
                paletteSize = 5,
                resolution = 0.5f,
                algorithm = ColorSkim.Algorithm.MacQueen()
            )
            val paletteAwtColors = palette.map {
                val rgb = it.color.convert<Srgb>()
                java.awt.Color(rgb.r.toFloat(), rgb.g.toFloat(), rgb.b.toFloat())
            }
            val palette2AwtColors = palette2.map {
                val rgb = it.color.convert<Srgb>()
                java.awt.Color(rgb.r.toFloat(), rgb.g.toFloat(), rgb.b.toFloat())
            }
            val palette3AwtColors = palette3.map {
                val rgb = it.color.convert<Srgb>()
                java.awt.Color(rgb.r.toFloat(), rgb.g.toFloat(), rgb.b.toFloat())
            }
            println(palette)
        }

    }
}

