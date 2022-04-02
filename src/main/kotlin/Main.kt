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
        val palette = ColorSkim.computeSchemeFromImage(
            inputStream = FileInputStream(imagePath),
            colorType = Oklab::class,
            paletteSize = 8,
            resolution = 0.2f,
            algorithm = ColorSkim.Algorithm.HartiganWong
        )
        val paletteAwtColors = palette.map {
            val rgb = it.color.convert<Srgb>()
            java.awt.Color(rgb.r.toFloat(), rgb.g.toFloat(), rgb.b.toFloat())
        }
        println(palette)
    }
}

