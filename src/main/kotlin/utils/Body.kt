import javafx.scene.paint.Color
import kotlin.math.pow
import kotlin.math.sqrt

data class Body2D(
    val color: Color,
    var mass: Double,
    var x: Double,
    var y: Double,
    var velocityX: Double = 0.0,
    var velocityY: Double = 0.0,
) {
    var accelerationX = 0.0
    var accelerationY = 0.0
    val trajectory = mutableListOf<Pair<Double, Double>>()

    fun addToTrajectory() {
        if (trajectory.size > 0 &&
            sqrt((trajectory.last().first - x).pow(2) + (trajectory.last().second - y).pow(2)) < 5) return
        trajectory.add(Pair(x, y))
        if (trajectory.size > 1000) {
            trajectory.removeAt(0)
        }
    }
}

data class Body3D(
    val color: Color,
    var mass: Double,
    var x: Double,
    var y: Double,
    var z: Double,
    var velocityX: Double = 0.0,
    var velocityY: Double = 0.0,
    var velocityZ: Double = 0.0,
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
        if (trajectory.size > 1000) {
            trajectory.removeAt(0)
        }
    }
}

enum class BodiesPreset2D(val bodies: List<Body2D>, val displayName: String) {
    TWO_SIMILAR(
        listOf(
            Body2D(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 15.0, velocityY = 20.0
            ),
            Body2D(
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
            Body2D(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 0.0, velocityY = 1.0
            ),
            Body2D(
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
            Body2D(
                color = Color.RED,
                mass = 2000.0,
                x = -0.97000436 * (200) + 300, y = 0.24308753 * (200) + 250,
                velocityX = 0.466203685 * 25.83424684406, velocityY = 0.43236573 * 25.83424684406
            ),
            Body2D(
                color = Color.BLUE,
                mass = 2000.0,
                x = 0.0 * (200) + 300, y = 0.0 * (200) + 250,
                velocityX = -0.93240737 * 25.83424684406, velocityY = -0.86473146 * 25.83424684406
            ),
            Body2D(
                color = Color.GREEN,
                mass = 2000.0,
                x = 400.0, y = 200.0,
                velocityX = -20.0, velocityY = -20.0
            ),
        ),
        listOf(
            Body2D(
                0.0 * (200) + 300, 0.0 * (200) + 250,
                2000.0,
                -0.93240737 * 25.83424684406, -0.86473146 * 25.83424684406,
                Color.BLUE
            ),
            Body2D(
                0.97000436 * (200) + 300, -0.24308753 * (200) + 250,
                2000.0,
                0.466203685 * 25.83424684406, 0.43236573 * 25.83424684406,
                Color.GREEN
            )
        ),
        "Trzy podobne ciała"
    ),
    ONE_BIG_TWO_SMALL(
        listOf(
            Body2D(
                color = Color.RED,
                mass = 5000.0,
                x = 200.0, y = 200.0,
                velocityX = 0.0, velocityY = 1.0
            ),
            Body2D(
                color = Color.BLUE,
                mass = 0.001,
                x = 400.0, y = 200.0,
                velocityX = -20.0, velocityY = -20.0
            ),
            Body2D(
                color = Color.GREEN,
                mass = 0.001,
                x = 400.0, y = 200.0,
                velocityX = -20.0, velocityY = -20.0
            ),
        ),
        listOf(
            Body2D(200.0, 200.0, 5000.0, 0.0, 1.0, Color.RED),
            Body2D(400.0, 200.0, 0.001, -25.0, -25.0, Color.BLUE),
            Body2D(300.0, 373.20508075689, 0.001, 25.0, -25.0, Color.GREEN)
        ),
        "Duże i dwa małe ciała"
    )
}