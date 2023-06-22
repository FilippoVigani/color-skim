package kmeans

import kmeans.evaluation.elbow
import kmeans.evaluation.silhouette

internal class KMeansResult(
    internal val points: Array<Point>,
    internal val clustersIndexes: Array<Int>,
    internal val centroids: Array<out Point?>
) {
    val numberOfPoints = points.size
    val clusters = clusterizePoints(
        points = points,
        clustersIndexes = clustersIndexes,
        centroids = centroids
    )

    val silhouette: Double by lazy {
        silhouette(this)
    }

    val elbowVariance: Double by lazy {
        elbow(this)
    }
}


internal class Cluster(
    val centroid: Point,
    val points: Collection<Point>
)

internal fun clusterizePoints(
    points: Array<Point>,
    clustersIndexes: Array<Int>,
    centroids: Array<out Point?>
): List<Cluster> {
    val clusters = List(centroids.size) { mutableListOf<Point>() }
    for (p in points.indices) {
        clusters[clustersIndexes[p]].add(points[p])
    }
    return (centroids.indices).mapNotNull {
        val centroid = centroids[it]
        if (centroid != null) {
            Cluster(
                centroid = centroid,
                points = clusters[it]
            )
        } else null
    }
}