package kmeans

import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


@OptIn(ExperimentalTime::class)
internal fun hartiganWong(
    k: Int,
    points: Array<Point>,
    selectClustersIndexes: (k: Int, points: Array<Point>) -> Array<Int> = ::randomClusters
): KMeansResult {
    println("Running Hartigan-Wong on ${points.size} points with $k clusters")
    var iterations = 0
    val timedValue = measureTimedValue {
        val clustersIndexes = selectClustersIndexes(k, points)
        val (clustersSizes, centroids) = computeClustersSizesAndCentroids(k, points, clustersIndexes)
        var lastConvergedPointIndex = 0
        var pointIndex = 0
        do {
            if (pointIndex == 0) {
                iterations++
            }
            val clusterIndex = clustersIndexes[pointIndex]
            val currentClusterSize = clustersSizes[clusterIndex]
            val point = points[pointIndex]
            var maxImprovement: IndexedValue<PointDistance>? = null
            for (targetClusterIndex in 0 until k) {
                if (targetClusterIndex != clusterIndex) {
                    val sourceCentroid = centroids[clusterIndex]
                    val targetCentroid = centroids[targetClusterIndex]
                    if (sourceCentroid != null && targetCentroid != null) {
                        val improvement = costImprovement(
                            point,
                            sourceCentroid,
                            currentClusterSize,
                            targetCentroid,
                            clustersSizes[targetClusterIndex]
                        )
                        if (improvement > (maxImprovement?.value ?: 0.0)) {
                            maxImprovement = IndexedValue(targetClusterIndex, improvement)
                        }
                    }
                }
            }
            val targetClusterIndex = maxImprovement?.index
            if (targetClusterIndex != null) {
                centroids[clusterIndex]?.let {
                    if (currentClusterSize > 1) {
                        removePointFromCentroid(it, point, currentClusterSize)
                    } else {
                        centroids[clusterIndex] = null
                    }
                }
                centroids[targetClusterIndex]?.let {
                    addPointToCentroid(it, point, clustersSizes[targetClusterIndex])
                }
                clustersIndexes[pointIndex] = targetClusterIndex
                clustersSizes[clusterIndex]--
                clustersSizes[targetClusterIndex]++

                lastConvergedPointIndex = pointIndex
            }
            pointIndex = (pointIndex + 1) % points.size
        } while (pointIndex != lastConvergedPointIndex)
        val clusters = List(k) { mutableListOf<Point>() }
        for (p in points.indices) {
            clusters[clustersIndexes[p]].add(points[p])
        }
        (0 until k).mapNotNull {
            val centroid = centroids[it]
            if (centroid != null) {
                Cluster(
                    centroid = centroid,
                    points = clusters[it]
                )
            } else null
        }.sortedByDescending { it.points.size }
    }
    println("Operation took ${timedValue.duration.inWholeMilliseconds} ms ($iterations iterations)")
    return timedValue.value
}

fun computeClustersSizesAndCentroids(
    k: Int,
    points: Array<Point>,
    clusterIndexes: Array<Int>
): Pair<Array<Int>, Array<Point?>> {
    val centroids = Array<Point?>(k) { Point(points.first().size) }
    val clusterSizes = Array(k) { 0 }
    for (i in points.indices) {
        val point = points[i]
        val clusterIndex = clusterIndexes[i]
        clusterSizes[clusterIndex]++
        for (d in centroids[clusterIndex]!!.indices) {
            centroids[clusterIndex]!![d] += point[d]
        }
    }
    for (j in centroids.indices) {
        for (d in centroids[j]!!.indices) {
            val size = clusterSizes[j]
            if (size > 0) {
                centroids[j]?.set(d, centroids[j]!![d] / size)
            } else {
                centroids[j] = null
            }
        }
    }
    return clusterSizes to centroids
}


fun randomClusters(k: Int, points: Array<Point>): Array<Int> {
    //Use a fixed seed so that it's easier to compare iterations count with the same input data
    val random = Random(0)
    return Array(points.size) { random.nextInt(k) }
}

fun costImprovement(
    point: Point,
    sourceCentroid: Point,
    sourceSize: Int,
    destinationCentroid: Point,
    destinationSize: Int
): PointDistance {
    return sourceSize * euclideanDistanceSquared(sourceCentroid, point) / (sourceSize + 1) -
            destinationSize * euclideanDistanceSquared(destinationCentroid, point) / (destinationSize + 1)
}