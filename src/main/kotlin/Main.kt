import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.animation.AnimationTimer
import javafx.scene.canvas.GraphicsContext
import javafx.scene.text.Text
import kotlin.math.*

const val G = 6.6740831e-11

data class Body(
    var x: Double,
    var y: Double,
    var mass: Double,
    var velocityX: Double = 0.0,
    var velocityY: Double = 0.0,
    val color: Color,
    var accelerationX: Double = 0.0,
    var accelerationY: Double = 0.0,
) {
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

enum class BodiesPreset(val bodies: List<Body>, val displayName: String) {
    TWO_SIMILAR(
        listOf(
            Body(200.0, 200.0, 5000.0, 15.0, 20.0, Color.RED),
            Body(400.0, 200.0, 5000.0, -15.0, -15.0, Color.BLUE)
        ),
        "Dwa podobne ciała"
    ),
    ONE_BIG_ONE_SMALL(
        listOf(
            Body(200.0, 200.0, 5000.0, 0.0, 1.0, Color.RED),
            Body(400.0, 200.0, 0.001, -20.0, -20.0, Color.BLUE)
        ),
        "Duże i małe ciało"
    ),
    THREE_SIMILAR(
        listOf(
            Body(
                -0.97000436 * (200) + 300, 0.24308753 * (200) + 250,
                2000.0,
                0.466203685 * 25.83424684406, 0.43236573 * 25.83424684406,
                Color.RED
            ),
            Body(
                0.0 * (200) + 300, 0.0 * (200) + 250,
                2000.0,
                -0.93240737 * 25.83424684406, -0.86473146 * 25.83424684406,
                Color.BLUE
            ),
            Body(
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
            Body(200.0, 200.0, 5000.0, 0.0, 1.0, Color.RED),
            Body(400.0, 200.0, 0.001, -25.0, -25.0, Color.BLUE),
            Body(300.0, 373.20508075689, 0.001, 25.0, -25.0, Color.GREEN)
        ),
        "Duże i dwa małe ciała"
    )
}

class BodySimulator : Application() {
    private var bodyCount = 2
    private var simulationSpeed = 1.0
    private var isLightTheme = false

    private var isRunning = false
    private var lastTime = 0L

    private var preset = BodiesPreset.TWO_SIMILAR
    private var bodies = preset.bodies.map { it.copy() }.toMutableList()

    // Elements that need to be modified not only with user input (like for resetting):
    private lateinit var canvas: Canvas
    private lateinit var startStopButton: Button
    private lateinit var speedSlider: Slider
    private lateinit var bodyCountBox: ComboBox<String>
    private lateinit var bodiesVBox: VBox
    private lateinit var scrollPane: ScrollPane

    private val bodyFields = mutableListOf<List<TextField>>()

    private lateinit var timer: AnimationTimer

    override fun start(stage: Stage) {

        val borderPane = BorderPane()

        // Simulation space:
        canvas = Canvas(600.0, 600.0)
        val canvasPane = StackPane(canvas)
        canvasPane.style = "-fx-background-color: black;"
        borderPane.right = canvasPane

        // Control bar on the left:
        val controlsVBox = VBox(10.0)
        controlsVBox.padding = Insets(10.0)
        controlsVBox.prefWidth = 350.0

        // Buttons:
        val buttonBar = HBox(10.0)
        startStopButton = Button("Start")
        val resetButton = Button("Reset")

        startStopButton.setOnAction {
            if (isRunning) {
                pauseAnimation()
            } else {
                startAnimation()
            }
        }
        resetButton.setOnAction {
            resetSimulation()
        }

        // Body count:
        bodyCountBox = ComboBox<String>()
        bodyCountBox.items.addAll("2 Ciała", "3 Ciała")
        bodyCountBox.value = "2 Ciała"

        bodyCountBox.valueProperty().addListener { _, _, newValue ->
            if (newValue == "2 Ciała" && bodyCount == 3) {
                bodies.removeLast()
                bodyCount = 2
                drawBodies()
            } else if (newValue == "3 Ciała" && bodyCount == 2) {
                val thirdBody = preset.bodies.getOrNull(2)
                    ?: BodiesPreset.ONE_BIG_TWO_SMALL.bodies[2].copy(mass = 5000.0)
                bodies.add(thirdBody)
                bodyCount = 3
                drawBodies()
            }
            updateBodyControls()
        }

        // Color theme:
        val themeComboBox = ComboBox<String>()
        themeComboBox.items.addAll("Czarne tło", "Białe tło")
        themeComboBox.value = "Czarne tło"

        themeComboBox.valueProperty().addListener { _, _, newValue ->
            if (newValue == "Czarne tło" && isLightTheme || newValue == "Białe tło" && !isLightTheme) {
                isLightTheme = !isLightTheme
            }
            drawBodies()
        }

        buttonBar.children.addAll(startStopButton, resetButton, bodyCountBox, themeComboBox)

        // Simulation speed slider:
        val speedSliderContainer = HBox(10.0)
        val speedSliderLabel = Text("Szybkość symulacji:")
        speedSlider = Slider(0.1, 10.0, 1.0)
        speedSlider.prefWidth = 300.0
        speedSlider.isShowTickLabels = true
        speedSlider.isShowTickMarks = true
        speedSlider.majorTickUnit = 1.0
        speedSlider.blockIncrement = 0.25
        speedSlider.valueProperty().addListener { _, _, newValue ->
            simulationSpeed = (newValue.toDouble() * 4).roundToInt() / 4.0
        }
        speedSliderContainer.children.addAll(speedSliderLabel, speedSlider)

        // Preset choice:
        val presetComboBoxContainer = HBox(10.0)
        val presetComboBoxLabel = Text("Zagadnienia:")
        val presetComboBox = ComboBox<String>()
        presetComboBox.items.addAll(BodiesPreset.entries.map { it.displayName })
        presetComboBox.value = preset.displayName

        presetComboBox.valueProperty().addListener { _, _, newValue ->
            preset = BodiesPreset.entries.first { it.displayName == newValue }
            bodies.clear()
            bodies.addAll(preset.bodies.map { it.copy() })
            bodyCount = preset.bodies.size
            bodyCountBox.value = "$bodyCount Ciała"
            updateBodyControls()
            drawBodies()
        }

        presetComboBoxContainer.children.addAll(presetComboBoxLabel, presetComboBox)
        controlsVBox.children.addAll(buttonBar, speedSliderContainer, presetComboBoxContainer)

        // Body info:
        bodiesVBox = VBox(10.0)
        scrollPane = ScrollPane(bodiesVBox)
        scrollPane.isFitToWidth = true
        controlsVBox.children.add(scrollPane)
        VBox.setVgrow(scrollPane, Priority.ALWAYS)

        borderPane.left = controlsVBox

        updateBodyControls()
        drawBodies()

        val scene = Scene(borderPane, 950.0, 610.0)
        stage.title = "Symulator Grawitacji"
        stage.scene = scene
        stage.show()

        timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (lastTime > 0) {
                    val deltaTime: Double = (now - lastTime) * 1e-9 * simulationSpeed
                    updateSimulation(deltaTime)
                    updateBodyControls()
                    drawBodies()
                }
                lastTime = now
            }
        }
    }

    private fun updateBodyControls() {
        bodiesVBox.children.clear()
        bodyFields.clear()

        for (i in bodies.indices) {
            val body = bodies[i]

            val bodyContainer = VBox(5.0).apply {
                padding = Insets(10.0)
                style =
                    "-fx-border-color: #${body.color.toString().substring(2, 8)};" +
                            "-fx-border-width: 2px;" +
                            "-fx-border-radius: 5px;" +
                            "-fx-background-radius: 5px;" +
                            "-fx-background-color: #f9f9f9;"
            }

            val title = Text("Ciało ${i + 1}")
            title.style =
                "-fx-font: 15px Montserrat;" +
                        "-fx-font-weight: bold;" +
                        "-fx-fill: #${body.color.toString().substring(2, 8)};" +
                        "-fx-stroke: #666666;" +
                        "-fx-stroke-width: 1;"

            val grid = GridPane()
            grid.hgap = 10.0
            grid.vgap = 5.0
            grid.padding = Insets(0.0, 0.0, 10.0, 0.0)

            val labelStyle = "-fx-font-size: 12px; -fx-fill: black;"

            if (!isRunning) {

                grid.add(Text("Masa:").apply { style = labelStyle }, 0, 0)
                grid.add(Text("Pozycja (X, Y):").apply { style = labelStyle }, 0, 1)
                grid.add(Text("Prędkość (X, Y):").apply { style = labelStyle }, 0, 2)

                val massField = TextField(body.mass.toString()).apply {
                    textProperty().addListener { _, oldValue, newValue ->
                        body.mass = newValue.toDoubleOrNull() ?: oldValue.toDoubleOrNull() ?: body.mass
                        if (newValue.toDoubleOrNull() == null && newValue.isNotEmpty()) text = oldValue
                        drawBodies()
                    }
                }
                val xField = TextField(body.x.toString()).apply {
                    prefWidth = 60.0
                    textProperty().addListener { _, oldValue, newValue ->
                        body.x = newValue.toDoubleOrNull() ?: oldValue.toDoubleOrNull() ?: body.x
                        if (newValue.toDoubleOrNull() == null && newValue.isNotEmpty()) text = oldValue
                        drawBodies()
                    }
                }
                val yField = TextField(body.y.toString()).apply {
                    prefWidth = 60.0
                    textProperty().addListener { _, oldValue, newValue ->
                        body.y = newValue.toDoubleOrNull() ?: oldValue.toDoubleOrNull() ?: body.y
                        if (newValue.toDoubleOrNull() == null && newValue.isNotEmpty()) text = oldValue
                        drawBodies()
                    }
                }
                val velocityXField = TextField(body.velocityX.toString()).apply {
                    prefWidth = 60.0
                    textProperty().addListener { _, oldValue, newValue ->
                        body.velocityX = newValue.toDoubleOrNull() ?: oldValue.toDoubleOrNull() ?: body.velocityX
                        if (newValue.toDoubleOrNull() == null && newValue.isNotEmpty()
                            && newValue != "-") text = oldValue
                        drawBodies()
                    }
                }
                val velocityYField = TextField(body.velocityY.toString()).apply {
                    prefWidth = 60.0
                    textProperty().addListener { _, oldValue, newValue ->
                        body.velocityY = newValue.toDoubleOrNull() ?: oldValue.toDoubleOrNull() ?: body.velocityY
                        if (newValue.toDoubleOrNull() == null && newValue.isNotEmpty()
                            && newValue != "-") text = oldValue
                        drawBodies()
                    }
                }

                val fields = listOf(xField, yField, massField, velocityXField, velocityYField)
                bodyFields.add(fields)

                grid.add(massField, 1, 0, 3, 1)
                grid.add(xField, 1, 1)
                grid.add(yField, 3, 1)
                grid.add(velocityXField, 1, 2)
                grid.add(velocityYField, 3, 2)

                grid.add(Text("[Pg]").apply { style = labelStyle }, 4, 0)
                grid.add(Text("[px]").apply { style = labelStyle }, 2, 1)
                grid.add(Text("[px]").apply { style = labelStyle }, 4, 1)
                grid.add(Text("[px/s]").apply { style = labelStyle }, 2, 2)
                grid.add(Text("[px/s]").apply { style = labelStyle }, 4, 2)

            } else {

                grid.add(Text("Masa:").apply { style = labelStyle }, 0, 0)
                grid.add(Text("Prędkość:").apply { style = labelStyle }, 0, 1)
                grid.add(Text("Przyspieszenie:").apply { style = labelStyle }, 0, 2)

                grid.add(Text(String.format("%.2f", body.mass)).apply { style = labelStyle }, 1, 0)
                val totalVelocity = sqrt(body.velocityX.pow(2) + body.velocityY.pow(2))
                grid.add(Text(String.format("%.2f", totalVelocity)).apply { style = labelStyle }, 1, 1)
                val totalAcceleration = sqrt(body.accelerationX.pow(2) + body.accelerationY.pow(2))
                grid.add(Text(String.format("%.2f", totalAcceleration)).apply { style = labelStyle }, 1, 2)

                grid.add(Text("[Pg] = ${body.mass * 1e12} [kg]").apply { style = labelStyle }, 2, 0)
                grid.add(Text("[m/s]   (1m = 1 pixel)").apply { style = labelStyle }, 2, 1)
                grid.add(Text("[m/s^2]").apply { style = labelStyle }, 2, 2)

            }

            bodyContainer.children.addAll(title, grid)
            bodiesVBox.children.add(bodyContainer)

        }
    }

    private fun startAnimation() {
        for (i in bodies.indices) {
            val fields = bodyFields[i]
            val body = bodies[i]
            val currentPreset = if (i < preset.bodies.size) preset
            else BodiesPreset.ONE_BIG_TWO_SMALL.apply { bodies.last().mass = 5000.0 }

            if (fields[0].text.isEmpty()) body.x = currentPreset.bodies[i].x
            if (fields[1].text.isEmpty()) body.y = currentPreset.bodies[i].y
            if (fields[2].text.isEmpty() || fields[2].text.toDouble() == 0.0) body.mass = currentPreset.bodies[i].mass
            if (fields[3].text.isEmpty() || fields[3].text == "-") body.velocityX = currentPreset.bodies[i].velocityX
            if (fields[4].text.isEmpty() || fields[4].text == "-") body.velocityY = currentPreset.bodies[i].velocityY
        }

        isRunning = true
        startStopButton.text = "Stop"
        lastTime = 0
        timer.start()
        updateBodyControls()
    }

    private fun pauseAnimation() {
        isRunning = false
        startStopButton.text = "Start"
        timer.stop()
        updateBodyControls()
        drawBodies()
    }

    private fun resetSimulation() {
        if (isRunning) {
            pauseAnimation()
        }

        bodies = preset.bodies.map { it.copy() }.toMutableList()
        bodyCount = preset.bodies.size
        bodyCountBox.value = "$bodyCount Ciała"
        simulationSpeed = 1.0
        speedSlider.value = 1.0

        updateBodyControls()
        drawBodies()
    }

    private fun updateSimulation(deltaTime: Double) {
        calculateNewPositions(deltaTime)
        bodies.forEach { it.addToTrajectory() }
    }

    private fun calculateNewPositions(deltaTime: Double) {
        val accelerations = Array(bodies.size) { DoubleArray(2) }

        for (i in bodies.indices) {
            for (j in bodies.indices) {
                if (i == j) continue

                val body1 = bodies[i]
                val body2 = bodies[j]

                val dx = body2.x - body1.x
                val dy = body2.y - body1.y
                val distanceSquared = dx * dx + dy * dy
                val distance = sqrt(distanceSquared)

                val force = if (distanceSquared > 0.0) {
                    G * body1.mass * body2.mass * 1e24 / distanceSquared
                } else {
                    0.0
                }

                val acceleration = force / (body1.mass * 1e12)
                accelerations[i][0] += acceleration * dx / distance
                accelerations[i][1] += acceleration * dy / distance
            }
        }

        for (i in bodies.indices) {
            val body = bodies[i]
            body.accelerationX = accelerations[i][0]
            body.accelerationY = accelerations[i][1]

            body.velocityX += accelerations[i][0] * deltaTime
            body.velocityY += accelerations[i][1] * deltaTime

            body.x += body.velocityX * deltaTime
            body.y += body.velocityY * deltaTime
        }
    }

    private fun drawBodies() {
        val gc = canvas.graphicsContext2D

        gc.fill = if (isLightTheme) Color.WHITE else Color.BLACK
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)

        val radius = 10.0

        for (body in bodies) {
            gc.stroke = body.color
            gc.lineWidth = 1.0

            for (i in 1..<body.trajectory.size) {
                val prev = body.trajectory[i - 1]
                val curr = body.trajectory[i]
                gc.strokeLine(prev.first, prev.second, curr.first, curr.second)
            }
        }

        for (body in bodies) {

            // Vector arrows:
            val scale = 1.5

            val totalVelocity = sqrt(body.velocityX.pow(2) + body.velocityY.pow(2))
            if (totalVelocity > 0) {
                drawArrow(
                    gc,
                    body.x,
                    body.y,
                    body.x + body.velocityX * scale,
                    body.y + body.velocityY * scale,
                    body.color,
                    radius
                )
            }

            val totalAcceleration = sqrt(body.accelerationX.pow(2) + body.accelerationY.pow(2))
            if (totalAcceleration > 0) {
                drawArrow(
                    gc,
                    body.x,
                    body.y,
                    body.x + body.accelerationX * scale,
                    body.y + body.accelerationY * scale,
                    if (isLightTheme) Color.BLACK else Color.WHITE,
                    radius
                )
            }

            gc.fill = body.color
            gc.fillOval(body.x - radius, body.y - radius, radius * 2, radius * 2)
            gc.stroke = if (isLightTheme) Color.BLACK else Color.WHITE
            gc.lineWidth = 2.0
            gc.strokeOval(body.x - radius, body.y - radius, radius * 2, radius * 2)
        }
    }
}

private fun drawArrow(
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


fun main() {
    Application.launch(BodySimulator::class.java)
}