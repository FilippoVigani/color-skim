package kmeans

typealias KMeansResult = Collection<Cluster>

class Cluster(
    val centroid: Point,
    val points: Collection<Point>
)