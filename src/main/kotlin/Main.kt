import colors.ColorSkim
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import kotlinx.cli.*
import dev.kdrag0n.colorkt.rgb.LinearSrgb as ColorKtLinearSrgb
import dev.kdrag0n.colorkt.rgb.Srgb as ColorKtSrgb
import dev.kdrag0n.colorkt.tristimulus.CieXyz as ColorKtCieXyz
import dev.kdrag0n.colorkt.tristimulus.CieXyzAbs as ColorKtCieXyzAbs
import dev.kdrag0n.colorkt.ucs.lab.CieLab as ColorKtCieLab
import dev.kdrag0n.colorkt.ucs.lab.Oklab as ColorKtOklab
import dev.kdrag0n.colorkt.ucs.lch.CieLch as ColorKtCieLch
import dev.kdrag0n.colorkt.ucs.lch.Oklch as ColorKtOklch
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import java.awt.Color as AwtColor

fun main(args: Array<String>) {

    val parser = ArgParser("colorskim")

    val input by parser.argument(
        ArgType.String,
        fullName = "input",
        description = "Input file or directory containing images"
    )

    val output by parser.option(
        ArgType.String,
        shortName = "o",
        fullName = "output",
        description = "Creates a visual representation of the output in the specified output file"
    )

    val colorSpace by parser.option(
        ArgType.Choice<ArgColorSpace>(),
        shortName = "c",
        fullName = "color-space",
        description = "Color space used to compute how close two colors are to each other. Perceptually uniform color spaces such as oklab are recommended."
    ).default(ArgColorSpace.Oklab)

    val algorithm by parser.option(
        ArgType.Choice<ArgAlgorithm>(),
        shortName = "a",
        fullName = "algorithm",
        description = "Algorithm used to compute the matching colors"
    ).default(ArgAlgorithm.Lloyd)

    val paletteSizeArg by parser.option(
        ArgType.Int,
        shortName = "p",
        fullName = "palette-size",
        description = "The size of the resulting palette. Use a single value for a fixed size. Use a range delimited by a dash, for example 3-10, to automatically evaluate the best palette size in that range."
    ).delimiter(delimiterValue = "-").default(listOf(3, 10))

    val selection by parser.option(
        ArgType.Choice<ColorSkim.ColorSelection>(),
        shortName = "s",
        fullName = "selection",
        description = "Color selection algorithm for the final palette colors. Sampled takes the best color from the image. Average makes an average of the color."
    ).default(ColorSkim.ColorSelection.Sampled)

    val resolution by parser.option(
        ArgType.Double,
        shortName = "r",
        fullName = "resolution",
        description = "Resolution from 0 to 1 of the color skim, i.e. what fraction of the pixels should be considered. The highest the number, the more precise and slow the computation"
    ).default(1.0)

    val limit by parser.option(
        ArgType.Int,
        shortName = "l",
        fullName = "limit",
        description = "Upper limit on the number of pixels for the computation. If the number of pixels times the resolution is lower than this number, this has no effect"
    ).default(100 * 100)

    val debug by parser.option(
        ArgType.Boolean,
        shortName = "d",
        fullName = "debug",
        description = "Print debug information such as execution time and algorithm specifics"
    ).default(false)

    parser.parse(args)

    val colorSkim = ColorSkim(
        algorithm = when (algorithm) {
            ArgAlgorithm.Lloyd -> ColorSkim.Algorithm.LLoyd()
            ArgAlgorithm.MacQueen -> ColorSkim.Algorithm.MacQueen()
            ArgAlgorithm.HartiganWong -> ColorSkim.Algorithm.HartiganWong
        },
        colorType = colorSpace.kClass,
    )

    val paletteSize = when (paletteSizeArg.size) {
        0 -> throw IllegalArgumentException("Invalid palette size")
        1 -> ColorSkim.PaletteSize.Fixed(paletteSizeArg[0])
        else -> ColorSkim.PaletteSize.Auto(paletteSizeArg[0]..paletteSizeArg[1])
    }

    val argFile = File(input)
    val files = if (argFile.isDirectory) {
        argFile.listFiles(FileFilter { it.isFile && !it.isHidden })!!.sortedBy { it.name }
    } else listOf(argFile)
    files.forEach { file ->
        val palette = colorSkim.computeSchemeFromImage(
            inputStream = FileInputStream(file),
            paletteSize = paletteSize,
            resolution = resolution.toFloat(),
            maxResolution = limit,
            colorSelection = selection,

            )
        val paletteAwtColors = palette.map {
            it.color.toAwtColor()
        }
        println(palette)
    }

}

private enum class ArgAlgorithm {
    Lloyd, MacQueen, HartiganWong
}

private enum class ArgColorSpace(val kClass: KClass<out Color>) {
    Oklab(ColorKtOklab::class),
    CieLab(ColorKtCieLab::class),
    Srgb(ColorKtSrgb::class),
    LinearSrgb(ColorKtLinearSrgb::class),
    CieXyz(ColorKtCieXyz::class),
    CieXyzAbs(ColorKtCieXyzAbs::class),
    Oklch(ColorKtOklch::class),
    CieLch(ColorKtCieLch::class),
}

private fun Color.toAwtColor(): AwtColor {
    val rgb = this.convert<ColorKtSrgb>()
    return AwtColor(
        rgb.r.toFloat().coerceIn(0f, 1f),
        rgb.g.toFloat().coerceIn(0f, 1f),
        rgb.b.toFloat().coerceIn(0f, 1f)
    )
}
