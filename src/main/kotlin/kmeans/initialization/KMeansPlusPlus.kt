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
        val distances = Array(points.size) { p ->
            var minDistance: IndexedValue<PointDistance>? = null
            for(i in centersIndexes.indices){
                if (centersIndexes[i] != -1){
                    val distance = euclideanDistanceSquared(points[centersIndexes[i]], points[p])
                    if (distance <= (minDistance?.value ?: PointDistance.MAX_VALUE)){
                        minDistance = IndexedValue(i, distance)
                    }
                }
            }
            cumulativeDistance += minDistance!!.value
            minDistance.value
        }
        var randomDistanceTotal = random.nextDouble(cumulativeDistance)
        var p = -1
        while (randomDistanceTotal >= 0){
            p++
            val currentDistance = distances[p]
            randomDistanceTotal -= currentDistance
        }
        centersIndexes[centersCount++] = p
    }
    return centersIndexes
}