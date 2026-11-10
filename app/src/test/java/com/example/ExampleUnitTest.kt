package com.example

import com.example.gameengine.CollisionDetector
import com.example.gameengine.CollisionResult
import com.example.gameengine.LevelCatalog
import com.example.gameengine.PuzzleEngine
import com.example.gameengine.PuzzleSolver
import com.example.gameengine.TapResult
import com.example.models.Arrow
import com.example.models.Direction
import com.example.models.GridPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLevel1Solvability() {
        val level1 = LevelCatalog.getLevel(1)
        assertTrue("Level 1 must be solvable", PuzzleSolver.isSolvable(level1))
        val solution = PuzzleSolver.findSolutionSequence(level1)
        assertNotNull(solution)
        assertEquals(3, solution!!.size)
    }

    @Test
    fun testLevel23And42Solvability() {
        val level23 = LevelCatalog.getLevel(23)
        assertTrue("Level 23 must be solvable", PuzzleSolver.isSolvable(level23))

        val level42 = LevelCatalog.getLevel(42)
        assertTrue("Level 42 (Heart shape) must be solvable", PuzzleSolver.isSolvable(level42))
    }

    @Test
    fun testCollisionDetectionStraightAndBlocked() {
        // Arrow A at (0,1) pointing RIGHT towards (2,1)
        val arrowA = Arrow("a", listOf(GridPoint(0, 1), GridPoint(1, 1)), Direction.RIGHT)
        // Arrow B at (2,0) pointing DOWN through (2,1)
        val arrowB = Arrow("b", listOf(GridPoint(2, 0), GridPoint(2, 2)), Direction.DOWN)

        val collisionA = CollisionDetector.checkForwardPath(arrowA, listOf(arrowA, arrowB), 3, 3)
        assertTrue("Arrow A should be blocked by Arrow B", collisionA is CollisionResult.Blocked)

        val collisionB = CollisionDetector.checkForwardPath(arrowB, listOf(arrowA, arrowB), 3, 3)
        assertTrue("Arrow B should have clear path exiting board", collisionB is CollisionResult.Clear)
    }

    @Test
    fun testPuzzleEngineUndoAndLives() {
        val level = LevelCatalog.getLevel(1)
        val engine = PuzzleEngine(level)
        assertEquals(3, engine.lives)

        // Find safe arrow and tap it
        val safe = PuzzleSolver.findSafeArrow(engine.activeArrows, level.cols, level.rows)
        assertNotNull(safe)

        val tapSuccess = engine.tapArrow(safe!!.id)
        assertTrue(tapSuccess is TapResult.Success)
        assertEquals(3, engine.lives)
        assertTrue(engine.canUndo)

        // Undo move
        val undoSuccess = engine.undo()
        assertTrue(undoSuccess)
        assertEquals(3, engine.activeArrows.size)
    }
}
