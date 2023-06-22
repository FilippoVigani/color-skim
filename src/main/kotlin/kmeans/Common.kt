package kmeans

import kotlin.math.pow

typealias Point = DoubleArray
typealias PointDistance = Double

/**
 * Computes the squared euclidean distance between two points.
 *
 * @param x the first point
 * @param y the second point
 * @return the squared euclidean distance
 */
internal fun euclideanDistanceSquared(x: Point, y: Point): PointDistance {
    var total = 0.0
    for (i in x.indices) {
        total += (x[i] - y[i]).pow(2)
    }
    return total
}

/**
 * Efficient way to compute the new centroid value of a centroid when adding a point to it
 *
 * @param centroid the initial centroid value
 * @param point the point to add
 * @param previousClusterSize the number of points of the centroid before adding the new one
 */
internal fun addPointToCentroid(centroid: Point, point: Point, previousClusterSize: Int) {
    for (d in centroid.indices) {
        centroid[d] = (centroid[d] * previousClusterSize + point[d]) / (previousClusterSize + 1)
    }
}

/**
 * Efficient way to compute the new centroid value of a centroid when removing a point from it
 *
 * @param centroid the initial centroid value
 * @param point the point to remove
 * @param previousClusterSize the number of points of the centroid before removing the new one
 */
internal fun removePointFromCentroid(centroid: Point, point: Point, previousClusterSize: Int) {
    if (previousClusterSize == 1) {
        throw IllegalArgumentException("Can't calculate centroid of empty set")
    }
    for (d in centroid.indices) {
        centroid[d] = (centroid[d] * previousClusterSize - point[d]) / (previousClusterSize - 1)
    }
}

typealias InitialPointsSelector = (k: Int, points: Array<Point>) -> Array<Int>