package com.plattysoft.leonids.initializers

import com.plattysoft.leonids.Particle
import java.util.*

class RotationSpeedInitializer(private val mMinRotationSpeed: Float, private val mMaxRotationSpeed: Float) : ParticleInitializer {

    override fun initParticle(p: Particle, r: Random) {
        val rotationSpeed = r.nextFloat() * (mMaxRotationSpeed - mMinRotationSpeed) + mMinRotationSpeed
        p.mRotationSpeed = rotationSpeed
    }

}
