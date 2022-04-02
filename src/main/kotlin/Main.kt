import colors.ColorSkim
import dev.kdrag0n.colorkt.conversion.ConversionGraph.convert
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.colorkt.ucs.lab.Oklab
import java.io.FileInputStream
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) {
    println("Hello World!")

    args.forEach { imagePath ->
        ColorSkim.Algorithm.values().forEach { algorithm ->
            val palette = ColorSkim.computeSchemeFromImage(
                inputStream = FileInputStream(imagePath),
                colorType = Oklab::class,
                paletteSize = 15,
                resolution = 0.01f,
                algorithm = algorithm
            )
            val paletteAwtColors = palette.map {
                val rgb = it.color.convert<Srgb>()
                java.awt.Color(rgb.r.toFloat(), rgb.g.toFloat(), rgb.b.toFloat())
            }
            println(palette)
        }
    }
}

