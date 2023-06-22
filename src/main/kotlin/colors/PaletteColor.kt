package colors

import dev.kdrag0n.colorkt.Color

/**
 * Color which is part of a palette.
 *
 * @property color the color in a specific color space
 * @property prevalence the prevalence of the color in the palette from 0 to 1
 */
data class PaletteColor(
    val color: Color,
    val prevalence: Double,
)