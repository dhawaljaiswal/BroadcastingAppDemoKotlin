package com.plattysoft.leonids.initializers

import com.plattysoft.leonids.Particle
import java.util.*

class RotationInitializer(private val mMinAngle: Int, private val mMaxAngle: Int) : ParticleInitializer {

    override fun initParticle(p: Particle, r: Random) {
        p.mInitialRotation = if (mMinAngle == mMaxAngle) mMinAngle.toFloat() else (r.nextInt(mMaxAngle - mMinAngle) + mMinAngle).toFloat()
    }

}
