package utils

import javafx.geometry.Point3D
import javafx.scene.SubScene
import javafx.scene.PerspectiveCamera
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Sphere
import javafx.scene.AmbientLight
import javafx.scene.PointLight
import javafx.scene.Group
import javafx.scene.shape.Cylinder
import kotlin.math.*

class Renderer3D(
    private val subScene: SubScene,
    private val camera: PerspectiveCamera,
    private val bodyRadius: Double = 10.0
) {
    var isLightTheme = false
        set(value) {
            subScene.fill = if (value) Color.WHITE else Color.BLACK
            field = value
        }

    private val root = subScene.root as Group
    private val spheresList = mutableListOf<Sphere>()
    private val trajectoryGroupsList = mutableListOf<Group>()
    private var pointLight: PointLight
    private var cameraDistance = 500.0
    private val trajectoryPointRadius = 0.8

    init {
        root.children.add(AmbientLight(Color.color(0.3, 0.3, 0.3)))
        pointLight = PointLight(Color.WHITE).also { root.children.add(it) }
        subScene.setOnScroll { event ->
            cameraDistance = max(100.0, min(2000.0, cameraDistance - event.deltaY))
        }
    }

    private fun updateCameraPosition(bodies: List<Body>) {
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        for (body in bodies) {
            sumX += body.x
            sumY += body.y
            sumZ += body.z
        }
        val centerX = sumX / bodies.size
        val centerY = sumY / bodies.size
        val centerZ = sumZ / bodies.size

        camera.translateX = centerX
        camera.translateY = centerY
        camera.translateZ = -cameraDistance

        pointLight.translateX = centerX
        pointLight.translateY = centerY
        pointLight.translateZ = centerZ
    }

    private fun createOrUpdateTrajectory(body: Body, index: Int): Group {
        val trajectoryGroup = trajectoryGroupsList.getOrNull(index) ?: Group().also {
            root.children.add(it)
            trajectoryGroupsList.add(it)
        }

        trajectoryGroup.children.clear()

        for (point in body.trajectory) {
            val (x, y, z) = point

            val trajectoryPoint = Sphere(trajectoryPointRadius).apply {
                translateX = x
                translateY = y
                translateZ = z
                material = PhongMaterial().apply {
                    diffuseColor = body.color
                    specularColor = Color.TRANSPARENT
                }
            }

            trajectoryGroup.children.add(trajectoryPoint)
        }

        return trajectoryGroup
    }

    fun render3DFrame(bodies: List<Body>) {
        updateCameraPosition(bodies)

        var bodyIndex = 0
        while (bodyIndex < bodies.size) {
            val body = bodies[bodyIndex]
            val spheres = spheresList.getOrNull(bodyIndex) ?: Sphere(bodyRadius).apply {
                root.children.add(this)
                material = PhongMaterial().apply {
                    diffuseColor = body.color
                    specularColor = Color.WHITE
                }
            }.also { spheresList.add(it) }

            spheres.apply {
                translateX = body.x
                translateY = body.y
                translateZ = body.z
            }

            createOrUpdateTrajectory(body, bodyIndex)

            bodyIndex++
        }

        for (sphereIndex in spheresList.size - 1 downTo bodyIndex) {
            spheresList.removeAt(sphereIndex).let { sphere ->
                root.children.remove(sphere)
            }
            trajectoryGroupsList.removeAt(sphereIndex).let { group ->
                root.children.remove(group)
            }
        }


    }
}