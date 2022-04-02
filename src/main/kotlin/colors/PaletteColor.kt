package colors

import dev.kdrag0n.colorkt.Color

data class PaletteColor(
    val color: Color,
    val prevalence: Double,
) {
    override fun toString(): String {
        val prevalencePercentage = String.format("%.2f", prevalence * 100)
        return "$color $prevalencePercentage%"
    }
}