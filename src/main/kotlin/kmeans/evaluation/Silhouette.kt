package kmeans.evaluation

import kmeans.KMeansResult
import kmeans.PointDistance
import kmeans.euclideanDistanceSquared
import java.lang.Double.max

/**
 * [Silhouette points discriminant method](https://www.sciencedirect.com/science/article/pii/0377042787901257?via%3Dihub) implementation,
 * used to determine the optimal number of clusters.
 *
 */
internal fun silhouette(kMeansResult: KMeansResult): Double {
    val k = kMeansResult.centroids.size
    val points = kMeansResult.points
    var totalSi = 0.0
    for (i in kMeansResult.points.indices) {
        val distances = Array(k) { 0.0 }
        val clusterIndex = kMeansResult.clustersIndexes[i]
        val clusterSize = kMeansResult.clusters[kMeansResult.clustersIndexes[i]].points.size
        for (j in kMeansResult.points.indices) {
            distances[kMeansResult.clustersIndexes[j]] += euclideanDistanceSquared(points[i], points[j])
        }
        val intraDistance = distances[clusterIndex]
        var minInterDistance = IndexedValue(-1, PointDistance.POSITIVE_INFINITY)

        for (c in 0 until k) {
            if (c != clusterIndex && distances[c] < minInterDistance.value) {
                minInterDistance = IndexedValue(c, distances[c])
            }
        }
        val si = if (clusterSize == 1) {
            0.0
        } else {
            val ai = 1.0 / (clusterSize - 1) * intraDistance
            val interClusterSize = kMeansResult.clusters[minInterDistance.index].points.size
            val bi = 1.0 / interClusterSize * minInterDistance.value
            (bi - ai) / max(bi, ai)
        }

        totalSi += si
    }
    return totalSi / kMeansResult.points.size
}