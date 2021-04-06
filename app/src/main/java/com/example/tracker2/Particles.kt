package com.example.tracker2



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

fun addUniformNoise(value: Double, noiseRange: Double) = value - noiseRange/2.0 + Math.random()*noiseRange

fun addUniformNoise(value: PolarCoord, noiseRange: PolarCoord) =
    PolarCoord(addUniformNoise(value.r, noiseRange.r),
        addUniformNoise(value.theta, noiseRange.theta))

class ParticleFilter(var particles: ArrayList<Particle>,
                     val numParticles: Int,
                     val motionNoiseRange: PolarCoord,
                     val cellsPerMeter: Double,
                     val converter: PixelConverter) {
    init {
        for (i in 0 until numParticles) {
            particles.add(Particle(RobotPosition(0.0, 0.0, Heading(0)), GridMap(cellsPerMeter)))
        }
    }

    fun addNewGroundline(motion: PolarCoord, groundline: ArrayList<Int>) {
        for (particle in particles) {
            particle.robot.updatedBy(addUniformNoise(motion, motionNoiseRange))
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
        particles.clear()
        for (i in 0 until numParticles) {
            particles.add(distribution.randomPick().copy())
        }
    }
}