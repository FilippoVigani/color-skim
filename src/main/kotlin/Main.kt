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
    val colorSkim = ColorSkim(algorithm = ColorSkim.Algorithm.LLoyd())

    args.forEach { path ->
        val argFile = File(path)
        val files = if (argFile.isDirectory) {
            argFile.listFiles(FileFilter { it.isFile && !it.isHidden })!!.sortedBy { it.name }
        } else listOf(argFile)
        files.forEach { file ->
            val palette = colorSkim.computeSchemeFromImage(
                inputStream = FileInputStream(file),
                paletteSize = ColorSkim.PaletteSize.Auto(3..15),
                maxResolution = 100 * 100,
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
    return AwtColor(rgb.r.toFloat().coerceIn(0f, 1f), rgb.g.toFloat().coerceIn(0f, 1f), rgb.b.toFloat().coerceIn(0f, 1f))
}
