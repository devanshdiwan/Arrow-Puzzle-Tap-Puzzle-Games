package com.example.gameengine

import com.example.models.Arrow
import com.example.models.Difficulty
import com.example.models.Direction
import com.example.models.GridPoint
import com.example.models.LevelDefinition
import java.util.Random

object LevelGenerator {

    enum class Silhouette {
        RECTANGLE,
        HEART,
        DIAMOND,
        HOURGLASS,
        STAR,
        FRAME
    }

    /**
     * Creates a boolean mask array of valid cell positions for a given silhouette.
     */
    fun createSilhouetteMask(silhouette: Silhouette, cols: Int, rows: Int): Array<BooleanArray> {
        val mask = Array(cols) { BooleanArray(rows) { true } }

        when (silhouette) {
            Silhouette.RECTANGLE -> {
                // all true
            }
            Silhouette.HEART -> {
                // Approximate heart shape on a grid
                for (x in 0 until cols) {
                    for (y in 0 until rows) {
                        val nx = (x.toFloat() / (cols - 1)) * 2f - 1f // -1 to 1
                        val ny = (y.toFloat() / (rows - 1)) * 2f - 0.8f // top -0.8 to bottom 1.2
                        // Heart equation approximation: (x^2 + (y - sqrt(|x|))^2) <= 1
                        val topCleft = (nx in -0.2f..0.2f && ny < -0.4f)
                        val bottomPoint = (ny > 0.8f && (nx < -0.4f || nx > 0.4f))
                        val corners = (ny < -0.6f && (nx < -0.7f || nx > 0.7f))
                        val bottomCorners = (ny > 0.3f && Math.abs(nx) > (1.2f - ny))
                        if (topCleft || bottomPoint || corners || bottomCorners) {
                            mask[x][y] = false
                        }
                    }
                }
            }
            Silhouette.DIAMOND -> {
                val cx = (cols - 1) / 2f
                val cy = (rows - 1) / 2f
                for (x in 0 until cols) {
                    for (y in 0 until rows) {
                        val dx = Math.abs(x - cx) / cx
                        val dy = Math.abs(y - cy) / cy
                        if (dx + dy > 1.05f) {
                            mask[x][y] = false
                        }
                    }
                }
            }
            Silhouette.HOURGLASS -> {
                for (x in 0 until cols) {
                    for (y in 0 until rows) {
                        val midY = (rows - 1) / 2f
                        val distY = Math.abs(y - midY) / midY
                        val allowedXRatio = 0.3f + distY * 0.7f
                        val cx = (cols - 1) / 2f
                        val distX = Math.abs(x - cx) / cx
                        if (distX > allowedXRatio) {
                            mask[x][y] = false
                        }
                    }
                }
            }
            Silhouette.STAR -> {
                val cx = (cols - 1) / 2f
                val cy = (rows - 1) / 2f
                for (x in 0 until cols) {
                    for (y in 0 until rows) {
                        val dist = Math.hypot((x - cx).toDouble(), (y - cy).toDouble())
                        val angle = Math.atan2((y - cy).toDouble(), (x - cx).toDouble())
                        val r = (cols / 2f) * (0.6f + 0.4f * Math.cos(4.0 * angle))
                        if (dist > r) {
                            mask[x][y] = false
                        }
                    }
                }
            }
            Silhouette.FRAME -> {
                val padX = cols / 4
                val padY = rows / 4
                for (x in padX until cols - padX) {
                    for (y in padY until rows - padY) {
                        mask[x][y] = false
                    }
                }
            }
        }
        return mask
    }

    /**
     * Generates a guaranteed solvable puzzle using Reverse Construction.
     */
    fun generateSolvableLevel(
        levelNumber: Int,
        cols: Int,
        rows: Int,
        targetArrowCount: Int,
        allowBends: Boolean,
        silhouette: Silhouette,
        seed: Long = levelNumber.toLong() * 9973L + 42L
    ): LevelDefinition {
        val random = Random(seed)
        val mask = createSilhouetteMask(silhouette, cols, rows)
        val occupied = mutableSetOf<GridPoint>()
        val reverseArrows = mutableListOf<Arrow>()

        var attempts = 0
        val maxAttempts = 1200

        while (reverseArrows.size < targetArrowCount && attempts < maxAttempts) {
            attempts++
            // Pick head point that is within mask and not occupied
            val availablePoints = mutableListOf<GridPoint>()
            for (x in 0 until cols) {
                for (y in 0 until rows) {
                    val p = GridPoint(x, y)
                    if (mask[x][y] && !occupied.contains(p)) {
                        availablePoints.add(p)
                    }
                }
            }
            if (availablePoints.isEmpty()) break

            val head = availablePoints[random.nextInt(availablePoints.size)]
            val allDirections = Direction.values().toList().shuffled(random)

            var placedArrow: Arrow? = null

            for (dir in allDirections) {
                // In reverse construction: this arrow will escape in `dir`.
                // Its forward path must NOT intersect any currently occupied cell in `occupied`!
                var current = head.offset(dir)
                var clearPath = true
                while (current.x in 0 until cols && current.y in 0 until rows) {
                    if (occupied.contains(current)) {
                        clearPath = false
                        break
                    }
                    current = current.offset(dir)
                }
                if (!clearPath) continue

                // Now construct the body extending backward from head opposite to dir
                val backDir = dir.opposite()
                val bodyLength = if (allowBends && random.nextBoolean()) random.nextInt(2) + 2 else random.nextInt(2) + 1

                val straightPoints = mutableListOf<GridPoint>()
                straightPoints.add(head)
                var curr = head
                var canBuildStraight = true
                for (step in 1..bodyLength) {
                    val next = curr.offset(backDir)
                    if (next.x in 0 until cols && next.y in 0 until rows && mask[next.x][next.y] && !occupied.contains(next)) {
                        straightPoints.add(next)
                        curr = next
                    } else {
                        if (step == 1) canBuildStraight = false
                        break
                    }
                }

                if (!canBuildStraight || straightPoints.size < 2) continue

                // Check if we should add an elbow bend to this arrow
                val shouldBend = allowBends && straightPoints.size >= 2 && random.nextFloat() < 0.6f
                var finalPoints = straightPoints.reversed() // tail to head

                if (shouldBend) {
                    val tail = finalPoints.first()
                    val bendDirs = listOf(
                        if (dir == Direction.UP || dir == Direction.DOWN) Direction.LEFT else Direction.UP,
                        if (dir == Direction.UP || dir == Direction.DOWN) Direction.RIGHT else Direction.DOWN
                    ).shuffled(random)

                    for (bDir in bendDirs) {
                        val bendLeg = mutableListOf<GridPoint>()
                        var bCurr = tail
                        val legLength = random.nextInt(2) + 1
                        var legValid = true
                        for (s in 1..legLength) {
                            val bNext = bCurr.offset(bDir)
                            if (bNext.x in 0 until cols && bNext.y in 0 until rows && mask[bNext.x][bNext.y] && !occupied.contains(bNext)) {
                                bendLeg.add(bNext)
                                bCurr = bNext
                            } else {
                                legValid = false
                                break
                            }
                        }
                        if (legValid && bendLeg.isNotEmpty()) {
                            // Prepend bendLeg reversed to tail
                            finalPoints = bendLeg.reversed() + finalPoints
                            break
                        }
                    }
                }

                val arrow = Arrow(
                    id = "arrow_${levelNumber}_${reverseArrows.size + 1}",
                    points = finalPoints,
                    direction = dir
                )

                placedArrow = arrow
                break
            }

            if (placedArrow != null) {
                reverseArrows.add(placedArrow)
                occupied.addAll(placedArrow.allOccupiedPoints())
            }
        }

        // reverseArrows was built in reverse order of removal:
        // The first placed in reverseArrows is the last to escape in forward play.
        // All arrows in reverseArrows are placed on the board simultaneously.
        val difficulty = when {
            levelNumber <= 10 -> Difficulty.TUTORIAL
            levelNumber <= 30 -> Difficulty.EASY
            levelNumber <= 60 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }

        val level = LevelDefinition(
            levelNumber = levelNumber,
            cols = cols,
            rows = rows,
            arrows = reverseArrows,
            difficulty = difficulty,
            silhouetteName = silhouette.name,
            parMoves = reverseArrows.size
        )

        // Safety verification: verify with topological solver
        return if (PuzzleSolver.isSolvable(level)) {
            level
        } else {
            // Fallback: build standard solvable grid if random seed encountered density deadlock
            buildGuaranteedFallbackLevel(levelNumber, cols, rows, difficulty, silhouette)
        }
    }

    private fun buildGuaranteedFallbackLevel(
        levelNumber: Int,
        cols: Int,
        rows: Int,
        difficulty: Difficulty,
        silhouette: Silhouette
    ): LevelDefinition {
        val arrows = mutableListOf<Arrow>()
        var count = 0
        // Clean perimeter arrows that escape outward
        for (x in 0 until cols step 2) {
            arrows.add(Arrow("f_${levelNumber}_${++count}", listOf(GridPoint(x, 1), GridPoint(x, 0)), Direction.UP))
            arrows.add(Arrow("f_${levelNumber}_${++count}", listOf(GridPoint(x, rows - 2), GridPoint(x, rows - 1)), Direction.DOWN))
        }
        for (y in 1 until rows - 1 step 2) {
            arrows.add(Arrow("f_${levelNumber}_${++count}", listOf(GridPoint(1, y), GridPoint(0, y)), Direction.LEFT))
            arrows.add(Arrow("f_${levelNumber}_${++count}", listOf(GridPoint(cols - 2, y), GridPoint(cols - 1, y)), Direction.RIGHT))
        }
        return LevelDefinition(
            levelNumber = levelNumber,
            cols = cols,
            rows = rows,
            arrows = arrows,
            difficulty = difficulty,
            silhouetteName = silhouette.name,
            parMoves = arrows.size
        )
    }
}
