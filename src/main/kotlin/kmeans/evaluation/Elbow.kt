package kmeans.evaluation

import kmeans.KMeansResult
import kmeans.euclideanDistanceSquared

fun elbow(kMeansResult: KMeansResult): Double{
    var wcss = 0.0
    for (i in kMeansResult.points.indices){
        val distance = euclideanDistanceSquared(kMeansResult.points[i], kMeansResult.centroids[kMeansResult.clustersIndexes[i]]!!)
        wcss += distance
    }
    return wcss / kMeansResult.points.size
}