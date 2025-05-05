import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.animation.AnimationTimer
import javafx.geometry.Pos
import javafx.scene.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import kotlin.math.*
import utils.*
import kotlin.reflect.KMutableProperty1

const val G = 6.6740831e-11

class BodySimulator : Application() {
    private var is3D = false
    private var bodyCount = 2
    private var simulationSpeed = 1.0
    private var isLightTheme = false

    private var isRunning = false
    private var lastFrameTime = 0L

    private var currentPreset2D = BodiesPreset2D.TWO_SIMILAR
    private var bodies2D = currentPreset2D.bodies.map { it.copy() }.toMutableList()

    private var currentPreset3D = BodiesPreset3D.TWO_SIMILAR
    private var bodies3D = currentPreset3D.bodies.map { it.copy() }.toMutableList()

    // Elements that need to be modified not only with user input (like for resetting):
    private lateinit var canvas: Canvas
    private lateinit var startStopButton: Button
    private lateinit var speedSlider: Slider
    private lateinit var dimensionCombo: ComboBox<String>
    private lateinit var bodyCountCombo: ComboBox<String>
    private lateinit var bodyInfoVBox: VBox

    private lateinit var renderer3D: Renderer3D
    private lateinit var timer: AnimationTimer

    override fun start(stage: Stage) {

        val borderPane = BorderPane()
        stage.apply {
            title = "Symulator Grawitacji"
            scene = Scene(borderPane, 1000.0, 610.0)
            icons.add(Image(javaClass.getResourceAsStream("/icons/m3/app_icon.png")))
        }

        //=== ANIMATION SPACE ==========================================================================================

        canvas = Canvas(600.0, 620.0)
        val canvasPane = StackPane(canvas).apply { style = "-fx-background-color: black;" }
        borderPane.right = canvasPane

        // 3D
        val root3D = Group()
        val camera3D = PerspectiveCamera(true).apply {
            nearClip = 0.1
            farClip = 10000.0
            fieldOfView = 45.0
        }
        val scene3D = SubScene(root3D, 600.0, 600.0, true, SceneAntialiasing.BALANCED).apply {
            fill = Color.BLACK
            camera = camera3D
            widthProperty().bind(canvasPane.widthProperty())
            heightProperty().bind(canvasPane.heightProperty())
        }

        renderer3D = Renderer3D(scene3D, camera3D, bodyRadius = 10.0)


        //=== CONTROLS =================================================================================================

        val controlsVBox = VBox(10.0).apply {
            padding = Insets(10.0)
            prefWidth = 400.0
        }
        borderPane.left = controlsVBox

        // First control row:
        val firstControlRow = HBox(10.0)
        startStopButton = Button().apply {
            graphic = ImageView(Image(javaClass.getResourceAsStream("/icons/m3/start.png")))
            padding = Insets(0.0)
            shape = Circle(20.0)
            tooltip = Tooltip("Start")
            setOnAction {
                if (isRunning) pauseAnimation()
                else startAnimation()
            }
        }
        val resetButton = Button().apply {
            graphic = ImageView(Image(javaClass.getResourceAsStream("/icons/m3/reset.png")))
            padding = Insets(0.0)
            shape = Circle(20.0)
            tooltip = Tooltip("Reset")
            setOnAction { resetSimulation() }
        }
        val buttonContainer = HBox().apply {
            children.addAll(startStopButton, resetButton)
            spacing = 4.0
        }
        val ovalBorder = Rectangle(84.0, 40.0).apply {
            arcWidth = 40.0
            arcHeight = 84.0
            fill = Color.TRANSPARENT
            stroke = Color.LIGHTGRAY
        }
        val buttonBar = StackPane().apply {
            children.addAll(ovalBorder, buttonContainer)
            alignment = Pos.TOP_LEFT
        }

        dimensionCombo = ComboBox<String>().apply {
            items.addAll("2D", "3D")
            value = "2D"
            valueProperty().addListener { _, _, newValue ->
                if (newValue == "3D" == is3D) return@addListener
                is3D = newValue == "3D"
                canvasPane.children.clear()
                if (is3D) canvasPane.children.add(scene3D)
                else canvasPane.children.add(canvas)
                drawBodies()
                updateBodyControls()
            }
        }

        bodyCountCombo = ComboBox<String>().apply {
            items.addAll("2 Ciała", "3 Ciała")
            value = "2 Ciała"
            valueProperty().addListener { _, _, newValue ->
                if (newValue == "2 Ciała" && bodyCount == 3) {
                    bodies2D.removeLast()
                    bodies3D.removeLast()
                    bodyCount = 2
                    drawBodies()
                } else if (newValue == "3 Ciała" && bodyCount == 2) {
                    val thirdBody2D = currentPreset2D.bodies.getOrNull(2)?.copy()
                        ?: BodiesPreset2D.ONE_BIG_TWO_SMALL.bodies[2].copy(mass = 5000.0)
                    bodies2D.add(thirdBody2D)
                    val thirdBody3D = currentPreset3D.bodies.getOrNull(2)?.copy()
                        ?: BodiesPreset2D.ONE_BIG_TWO_SMALL.bodies[2].copy(mass = 5000.0)
                    bodies3D.add(thirdBody3D)
                    bodyCount = 3
                    drawBodies()
                }
                updateBodyControls()
            }
        }

        val themeCombo = ComboBox<String>().apply {
            items.addAll("Czarne tło", "Białe tło")
            value = "Czarne tło"
            valueProperty().addListener { _, _, newValue ->
                isLightTheme = newValue == "Białe tło"
                renderer3D.isLightTheme = isLightTheme
                drawBodies()
            }
        }

        firstControlRow.apply {
            children.addAll(buttonBar, dimensionCombo, bodyCountCombo, themeCombo)
            alignment = Pos.CENTER_LEFT
        }

        // Second control row (speed slider):
        val secondControlRow = HBox(10.0)
        val speedSliderLabel = Text("Szybkość symulacji:")
        speedSlider = Slider(0.1, 10.0, 1.0).apply {
            prefWidth = 300.0
            isShowTickLabels = true
            isShowTickMarks = true
            majorTickUnit = 1.0
            blockIncrement = 0.25
            valueProperty().addListener { _, _, newValue ->
                simulationSpeed = (newValue.toDouble() * 4).roundToInt() / 4.0
            }
        }
        secondControlRow.children.addAll(speedSliderLabel, speedSlider)

        // Third control row (preset choice):
        val thirdControlRow = HBox(10.0)
        val presetComboLabel = Text("Zagadnienia:")
        val presetCombo = ComboBox<String>().apply {
            items.addAll(BodiesPreset2D.entries.map { it.displayName })
            value = currentPreset2D.displayName
            valueProperty().addListener { _, _, newValue ->
                currentPreset2D = BodiesPreset2D.entries.first { it.displayName == newValue }
                bodies2D.clear()
                bodies2D.addAll(currentPreset2D.bodies.map { it.copy() })
                currentPreset3D = BodiesPreset3D.entries.first { it.displayName == newValue }
                bodies3D.clear()
                bodies3D.addAll(currentPreset3D.bodies.map { it.copy() })
                bodyCount = currentPreset2D.bodies.size
                bodyCountCombo.value = "$bodyCount Ciała"
                updateBodyControls()
                drawBodies()
            }
        }
        thirdControlRow.children.addAll(presetComboLabel, presetCombo)

        controlsVBox.children.addAll(firstControlRow, secondControlRow, thirdControlRow)

        // Body info box (under the controls) scrolling when too long:
        bodyInfoVBox = VBox(10.0)
        val bodyInfoPane = ScrollPane(bodyInfoVBox).apply { isFitToWidth = true }
        controlsVBox.children.add(bodyInfoPane)
        VBox.setVgrow(bodyInfoPane, Priority.ALWAYS)

        updateBodyControls()
        drawBodies2D()

        stage.show()


        //=== ANIMATION ================================================================================================

        timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                if (lastFrameTime > 0) {
                    val deltaTime: Double = (now - lastFrameTime) * 1e-9 * simulationSpeed
                    updateSimulation(deltaTime)
                    updateBodyControls()
                    drawBodies()
                }
                lastFrameTime = now
            }
        }
    }

    private fun makeBodyInputField(
        body: Body,
        bodyProperty: KMutableProperty1<Body, Double>,
        width: Double? = 60.0,
        canBeNegative: Boolean = true,
        canBeZero: Boolean = true,
    ): TextField {
        val propertyValue = bodyProperty.get(body)
        return TextField(propertyValue.toString()).apply {
            if (width != null) prefWidth = width

            textProperty().addListener { _, oldValue, newValue ->
                val newValueDouble = newValue.toDoubleOrNull()
                // Allow for writing more after the zero, but don't change the property value:
                if (!canBeZero && newValueDouble == 0.0) return@addListener
                bodyProperty.set(body,
                    newValueDouble ?: oldValue.toDoubleOrNull() ?: propertyValue
                )
                // Invalid input:
                if ((newValue.isNotEmpty()) && !(canBeNegative && newValue == "-") &&
                        ((newValueDouble == null) || !(newValueDouble.isFinite())))
                    text = oldValue
                drawBodies()
            }

        }
    }

    private fun updateBodyControls() {
        bodyInfoVBox.children.clear()

        val bodies = if (is3D) bodies3D else bodies2D
        for (bodyIndex in bodies.indices) {
            val body = bodies[bodyIndex]

            val bodyContainer = VBox(5.0).apply {
                padding = Insets(10.0)
                style = "-fx-border-color: #${body.color.toString().substring(2, 8)};" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 5px;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-background-color: #f9f9f9;"
            }

            val title = Text("Ciało ${bodyIndex + 1}").apply {
                style = "-fx-font: 15px Montserrat;" +
                        "-fx-font-weight: bold;" +
                        "-fx-fill: #${body.color.toString().substring(2, 8)};" +
                        "-fx-stroke: #666666;" +
                        "-fx-stroke-width: 1;"
            }

            val contentGrid = GridPane().apply {
                hgap = 10.0
                vgap = 5.0
                padding = Insets(0.0, 0.0, 10.0, 0.0)
            }

            val labelStyle = "-fx-font-size: 12px; -fx-fill: black;"

            if (!isRunning) {

                contentGrid.add(Text("Masa:").apply { style = labelStyle }, 0, 0)
                contentGrid.add(Text("Pozycja:").apply { style = labelStyle }, 0, 1)
                contentGrid.add(Text("Prędkość:").apply { style = labelStyle }, 0, 2)

                contentGrid.add(Text("X =").apply { style = labelStyle }, 1, 1)
                contentGrid.add(Text("Y =").apply { style = labelStyle }, 3, 1)
                contentGrid.add(Text("X =").apply { style = labelStyle }, 1, 2)
                contentGrid.add(Text("Y =").apply { style = labelStyle }, 3, 2)
                contentGrid.add(Text("[Pg]").apply { style = labelStyle }, if (is3D) 7 else 5, 0)
                contentGrid.add(Text("[px]").apply { style = labelStyle }, if (is3D) 7 else 5, 1)
                contentGrid.add(Text("[px/s]").apply { style = labelStyle }, if (is3D) 7 else 5, 2)

                val massField = makeBodyInputField(
                    body, Body::mass, width = null, canBeNegative = false, canBeZero = false
                )
                val xField = makeBodyInputField(body, Body::x)
                val yField = makeBodyInputField(body, Body::y)
                val velocityXField = makeBodyInputField(body, Body::velocityX)
                val velocityYField = makeBodyInputField(body, Body::velocityY)

                contentGrid.add(massField, 1, 0, if (is3D) 6 else 4, 1)
                contentGrid.add(xField, 2, 1)
                contentGrid.add(yField, 4, 1)
                contentGrid.add(velocityXField, 2, 2)
                contentGrid.add(velocityYField, 4, 2)

                if (is3D) {
                    val zField = makeBodyInputField(body, Body::z)
                    val velocityZField = makeBodyInputField(body, Body::velocityZ)

                    contentGrid.add(Text("Z =").apply { style = labelStyle }, 5, 1)
                    contentGrid.add(Text("Z =").apply { style = labelStyle }, 5, 2)
                    contentGrid.add(zField, 6, 1)
                    contentGrid.add(velocityZField, 6, 2)
                }

            } else {

                contentGrid.add(Text("Masa:").apply { style = labelStyle }, 0, 0)
                contentGrid.add(Text("Prędkość:").apply { style = labelStyle }, 0, 1)
                contentGrid.add(Text("Przyspieszenie:").apply { style = labelStyle }, 0, 2)

                contentGrid.add(Text(String.format("%.2f", body.mass)).apply { style = labelStyle }, 1, 0)
                val totalVelocity = sqrt(
                    body.velocityX.pow(2) + body.velocityY.pow(2) + body.velocityZ.pow(2)
                )
                contentGrid.add(Text(String.format("%.2f", totalVelocity)).apply { style = labelStyle }, 1, 1)
                val totalAcceleration = sqrt(
                    body.accelerationX.pow(2) + body.accelerationY.pow(2) + body.accelerationZ.pow(2)
                )
                contentGrid.add(Text(String.format("%.2f", totalAcceleration)).apply { style = labelStyle }, 1, 2)

                contentGrid.add(Text("[Pg] = ${body.mass * 1e12} [kg]").apply { style = labelStyle }, 2, 0)
                contentGrid.add(Text("[m/s]   (1m = 1 pixel)").apply { style = labelStyle }, 2, 1)
                contentGrid.add(Text("[m/s^2]").apply { style = labelStyle }, 2, 2)

            }

            bodyContainer.children.addAll(title, contentGrid)
            bodyInfoVBox.children.add(bodyContainer)

        }
    }

    private fun startAnimation() {
        isRunning = true
        lastFrameTime = 0
        timer.start()
        startStopButton.apply {
            graphic = ImageView(Image(javaClass.getResourceAsStream("/icons/m3/stop.png")))
            tooltip = Tooltip("Stop")
        }
        dimensionCombo.isDisable = true
        updateBodyControls()
    }

    private fun pauseAnimation() {
        isRunning = false
        timer.stop()
        startStopButton.apply {
            graphic = ImageView(Image(javaClass.getResourceAsStream("/icons/m3/start.png")))
            tooltip = Tooltip("Start")
        }
        dimensionCombo.isDisable = false
        updateBodyControls()
        drawBodies()
    }

    private fun resetSimulation() {
        if (isRunning) pauseAnimation()

        bodies2D = currentPreset2D.bodies.map { it.copy() }.toMutableList()
        bodies3D = currentPreset2D.bodies.map { it.copy() }.toMutableList()
        bodyCount = currentPreset2D.bodies.size
        bodyCountCombo.value = "$bodyCount Ciała"
        simulationSpeed = 1.0
        speedSlider.value = 1.0

        updateBodyControls()
        drawBodies()
    }

    private fun updateSimulation(deltaTime: Double) {
        calculateNewPositions(deltaTime)
        val bodies = if (is3D) bodies3D else bodies2D
        bodies.forEach { it.addToTrajectory() }
    }

    private fun calculateNewPositions(deltaTime: Double) {
        val bodies = if (is3D) bodies3D else bodies2D
        val accelerations = Array(bodies.size) { DoubleArray(3) }

        for (i in bodies.indices) {
            for (j in bodies.indices) {
                if (i == j) continue

                val body1 = bodies[i]
                val body2 = bodies[j]

                val dx = body2.x - body1.x
                val dy = body2.y - body1.y
                val dz = if (is3D) body2.z - body1.z else 0.0
                val distanceSquared = dx * dx + dy * dy + dz * dz
                val distance = sqrt(distanceSquared)

                val force =
                    if (distanceSquared > 0.0) G * body1.mass * body2.mass * 1e24 / distanceSquared
                    else 0.0

                val acceleration = force / (body1.mass * 1e12)
                accelerations[i][0] += acceleration * dx / distance
                accelerations[i][1] += acceleration * dy / distance
                accelerations[i][2] += acceleration * dz / distance
            }
        }

        for (i in bodies.indices) {
            val body = bodies[i]
            body.accelerationX = accelerations[i][0]
            body.accelerationY = accelerations[i][1]
            body.accelerationZ = accelerations[i][2]

            body.velocityX += accelerations[i][0] * deltaTime
            body.velocityY += accelerations[i][1] * deltaTime
            body.velocityZ += accelerations[i][2] * deltaTime

            body.x += body.velocityX * deltaTime
            body.y += body.velocityY * deltaTime
            body.z += body.velocityZ * deltaTime
        }
    }

    private fun drawBodies() {
        if (is3D) renderer3D.render3DFrame(bodies3D) else drawBodies2D()
    }

    private fun drawBodies2D() {
        val gc = canvas.graphicsContext2D

        gc.fill = if (isLightTheme) Color.WHITE else Color.BLACK
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)

        val radius = 10.0

        for (body in bodies2D) {
            gc.stroke = body.color
            gc.lineWidth = 1.0

            for (i in 1..<body.trajectory.size) {
                val previous = body.trajectory[i - 1]
                val current = body.trajectory[i]
                gc.strokeLine(previous.first, previous.second, current.first, current.second)
            }
        }

        for (body in bodies2D) {

            // Vector arrows:
            val scale = 1.5

            val totalVelocity = sqrt(body.velocityX.pow(2) + body.velocityY.pow(2))
            if (totalVelocity > 0)
                drawArrow(gc,
                    body.x, body.y,
                    body.x + body.velocityX * scale, body.y + body.velocityY * scale,
                    body.color, radius
                )

            val totalAcceleration = sqrt(body.accelerationX.pow(2) + body.accelerationY.pow(2))
            if (totalAcceleration > 0)
                drawArrow(gc,
                    body.x, body.y,
                    body.x + body.accelerationX * scale, body.y + body.accelerationY * scale,
                    if (isLightTheme) Color.BLACK else Color.WHITE, radius
                )

            gc.apply {
                fill = body.color
                fillOval(body.x - radius, body.y - radius, radius * 2, radius * 2)
                stroke = if (isLightTheme) Color.BLACK else Color.WHITE
                lineWidth = 2.0
                strokeOval(body.x - radius, body.y - radius, radius * 2, radius * 2)
            }
        }
    }
}


fun main() {
    Application.launch(BodySimulator::class.java)
}