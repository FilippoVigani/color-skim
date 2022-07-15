import colors.ColorSkim
import colors.PaletteColor
import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.rgb.Srgb
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.util.Collections.min
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.min
import kotlin.time.ExperimentalTime
import java.awt.Color as AwtColor

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) {
    val colorSkim = ColorSkim(algorithm = ColorSkim.Algorithm.MacQueen())

    args.forEach { path ->
        val argFile = File(path)
        val files = if (argFile.isDirectory) {
            argFile.listFiles(FileFilter { it.isFile && !it.isHidden })!!.sortedBy { it.name }
        } else listOf(argFile)
        files.forEach { file ->
            val palette = colorSkim.computeSchemeFromImage(
                inputStream = FileInputStream(file),
                paletteSize = ColorSkim.PaletteSize.Fixed(6),
                maxResolution = 1000 * 1000,
                colorSelection = ColorSkim.ColorSelection.Sampled
            )
            exportSVG(file, palette, File("./outputs/${file.name}.svg"))
            println(palette)
        }

    }
}

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
                        setAttribute("fill", paletteColor.color.convert<Srgb>().toHex())
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
    val rgb = this.convert<Srgb>()
    return AwtColor(
        rgb.r.toFloat().coerceIn(0f, 1f),
        rgb.g.toFloat().coerceIn(0f, 1f),
        rgb.b.toFloat().coerceIn(0f, 1f)
    )
}
