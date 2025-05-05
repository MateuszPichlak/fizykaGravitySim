package utils

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.*

data class Body(
    val color: Color,
    var mass: Double,
    var x: Double,
    var y: Double,
    var z: Double = 0.0,
    var velocityX: Double = 0.0,
    var velocityY: Double = 0.0,
    var velocityZ: Double = 0.0,
    var trajectorySize: Int = 1000,
) {
    var accelerationX = 0.0
    var accelerationY = 0.0
    var accelerationZ = 0.0
    val trajectory = mutableListOf<Triple<Double, Double, Double>>()

    fun addToTrajectory() {
        if (trajectory.size > 0 &&
            sqrt((trajectory.last().first - x).pow(2) +
                    (trajectory.last().second - y).pow(2) +
                    (trajectory.last().third - z).pow(2)) < 5)
            return
        trajectory.add(Triple(x, y, z))
        if (trajectory.size > trajectorySize) {
            trajectory.removeAt(0)
        }
    }
}

enum class BodiesPreset2D(val bodies: List<Body>, val displayName: String) {
    TWO_SIMILAR(
        listOf(
            Body(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 15.0, velocityY = 20.0
            ),
            Body(
                color = Color.BLUE,
                mass = 5000.0,
                x = 400.0, y = 200.0,
                velocityX = -15.0, velocityY = -15.0
            ),
        ),
        "Dwa podobne ciała"
    ),
    ONE_BIG_ONE_SMALL(
        listOf(
            Body(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 0.0, velocityY = 1.0
            ),
            Body(
                color = Color.BLUE,
                mass = 0.001,
                x = 400.0, y = 200.0,
                velocityX = -20.0, velocityY = -20.0
            ),
        ),
        "Duże i małe ciało"
    ),
    THREE_SIMILAR(
        listOf(
            Body(
                color = Color.RED,
                mass = 2000.0,
                x = -0.97000436 * (200) + 300, y = 0.24308753 * (200) + 250,
                velocityX = 0.466203685 * 25.83424684406, velocityY = 0.43236573 * 25.83424684406
            ),
            Body(
                color = Color.BLUE,
                mass = 2000.0,
                x = 0.0 * (200) + 300, y = 0.0 * (200) + 250,
                velocityX = -0.93240737 * 25.83424684406, velocityY = -0.86473146 * 25.83424684406
            ),
            Body(
                color = Color.GREEN,
                mass = 2000.0,
                x = 0.97000436 * (200) + 300, y = -0.24308753 * (200) + 250,
                velocityX = 0.466203685 * 25.83424684406, velocityY = 0.43236573 * 25.83424684406
            ),
        ),
        "Trzy podobne ciała"
    ),
    ONE_BIG_TWO_SMALL(
        listOf(
            Body(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 0.0, velocityY = 1.0
            ),
            Body(
                color = Color.BLUE,
                mass = 0.001,
                x = 400.0, y = 200.0,
                velocityX = -25.0, velocityY = -25.0
            ),
            Body(
                color = Color.GREEN,
                mass = 0.001,
                x = 300.0, y = 373.20508075689,
                velocityX = 25.0, velocityY = -25.0
            ),
        ),
        "Duże i dwa małe ciała"
    )
}

const val TRAJECTORY_SIZE_3D = 100

enum class BodiesPreset3D(val bodies: List<Body>, val displayName: String) {
    TWO_SIMILAR(
        listOf(
            Body(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 15.0, velocityY = 20.0, velocityZ = 15.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
            Body(
                color = Color.BLUE,
                mass = 5000.0,
                x = 400.0, y = 200.0,
                velocityX = -15.0, velocityY = -15.0, velocityZ = -15.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
        ),
        "Dwa podobne ciała"
    ),
    ONE_BIG_ONE_SMALL(
        listOf(
            Body(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 0.0, velocityY = 1.0, velocityZ = 1.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
            Body(
                color = Color.BLUE,
                mass = 0.001,
                x = 400.0, y = 200.0,
                velocityX = -20.0, velocityY = -20.0, velocityZ = -20.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
        ),
        "Duże i małe ciało"
    ),
    THREE_SIMILAR(
        listOf(
            Body(
                color = Color.RED,
                mass = 2000.0,
                x = -0.97000436 * (200) + 300, y = 0.24308753 * (200) + 250,
                velocityX = 0.466203685 * 25.83424684406, velocityY = 0.43236573 * 25.83424684406,
                velocityZ = -10.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
            Body(
                color = Color.BLUE,
                mass = 2000.0,
                x = 0.0 * (200) + 300, y = 0.0 * (200) + 250,
                velocityX = -0.93240737 * 25.83424684406, velocityY = -0.86473146 * 25.83424684406,
                velocityZ = -10.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
            Body(
                color = Color.GREEN,
                mass = 2000.0,
                x = 0.97000436 * (200) + 300, y = -0.24308753 * (200) + 250,
                velocityX = 0.466203685 * 25.83424684406, velocityY = 0.43236573 * 25.83424684406,
                velocityZ = -10.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
        ),
        "Trzy podobne ciała"
    ),
    ONE_BIG_TWO_SMALL(
        listOf(
            Body(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 0.0, velocityY = 1.0, velocityZ = 1.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
            Body(
                color = Color.BLUE,
                mass = 0.001,
                x = 400.0, y = 200.0,
                velocityX = -25.0, velocityY = -25.0, velocityZ = -25.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
            Body(
                color = Color.GREEN,
                mass = 0.001,
                x = 300.0, y = 373.20508075689,
                velocityX = 25.0, velocityY = -25.0, velocityZ = -25.0,
                trajectorySize = TRAJECTORY_SIZE_3D
            ),
        ),
        "Duże i dwa małe ciała"
    )
}

fun drawArrow(
    gc: GraphicsContext,
    startX: Double,
    startY: Double,
    endX: Double,
    endY: Double,
    color: Color,
    shift: Double
) {
    val angle = atan2(endY - startY, endX - startX)

    val shiftedStartX = startX + shift * cos(angle)
    val shiftedStartY = startY + shift * sin(angle)
    val shiftedEndX = endX + shift * cos(angle)
    val shiftedEndY = endY + shift * sin(angle)

    gc.stroke = color
    gc.lineWidth = 2.0
    gc.strokeLine(shiftedStartX, shiftedStartY, shiftedEndX, shiftedEndY)

    val arrowHeadSize = 5.0

    val x1 = shiftedEndX - arrowHeadSize * cos(angle - PI / 6)
    val y1 = shiftedEndY - arrowHeadSize * sin(angle - PI / 6)

    val x2 = shiftedEndX - arrowHeadSize * cos(angle + PI / 6)
    val y2 = shiftedEndY - arrowHeadSize * sin(angle + PI / 6)

    gc.strokeLine(shiftedEndX, shiftedEndY, x1, y1)
    gc.strokeLine(shiftedEndX, shiftedEndY, x2, y2)
}