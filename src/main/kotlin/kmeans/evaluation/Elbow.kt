package kmeans.evaluation

import kmeans.KMeansResult
import kmeans.euclideanDistanceSquared
import kotlin.math.acos
import kotlin.math.sqrt

/**
 *
 * [Elbow point discriminant method]((https://jwcn-eurasipjournals.springeropen.com/articles/10.1186/s13638-021-01910-w)) implementation,
 * used to determine the optimal number of clusters.
 *
 */
internal fun elbow(kMeansResult: KMeansResult): Double {
    var wcss = 0.0
    for (i in kMeansResult.points.indices) {
        val distance =
            euclideanDistanceSquared(kMeansResult.points[i], kMeansResult.centroids[kMeansResult.clustersIndexes[i]]!!)
        wcss += distance
    }
    val meanDistortion = wcss / kMeansResult.points.size
    return meanDistortion
}

internal fun estimateBestK(kmin: Int, kmax: Int, normalizedMeanDistortions: Array<Double>, empiricalValue: Double = 10.0): Int {
    var minAngle = Math.PI
    var kopt = 0
    val pl = Array(normalizedMeanDistortions.size) {
        doubleArrayOf(normalizedMeanDistortions[it] * empiricalValue, (it + kmin).toDouble())
    }
    for (i in (kmin - 1)..(kmax - kmin - 2)) {
        val j = i + 1
        val k = i + 2
        val a = euclideanDistanceSquared(pl[i], pl[j])
        val b = euclideanDistanceSquared(pl[j], pl[k])
        val c = euclideanDistanceSquared(pl[k], pl[i])
        val angle = acos((a + b - c) / (2 * sqrt(a) * sqrt(b)))
        if (angle < minAngle) {
            minAngle = angle
            kopt = j
        }
    }
    return kopt
}