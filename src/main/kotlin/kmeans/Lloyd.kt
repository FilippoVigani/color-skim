package kmeans

import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
fun lloyd(
    k: Int,
    points: Array<Point>,
    selectCentroids: (k: Int, points: Array<Point>) -> Array<Point> = ::randomCentroids
): Collection<Collection<Point>> {
    println("Running Lloyd on ${points.size} points with $k clusters")
    var iterations = 0
    val timedValue = measureTimedValue {
        val clustersIndexes = Array(points.size) { -1 }
        val clustersSizes = Array(k) { 0 }
        val centroids = selectCentroids(k, points)
        var converging: Boolean
        val nextCentroids = Array(k) { FloatArray(centroids.first().size) }
        do {
            iterations++
            converging = false
            for (pointIndex in points.indices) {
                var minDistance: IndexedValue<Float>? = null
                for (clusterIndex in 0 until k) {
                    val distance = euclideanDistanceSquared(points[pointIndex], centroids[clusterIndex])
                    if (minDistance == null || distance < minDistance.value) {
                        minDistance = IndexedValue(clusterIndex, distance)
                    }
                }
                if (minDistance != null) {
                    if (minDistance.index != clustersIndexes[pointIndex]) {
                        if (clustersIndexes[pointIndex] != -1) {
                            clustersSizes[clustersIndexes[pointIndex]]--
                        }
                        clustersIndexes[pointIndex] = minDistance.index
                        clustersSizes[minDistance.index]++
                        converging = true
                    }
                }

                for (d in nextCentroids[clustersIndexes[pointIndex]].indices) {
                    nextCentroids[clustersIndexes[pointIndex]][d] += points[pointIndex][d]
                }
            }
            for (i in nextCentroids.indices) {
                for (d in nextCentroids[i].indices) {
                    centroids[i][d] = nextCentroids[i][d] / clustersSizes[i]
                    nextCentroids[i][d] = 0f
                }
            }
        } while (converging)
        println("iterations=$iterations")
        val clusters = List(k) { mutableListOf<Point>() }
        for (p in points.indices) {
            clusters[clustersIndexes[p]].add(points[p])
        }
        clusters.sortedByDescending { it.size }
    }
    println("Operation took ${timedValue.duration.inWholeMilliseconds} ms ($iterations iterations)")
    return timedValue.value
}

fun randomCentroids(k: Int, points: Array<Point>): Array<Point> {
    val random = Random(0)
    //Use a fixed seed so that it's easier to compare iterations count with the same input data
    return Array(k) { points[random.nextInt(points.size)].clone() }
}