package com.example.tracker2

import android.graphics.Bitmap


class Particle(val robot: RobotPosition, val map: GridMap) {
    fun errorVs(currentPosition: RobotPosition, groundline: ArrayList<Int>, converter: PixelConverter): Double {
        val testMap = GridMap(map.cellsPerMeter, map.metersPerSide)
        testMap.setFrom(currentPosition, groundline, converter)
        val groundlinePoints = testMap.numFilledCells()
        testMap.intersect(map)
        val intersectPoints = testMap.numFilledCells()
        return intersectPoints.toDouble() / groundlinePoints.toDouble()
    }

    fun copy() = Particle(robot, map.copy())
}

fun addUniformNoise(value: Double, rangeMin: Double, rangeMax: Double) =
    value + rangeMin + Math.random()*(rangeMax - rangeMin)

fun addUniformNoise(value: PolarCoord, rangeMin: PolarCoord, rangeMax: PolarCoord) =
    PolarCoord(addUniformNoise(value.r, rangeMin.r, rangeMax.r),
        addUniformNoise(value.theta, rangeMin.theta, rangeMax.theta))

class ParticleFilter(val numParticles: Int,
                     val noiseRangeMin: PolarCoord,
                     val noiseRangeMax: PolarCoord,
                     cellsPerMeter: Double,
                     val converter: PixelConverter) {

    val particles = ArrayList<Particle>()
    var best = blankParticle(cellsPerMeter)

    init {
        for (i in 0 until numParticles) {
            particles.add(blankParticle(cellsPerMeter))
        }
    }

    private fun blankParticle(cellsPerMeter: Double) = Particle(RobotPosition(0.0, 0.0, Heading(0)), GridMap(cellsPerMeter))

    fun addNewGroundline(motion: PolarCoord, groundline: ArrayList<Int>) {
        for (particle in particles) {
            particle.robot.updatedBy(addUniformNoise(motion, noiseRangeMin, noiseRangeMax))
            particle.map.setFrom(particle.robot, groundline, converter)
        }
    }

    fun iterate(motion: PolarCoord, groundline: ArrayList<Int>) {
        resample(motion, groundline)
        addNewGroundline(motion, groundline)
    }

    private fun resample(motion: PolarCoord, groundline: java.util.ArrayList<Int>) {
        val distribution = Distribution<Particle>()
        for (particle in particles) {
            distribution.add(particle, particle.errorVs(particle.robot.updatedBy(motion), groundline, converter))
        }
        best = distribution.highestWeightSample()
        particles.clear()
        for (i in 0 until numParticles) {
            particles.add(distribution.randomPick().copy())
        }
    }
}

class ParticleFilterClassifier(
    images: ArrayList<Bitmap>,
    k: Int,
    minNotFloor: Int,
    maxJump: Int,
    numParticles: Int,
    noiseRangeMin: PolarCoord,
    noiseRangeMax: PolarCoord,
    cellsPerMeter: Double,
    meter1: CalibrationLine,
    meter2: CalibrationLine
) : GroundlineKmeans(images, k, minNotFloor, maxJump, GroundlineValue.CENTER) {

    val filter = ParticleFilter(numParticles, noiseRangeMin, noiseRangeMax, cellsPerMeter, PixelConverter(meter1, meter2, images[0].width, images[0].height))
    val mapOverlayer = MapOverlayer(GridMap(cellsPerMeter))

    override fun classify(image: Bitmap) {
        val x2y = findFilteredGroundline(image)
        val best = highestPoint(x2y)
        overlayer.updateHeights(x2y, height, best.first)
        notifyListeners("heading ${best.first} ${best.second}")
        updateFilter(x2y)
    }

    fun updateFilter(groundline: ArrayList<Int>) {
        if (messagesWaiting()) {
            var heading = PolarCoord(0.0, 0.0)
            while (messagesWaiting()) {
                val message = retrieveMessage().split(" ")
                heading += PolarCoord(message[0].toDouble(), message[1].toDouble())
            }
            filter.iterate(heading, groundline)
            mapOverlayer.map = filter.best.map.copy()
            notifyListeners("pos ${filter.best.robot.x} ${filter.best.robot.y} ${filter.best.robot.heading.radians()}")
        }
    }

    override fun overlayers(): java.util.ArrayList<Overlayer> {
        return if (altDisplay) {
            arrayListOf(mapOverlayer)
        } else {
            super.overlayers()
        }
    }
}