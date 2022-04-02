package colors.colorspace

import dev.kdrag0n.colorkt.Color
import dev.kdrag0n.colorkt.rgb.LinearSrgb
import dev.kdrag0n.colorkt.rgb.Rgb
import dev.kdrag0n.colorkt.rgb.Srgb
import dev.kdrag0n.colorkt.tristimulus.CieXyz
import dev.kdrag0n.colorkt.tristimulus.CieXyzAbs
import dev.kdrag0n.colorkt.ucs.lab.CieLab
import dev.kdrag0n.colorkt.ucs.lab.Lab
import dev.kdrag0n.colorkt.ucs.lab.Oklab
import dev.kdrag0n.colorkt.ucs.lch.CieLch
import dev.kdrag0n.colorkt.ucs.lch.Lch
import dev.kdrag0n.colorkt.ucs.lch.Oklch
import kotlin.reflect.KClass

fun Color.getComponents(): DoubleArray {
    return when (this) {
        is Lab -> doubleArrayOf(L, a, b)
        is Rgb -> doubleArrayOf(r, g, b)
        is CieXyz -> doubleArrayOf(x, y, z)
        is CieXyzAbs -> doubleArrayOf(x, y, z)
        is Lch -> doubleArrayOf(lightness, chroma, hue)
        else -> throw IllegalArgumentException("Unrecognized color type")
    }
}

fun DoubleArray.toColor(type: KClass<out Color>): Color {
    return when (type) {
        Oklab::class -> Oklab(this[0], this[1], this[2])
        CieLab::class -> CieLab(this[0], this[1], this[2])
        Srgb::class -> Srgb(this[0], this[1], this[2])
        LinearSrgb::class -> LinearSrgb(this[0], this[1], this[2])
        CieXyz::class -> CieXyz(this[0], this[1], this[2])
        CieXyzAbs::class -> CieXyzAbs(this[0], this[1], this[2])
        Oklch::class -> Oklch(this[0], this[1], this[2])
        CieLch::class -> CieLch(this[0], this[1], this[2])
        else -> throw IllegalArgumentException("Unrecognized color type")
    }
}