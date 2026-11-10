package com.example.gameengine

import com.example.models.Arrow
import com.example.models.LevelDefinition

object PuzzleSolver {

    /**
     * Finds the first arrow that can currently escape safely.
     * Used for the Hint feature.
     */
    fun findSafeArrow(activeArrows: List<Arrow>, cols: Int, rows: Int): Arrow? {
        return activeArrows.firstOrNull { arrow ->
            CollisionDetector.isArrowSafe(arrow, activeArrows, cols, rows)
        }
    }

    /**
     * Solves the level via topological search to guarantee it is 100% solvable.
     * Returns the list of arrow IDs in order of escape, or null if deadlocked.
     */
    fun findSolutionSequence(level: LevelDefinition): List<String>? {
        val remaining = level.arrows.toMutableList()
        val solution = mutableListOf<String>()

        while (remaining.isNotEmpty()) {
            val safe = remaining.firstOrNull { arrow ->
                CollisionDetector.isArrowSafe(arrow, remaining, level.cols, level.rows)
            } ?: return null // Deadlocked: no arrow can escape

            solution.add(safe.id)
            remaining.remove(safe)
        }

        return solution
    }

    /**
     * Checks if a level has at least one valid complete escape sequence.
     */
    fun isSolvable(level: LevelDefinition): Boolean {
        return findSolutionSequence(level) != null
    }
}
