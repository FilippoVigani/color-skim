package colors

import java.awt.Color

data class PaletteColor(
    val color: Color,
    val prevalence: Float,
) {
    override fun toString(): String {
        val hex = "#${color.red.toString(16)}${color.green.toString(16)}${color.blue.toString(16)}"
        val prevalencePercentage = String.format("%.2f", prevalence * 100)
        return "$hex $prevalencePercentage%"
    }
}