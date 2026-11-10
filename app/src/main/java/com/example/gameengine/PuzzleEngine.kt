package com.example.gameengine

import com.example.models.Arrow
import com.example.models.GridPoint
import com.example.models.LevelDefinition
import com.example.models.UndoSnapshot

sealed class TapResult {
    data class Success(val arrow: Arrow, val isLevelComplete: Boolean) : TapResult()
    data class Blocked(
        val tappedArrow: Arrow,
        val blockingArrow: Arrow,
        val collisionPoint: GridPoint,
        val pathPoints: List<GridPoint>,
        val remainingLives: Int,
        val isGameOver: Boolean
    ) : TapResult()
    object Ignored : TapResult()
}

class PuzzleEngine(val level: LevelDefinition) {

    private val _arrows = mutableListOf<Arrow>()
    val activeArrows: List<Arrow> get() = _arrows.toList()

    var lives: Int = 3
        private set

    val maxLives: Int = 3

    var movesCount: Int = 0
        private set

    var hintsUsed: Int = 0
        private set

    private val undoStack = ArrayDeque<UndoSnapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty() && !isGameOver && !isLevelCompleted

    val isLevelCompleted: Boolean get() = _arrows.isEmpty()

    val isGameOver: Boolean get() = lives <= 0

    init {
        reset()
    }

    fun reset() {
        _arrows.clear()
        _arrows.addAll(level.arrows)
        lives = maxLives
        movesCount = 0
        hintsUsed = 0
        undoStack.clear()
    }

    fun tapArrow(arrowId: String): TapResult {
        if (isGameOver || isLevelCompleted) return TapResult.Ignored

        val arrow = _arrows.find { it.id == arrowId } ?: return TapResult.Ignored
        if (arrow.isEscaping) return TapResult.Ignored

        val collision = CollisionDetector.checkForwardPath(arrow, _arrows, level.cols, level.rows)

        return when (collision) {
            is CollisionResult.Clear -> {
                // Safe escape
                undoStack.addLast(
                    UndoSnapshot(
                        removedArrow = arrow,
                        lives = lives,
                        movesCount = movesCount
                    )
                )
                _arrows.remove(arrow)
                movesCount++
                TapResult.Success(arrow, isLevelComplete = _arrows.isEmpty())
            }
            is CollisionResult.Blocked -> {
                lives = maxOf(0, lives - 1)
                TapResult.Blocked(
                    tappedArrow = arrow,
                    blockingArrow = collision.blockingArrow,
                    collisionPoint = collision.collisionPoint,
                    pathPoints = collision.pathPoints,
                    remainingLives = lives,
                    isGameOver = isGameOver
                )
            }
        }
    }

    fun undo(): Boolean {
        if (!canUndo) return false
        val snapshot = undoStack.removeLastOrNull() ?: return false
        _arrows.add(snapshot.removedArrow)
        lives = snapshot.lives
        movesCount = snapshot.movesCount
        return true
    }

    fun getHintArrow(): Arrow? {
        if (isGameOver || isLevelCompleted) return null
        val hint = PuzzleSolver.findSafeArrow(_arrows, level.cols, level.rows)
        if (hint != null) {
            hintsUsed++
        }
        return hint
    }

    fun addLife(amount: Int = 1) {
        lives = minOf(maxLives, lives + amount)
    }

    fun calculateStars(): Int {
        return when {
            lives == 3 && hintsUsed == 0 -> 3
            lives >= 2 -> 2
            else -> 1
        }
    }
}
