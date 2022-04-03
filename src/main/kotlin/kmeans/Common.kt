package kmeans

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

typealias Point = DoubleArray
typealias PointDistance = Double

internal fun euclideanDistanceSquared(x: Point, y: Point): PointDistance {
    var total = 0.0
    for (i in x.indices) {
        total += (x[i] - y[i]).pow(2)
    }
    return sqrt(total)
}

internal fun centroid(points: Collection<Point>): Point {
    if (points.isEmpty()) {
        throw IllegalArgumentException("Can't calculate centroid of empty set")
    }
    val centroid = points.first().copyOf()
    points.drop(1).forEach { point ->
        for (d in centroid.indices) {
            centroid[d] += point[d]
        }
    }
    for (d in centroid.indices) {
        centroid[d] = centroid[d] / points.size
    }
    return centroid
}


internal fun addPointToCentroid(centroid: Point, point: Point, previousClusterSize: Int) {
    for (d in centroid.indices) {
        centroid[d] = (centroid[d] * previousClusterSize + point[d]) / (previousClusterSize + 1)
    }
}

internal fun removePointFromCentroid(centroid: Point, point: Point, previousClusterSize: Int) {
    if (previousClusterSize == 1) {
        throw IllegalArgumentException("Can't calculate centroid of empty set")
    }
    for (d in centroid.indices) {
        centroid[d] = (centroid[d] * previousClusterSize - point[d]) / (previousClusterSize - 1)
    }
}

typealias InitialPointsSelector = (k: Int, points: Array<Point>) -> Array<Int>

internal fun randomPointsIndexes(k: Int, points: Array<Point>): Array<Int> {
    val random = Random(0)
    //Use a fixed seed so that it's easier to compare iterations count with the same input data
    return Array(k) { random.nextInt(points.size) }
}