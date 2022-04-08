package kmeans.initialization

import kmeans.Point
import kmeans.PointDistance
import kmeans.euclideanDistanceSquared
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.random.Random

fun scalableKMeans(k: Int, points: Array<Point>, l: Float, random: Random = Random.Default): Array<Int> {
    val centersIndexes = mutableListOf(random.nextInt(points.size))
    val phi = getTotalCost(points, centersIndexes.toTypedArray())
    var currentCost = phi
    for (n in 0 until ln(phi).roundToInt()) {
        var cumulativeDistance = 0.0
        val distances = Array(points.size) { p ->
            var minDistance = PointDistance.POSITIVE_INFINITY
            for (i in centersIndexes.indices) {
                val distance = euclideanDistanceSquared(points[centersIndexes[i]], points[p])
                if (distance <= minDistance) {
                    minDistance = distance
                }
            }
            cumulativeDistance += minDistance
            minDistance
        }
        for (p in points.indices) {
            val sampleProbability = l * distances[p] / currentCost
            if (random.nextDouble() < sampleProbability) {
                centersIndexes.add(p)
            }
        }
        currentCost = cumulativeDistance
    }
    val weights = getWeights(centersIndexes, points).toMutableList()

    val selectedCenters = Array(k) {
        val centerIndex = selectedWeightedIndex(weights, random)
        val center = centersIndexes[centerIndex]
        centersIndexes.removeAt(centerIndex)
        weights.removeAt(centerIndex)
        center
    }

    return selectedCenters
}

private fun selectedWeightedIndex(weights: List<Int>, random: Random): Int {
    var totalCumulativeWeight = 0L
    val cumulativeWeights = Array(weights.size) {
        totalCumulativeWeight += weights[it]
        totalCumulativeWeight
    }
    val randomCumulativeWeight = random.nextLong(totalCumulativeWeight)
    var i = -1
    do {
        i++
    } while (randomCumulativeWeight > cumulativeWeights[i])
    return i
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