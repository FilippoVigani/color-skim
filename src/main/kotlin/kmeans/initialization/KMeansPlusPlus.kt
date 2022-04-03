package kmeans.initialization

import kmeans.Point
import kmeans.PointDistance
import kmeans.euclideanDistanceSquared
import kotlin.random.Random

fun kmeansPlusPlus(k: Int, points: Array<Point>): Array<Int> {
    val random = Random(0)
    val centersIndexes = Array(k) { if (it == 0) random.nextInt(points.size) else -1 }
    var centersCount = 1
    while (centersCount < k){
        var cumulativeDistance = 0.0
        val cumulativeDistances = Array(points.size) { p ->
            var minDistance = PointDistance.POSITIVE_INFINITY
            for(i in centersIndexes.indices){
                if (centersIndexes[i] != -1){
                    val distance = euclideanDistanceSquared(points[centersIndexes[i]], points[p])
                    if (distance <= minDistance){
                        minDistance = distance
                    }
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
        centersIndexes[centersCount++] = p
    }
    return centersIndexes
}