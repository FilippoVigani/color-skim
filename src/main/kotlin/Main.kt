import colors.ColorSkim
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.rgb.Srgb
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import kotlin.time.ExperimentalTime
import java.awt.Color as AwtColor

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) {
    val colorSkim = ColorSkim()

    args.forEach { path ->
        val argFile = File(path)
        val files = if (argFile.isDirectory) {
            argFile.listFiles(FileFilter { it.isFile && !it.isHidden })!!.sortedBy { it.name }
        } else listOf(argFile)
        files.forEach { file ->
            val palette = colorSkim.computeSchemeFromImage(
                inputStream = FileInputStream(file),
                paletteSize = 5,
                maxResolution = 1000 * 1000,
                colorSelection = ColorSkim.ColorSelection.Sampled
            )
            val paletteAwtColors = palette.map {
                it.color.toAwtColor()
            }
            println(palette)
        }

    }
}

private fun Color.toAwtColor(): AwtColor {
    val rgb = this.convert<Srgb>()
    return AwtColor(rgb.r.toFloat(), rgb.g.toFloat(), rgb.b.toFloat())
}
