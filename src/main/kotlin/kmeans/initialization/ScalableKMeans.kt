package kmeans.initialization

import kmeans.Point
import kmeans.PointDistance
import kmeans.euclideanDistanceSquared
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.random.Random

fun scalableKMeans(k: Int, points: Array<Point>, l: Float): Array<Int> {
    val random = Random(0)
    val centersIndexes = mutableListOf(random.nextInt(points.size))
    val phi = getTotalCost(points, centersIndexes.toTypedArray())
    var currentCost = phi
    for (n in 0 until ln(phi).roundToInt()) {
        var cumulativeDistance = 0.0
        val distances = Array(points.size) { p ->
            var minDistance = PointDistance.POSITIVE_INFINITY
            for (i in centersIndexes.indices) {
                if (centersIndexes[i] != -1) {
                    val distance = euclideanDistanceSquared(points[centersIndexes[i]], points[p])
                    if (distance <= minDistance) {
                        minDistance = distance
                    }
                }
            }
            cumulativeDistance += minDistance
            minDistance
        }
        for (p in points.indices) {
            val sampleProbability = l * distances[p] / cumulativeDistance
            if (random.nextDouble() < sampleProbability) {
                centersIndexes.add(p)
            }
        }
        currentCost = cumulativeDistance
    }
    val weights = getWeights(centersIndexes, points)
    var cumulativeProbability = 0.0
    val cumulativeProbabilities = Array(weights.size) {
        cumulativeProbability += weights[it].toDouble() / points.size
        cumulativeProbability
    }

    val selectedCenters = Array(k) {
        val randomCumulativeProbability = random.nextDouble(cumulativeProbability)
        var w = -1
        do {
            w++
        } while (randomCumulativeProbability > cumulativeProbabilities[w])
        val center = centersIndexes[w]
        centersIndexes.removeAt(w)
        cumulativeProbability -= weights[w].toDouble() / points.size
        center
    }

    return selectedCenters
}

private fun getWeights(centerIndexes: List<Int>, points: Array<Point>): Array<Int> {
    val weights = Array(centerIndexes.size) { 0 }
    for (p in points.indices) {
        var minDistance = IndexedValue(-1, PointDistance.POSITIVE_INFINITY)
        for ((i, centerIndex) in centerIndexes.withIndex()) {
            val distance = euclideanDistanceSquared(points[centerIndex], points[p])
            if (distance < minDistance.value) {
                minDistance = IndexedValue(i, distance)
            }
        }
        weights[minDistance.index]++
    }
    return weights
}

private fun getTotalCost(points: Array<Point>, pointIndexes: Array<Int>): PointDistance {
    var cumulativeDistance = 0.0
    for (p in points.indices) {
        var minDistance = PointDistance.POSITIVE_INFINITY
        for (i in pointIndexes.indices) {
            if (pointIndexes[i] != -1) {
                val distance = euclideanDistanceSquared(points[pointIndexes[i]], points[p])
                if (distance <= minDistance) {
                    minDistance = distance
                }
            }
        }
        cumulativeDistance += minDistance
    }
    return cumulativeDistance
}