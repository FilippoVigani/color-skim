package kmeans

import kmeans.initialization.kmeansPlusPlus
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
internal fun lloyd(
    k: Int,
    points: Array<Point>,
    selectIndexes: InitialPointsSelector
): KMeansResult {
    println("Running Lloyd on ${points.size} points with $k clusters")
    var iterations = 0
    val timedValue = measureTimedValue {
        val startingCentroidsIndexes = selectIndexes(k, points)
        val centroids = Array(k) { points[startingCentroidsIndexes[it]].copyOf() }
        val clustersIndexes = Array(points.size) { -1 }
        val clustersSizes = Array(k) { 0 }
        var converging: Boolean
        val nextCentroids = Array(k) { Point(centroids.first().size) }
        do {
            iterations++
            converging = false
            for (pointIndex in points.indices) {
                val clusterIndex = clustersIndexes[pointIndex]
                var minDistance: IndexedValue<PointDistance>? = null
                for (targetClusterIndex in 0 until k) {
                    val distance = euclideanDistanceSquared(points[pointIndex], centroids[targetClusterIndex])
                    if (minDistance == null || distance < minDistance.value) {
                        minDistance = IndexedValue(targetClusterIndex, distance)
                    }
                }
                val targetClusterIndex = minDistance?.index
                if (targetClusterIndex != null) {
                    if (targetClusterIndex != clusterIndex) {
                        if (clusterIndex != -1) {
                            clustersSizes[clusterIndex]--
                        }
                        clustersIndexes[pointIndex] = targetClusterIndex
                        clustersSizes[targetClusterIndex]++
                        converging = true
                    }
                    for (d in nextCentroids[targetClusterIndex].indices) {
                        nextCentroids[targetClusterIndex][d] += points[pointIndex][d]
                    }
                }
            }
            for (i in nextCentroids.indices) {
                for (d in nextCentroids[i].indices) {
                    centroids[i][d] = nextCentroids[i][d] / clustersSizes[i]
                    nextCentroids[i][d] = 0.0
                }
            }
        } while (converging)
        KMeansResult(
            points = points,
            clustersIndexes = clustersIndexes,
            centroids = centroids
        )
    }
    println("Operation took ${timedValue.duration.inWholeMilliseconds} ms ($iterations iterations)")
    return timedValue.value
}