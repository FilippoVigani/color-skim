package kmeans.initialization

import kmeans.Point
import kotlin.random.Random

/**
 * Random points selection.
 */
internal fun randomPointsIndexes(k: Int, points: Array<Point>, random: Random = Random.Default): Array<Int> {
    return Array(k) { random.nextInt(points.size) }
}