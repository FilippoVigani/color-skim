package kmeans.initialization

import kmeans.Point
import kmeans.PointDistance
import kmeans.euclideanDistanceSquared
import kotlin.random.Random

/**
 * [K-means++](https://theory.stanford.edu/~sergei/papers/kMeansPP-soda.pdf) implementation.
 *
 */
internal fun kmeansPlusPlus(k: Int, points: Array<Point>, random: Random = Random.Default): Array<Int> {
    val centersIndexes = Array(k) { if (it == 0) random.nextInt(points.size) else -1 }
    for (c in 1 until k) {
        var cumulativeDistance = 0.0
        val cumulativeDistances = Array(points.size) { p ->
            var minDistance = PointDistance.POSITIVE_INFINITY
            for (i in 0 until c) {
                val distance = euclideanDistanceSquared(points[centersIndexes[i]], points[p])
                if (distance <= minDistance) {
                    minDistance = distance
                }
            }
            cumulativeDistance += minDistance
            cumulativeDistance
        }
        val randomCumulativeDistance = random.nextDouble(cumulativeDistance)
        var p = -1
        do {
            p++
        } while (randomCumulativeDistance > cumulativeDistances[p])
        centersIndexes[c] = p
    }
    return centersIndexes
}