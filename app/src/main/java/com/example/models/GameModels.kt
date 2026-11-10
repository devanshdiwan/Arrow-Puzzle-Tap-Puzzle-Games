package com.example.models

enum class Direction(val dx: Int, val dy: Int, val angleDegrees: Float) {
    UP(0, -1, 270f),
    RIGHT(1, 0, 0f),
    DOWN(0, 1, 90f),
    LEFT(-1, 0, 180f);

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

data class GridPoint(val x: Int, val y: Int) {
    fun offset(dir: Direction, step: Int = 1): GridPoint =
        GridPoint(x + dir.dx * step, y + dir.dy * step)
}

enum class Difficulty(val label: String) {
    TUTORIAL("Tutorial"),
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

data class Arrow(
    val id: String,
    val points: List<GridPoint>, // sequence from tail to head
    val direction: Direction,    // escape trajectory direction from head
    val isEscaping: Boolean = false,
    val escapeProgress: Float = 0f,
    val isShakeError: Boolean = false,
    val isHintHighlighted: Boolean = false
) {
    val headPoint: GridPoint get() = points.last()
    val tailPoint: GridPoint get() = points.first()

    /**
     * Interpolates all integer grid points covered by this arrow's segments.
     */
    fun allOccupiedPoints(): Set<GridPoint> {
        val occupied = mutableSetOf<GridPoint>()
        if (points.isEmpty()) return occupied
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val minX = minOf(p1.x, p2.x)
            val maxX = maxOf(p1.x, p2.x)
            val minY = minOf(p1.y, p2.y)
            val maxY = maxOf(p1.y, p2.y)
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    occupied.add(GridPoint(x, y))
                }
            }
        }
        occupied.add(points.last())
        return occupied
    }
}

data class LevelDefinition(
    val levelNumber: Int,
    val cols: Int,
    val rows: Int,
    val arrows: List<Arrow>,
    val difficulty: Difficulty,
    val silhouetteName: String = "RECTANGLE",
    val parMoves: Int = arrows.size
)

data class UndoSnapshot(
    val removedArrow: Arrow,
    val lives: Int,
    val movesCount: Int
)

data class GameCompletionResult(
    val levelNumber: Int,
    val stars: Int,
    val movesCount: Int,
    val hintsUsed: Int,
    val timeSeconds: Long
)
