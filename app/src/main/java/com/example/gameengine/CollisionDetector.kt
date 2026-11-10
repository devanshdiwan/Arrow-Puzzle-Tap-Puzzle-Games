package com.example.gameengine

import com.example.models.Arrow
import com.example.models.GridPoint

sealed class CollisionResult {
    data class Clear(val pathPoints: List<GridPoint>) : CollisionResult()
    data class Blocked(
        val blockingArrow: Arrow,
        val collisionPoint: GridPoint,
        val pathPoints: List<GridPoint>
    ) : CollisionResult()
}

object CollisionDetector {

    /**
     * Checks if the given arrow has a clear forward trajectory to exit the grid.
     * Raycasts from the arrow's headPoint in arrow.direction until exiting cols/rows bounds.
     */
    fun checkForwardPath(
        arrow: Arrow,
        activeArrows: List<Arrow>,
        cols: Int,
        rows: Int
    ): CollisionResult {
        val otherArrows = activeArrows.filter { it.id != arrow.id }
        // Pre-compute map of occupied points to arrow
        val occupiedMap = mutableMapOf<GridPoint, Arrow>()
        for (other in otherArrows) {
            for (p in other.allOccupiedPoints()) {
                occupiedMap[p] = other
            }
        }

        val forwardPath = mutableListOf<GridPoint>()
        var current = arrow.headPoint.offset(arrow.direction)

        // Cast ray until outside board bounds
        while (current.x in 0 until cols && current.y in 0 until rows) {
            forwardPath.add(current)
            val blockingArrow = occupiedMap[current]
            if (blockingArrow != null) {
                return CollisionResult.Blocked(
                    blockingArrow = blockingArrow,
                    collisionPoint = current,
                    pathPoints = forwardPath
                )
            }
            current = current.offset(arrow.direction)
        }

        // Ray cleared the board boundary without hitting any other arrow
        return CollisionResult.Clear(pathPoints = forwardPath)
    }

    /**
     * Checks whether an arrow is currently safe to escape.
     */
    fun isArrowSafe(arrow: Arrow, activeArrows: List<Arrow>, cols: Int, rows: Int): Boolean {
        return checkForwardPath(arrow, activeArrows, cols, rows) is CollisionResult.Clear
    }
}
