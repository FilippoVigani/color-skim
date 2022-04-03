package kmeans

import kmeans.initialization.kmeansPlusPlus
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
internal fun macQueen(
    k: Int,
    points: Array<Point>,
    selectIndexes: InitialPointsSelector = ::kmeansPlusPlus
): KMeansResult {
    println("Running MacQueen on ${points.size} points with $k clusters")
    var iterations = 0
    val timedValue = measureTimedValue {
        val startingCentroidsIndexes = selectIndexes(k, points)
        val centroids = Array(k) { points[startingCentroidsIndexes[it]].copyOf() }
        val clustersIndexes = Array(points.size) { startingCentroidsIndexes.indexOf(it) }
        val clustersSizes = Array(k) { 1 }
        var converging: Boolean
        do {
            iterations++
            converging = false
            for (pointIndex in points.indices) {
                var minDistance: IndexedValue<PointDistance>? = null
                val clusterIndex = clustersIndexes[pointIndex]
                for (targetClusterIndex in 0 until k) {
                    val distance = euclideanDistanceSquared(points[pointIndex], centroids[targetClusterIndex])
                    if (minDistance == null || distance < minDistance.value) {
                        minDistance = IndexedValue(targetClusterIndex, distance)
                    }
                }
                val targetClusterIndex = minDistance?.index
                if (targetClusterIndex != null && targetClusterIndex != clusterIndex) {
                    if (clusterIndex != -1) {
                        removePointFromCentroid(
                            centroid = centroids[clusterIndex],
                            point = points[pointIndex],
                            previousClusterSize = clustersSizes[clusterIndex]
                        )
                        clustersSizes[clusterIndex]--
                    }
                    addPointToCentroid(
                        centroid = centroids[targetClusterIndex],
                        point = points[pointIndex],
                        previousClusterSize = clustersSizes[targetClusterIndex]
                    )
                    clustersIndexes[pointIndex] = targetClusterIndex
                    clustersSizes[targetClusterIndex]++
                    converging = true
                }
            }
        } while (converging)
        val clusters = List(k) { mutableListOf<Point>() }
        for (p in points.indices) {
            clusters[clustersIndexes[p]].add(points[p])
        }
        (0 until k).mapNotNull {
            val centroid = centroids[it]
            Cluster(
                centroid = centroid,
                points = clusters[it]
            )
        }.sortedByDescending { it.points.size }
    }
    println("Operation took ${timedValue.duration.inWholeMilliseconds} ms ($iterations iterations)")
    return timedValue.value
}
