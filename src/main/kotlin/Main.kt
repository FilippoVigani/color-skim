import colors.ColorSkim
import colors.PaletteColor
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import kotlinx.cli.*
import logging.LogConsoleHandler
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.util.logging.*
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.min
import kotlin.reflect.KClass
import dev.kdrag0n.colorkt.rgb.LinearSrgb as ColorKtLinearSrgb
import dev.kdrag0n.colorkt.rgb.Srgb as ColorKtSrgb
import dev.kdrag0n.colorkt.tristimulus.CieXyz as ColorKtCieXyz
import dev.kdrag0n.colorkt.tristimulus.CieXyzAbs as ColorKtCieXyzAbs
import dev.kdrag0n.colorkt.ucs.lab.CieLab as ColorKtCieLab
import dev.kdrag0n.colorkt.ucs.lab.Oklab as ColorKtOklab
import dev.kdrag0n.colorkt.ucs.lch.CieLch as ColorKtCieLch
import dev.kdrag0n.colorkt.ucs.lch.Oklch as ColorKtOklch
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
        description = "Creates a visual representation of the output in the specified output folder"
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
    ).default(ArgAlgorithm.MacQueen)

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
        description = "Upper limit on the number of pixels for the computation. If the number of pixels times the resolution is lower than this number, this has no effect. Highly recommended for large images."
    ).default(Int.MAX_VALUE)

    val debug by parser.option(
        ArgType.Boolean,
        shortName = "d",
        fullName = "debug",
        description = "Print debug information such as execution time"
    ).default(false)

    val verbose by parser.option(
        ArgType.Boolean,
        shortName = "v",
        fullName = "verbose",
        description = "Print verbose debug information such as information about every iteration"
    ).default(false)

    parser.parse(args)
    val logger = setupLogger(debug, verbose)

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
        logger.finer("$argFile is a directory, will use every file in it")
        argFile.listFiles(FileFilter { it.isFile && !it.isHidden })!!.sortedBy { it.name }
    } else listOf(argFile)
    files.forEach { file ->
        logger.fine("Analysing ${file.path}")
        val palette = colorSkim.computeSchemeFromImage(
            inputStream = FileInputStream(file),
            paletteSize = paletteSize,
            resolution = resolution.toFloat(),
            maxResolution = limit,
            colorSelection = selection,
        )
        if (output != null) {
            exportSVG(file, palette, File("./$output/${file.nameWithoutExtension}.svg"))
        }
    }

}

private fun setupLogger(debug: Boolean, verbose: Boolean): Logger {
    LogManager.getLogManager()
        .readConfiguration(ColorSkim::class.java.classLoader.getResourceAsStream("logging.properties"))

    return Logger.getLogger(ColorSkim::class.qualifiedName).apply {
        useParentHandlers = false
        val handler = LogConsoleHandler()
        addHandler(handler)
        if (debug) {
            level = Level.FINE
            handler.level = Level.FINE
        }
        if (verbose) {
            level = Level.ALL
            handler.level = Level.ALL
        }
    }
}

private enum class ArgAlgorithm {
    Lloyd, MacQueen, HartiganWong
}

@Suppress("unused")
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

//TODO: Refactor svg exporter
private fun exportSVG(inputFile: File, palette: List<PaletteColor>, outputFile: File) {
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    val builder = documentBuilderFactory.newDocumentBuilder()
    val document = builder.newDocument()
    val image = ImageIO.read(inputFile)
    val imageHeight = image.height
    val imageWidth = image.width
    val colorHeight = imageHeight * 0.1
    val padding = min(imageHeight, imageWidth) * 0.05
    val colorWidth = imageWidth.toFloat() / palette.size
    document.appendChild(
        document.createElement("svg").apply {
            setAttribute("width", "100%")
            setAttribute("height", "100%")
            setAttribute("viewBox", "0 0 ${imageWidth + padding * 2} ${imageHeight + colorHeight + padding * 3}")
            setAttribute("xmlns", "http://www.w3.org/2000/svg")
            appendChild(
                document.createElement("image").apply {
                    setAttribute("href", inputFile.absolutePath)
                    setAttribute("x", "$padding")
                    setAttribute("y", "$padding")
                    setAttribute("width", "$imageWidth")
                    setAttribute("height", "$imageHeight")
                }
            )
            palette.forEachIndexed { index, paletteColor ->
                appendChild(
                    document.createElement("rect").apply {
                        setAttribute("x", "${(index * colorWidth) + padding}")
                        setAttribute("y", "${imageHeight + padding * 2}")
                        setAttribute("width", "$colorWidth")
                        setAttribute("height", "$colorHeight")
                        setAttribute("fill", paletteColor.color.convert<ColorKtSrgb>().toHex())
                    }
                )
            }
        }
    )
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    val domSource = DOMSource(document)
    outputFile.parentFile.mkdirs()
    val streamResult = StreamResult(outputFile)
    transformer.transform(domSource, streamResult)
}

private fun Color.toAwtColor(): AwtColor {
    val rgb = this.convert<ColorKtSrgb>()
    return AwtColor(
        rgb.r.toFloat().coerceIn(0f, 1f),
        rgb.g.toFloat().coerceIn(0f, 1f),
        rgb.b.toFloat().coerceIn(0f, 1f)
    )
}
