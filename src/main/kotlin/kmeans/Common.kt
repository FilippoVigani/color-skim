package kmeans

import kotlin.math.pow

typealias Point = DoubleArray
typealias PointDistance = Double

internal fun euclideanDistanceSquared(x: Point, y: Point): PointDistance {
    var total = 0.0
    for (i in x.indices) {
        total += (x[i] - y[i]).pow(2)
    }
    return total
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