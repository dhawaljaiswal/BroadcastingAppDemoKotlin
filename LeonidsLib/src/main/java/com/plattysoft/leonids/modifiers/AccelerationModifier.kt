package com.plattysoft.leonids.modifiers

import com.plattysoft.leonids.Particle

class AccelerationModifier(velocity: Float, angle: Float) : ParticleModifier {

    private val mVelocityX: Float
    private val mVelocityY: Float

    init {
        val velocityAngleInRads = (angle * Math.PI / 180f).toFloat()
        mVelocityX = (velocity * Math.cos(velocityAngleInRads.toDouble())).toFloat()
        mVelocityY = (velocity * Math.sin(velocityAngleInRads.toDouble())).toFloat()
    }

    override fun apply(particle: Particle, miliseconds: Long) {
        particle.mCurrentX = particle.mCurrentX + mVelocityX * miliseconds.toFloat() * miliseconds.toFloat()
        particle.mCurrentY = particle.mCurrentY + mVelocityY * miliseconds.toFloat() * miliseconds.toFloat()
    }

}
