package com.plattysoft.leonids.initializers

import com.plattysoft.leonids.Particle
import java.util.*

class SpeedModuleAndRangeInitializer(private val mSpeedMin: Float, private val mSpeedMax: Float, minAngle: Int, maxAngle: Int) : ParticleInitializer {
    private var mMinAngle: Int = 0
    private var mMaxAngle: Int = 0

    init {
        mMinAngle = minAngle
        mMaxAngle = maxAngle
        // Make sure the angles are in the [0-360) range
        while (mMinAngle < 0) {
            mMinAngle += 360
        }
        while (mMaxAngle < 0) {
            mMaxAngle += 360
        }
        // Also make sure that mMinAngle is the smaller
        if (mMinAngle > mMaxAngle) {
            val tmp = mMinAngle
            mMinAngle = mMaxAngle
            mMaxAngle = tmp
        }
    }

    override fun initParticle(p: Particle, r: Random) {
        val speed = r.nextFloat() * (mSpeedMax - mSpeedMin) + mSpeedMin
        val angle: Int
        if (mMaxAngle == mMinAngle) {
            angle = mMinAngle
        } else {
            angle = r.nextInt(mMaxAngle - mMinAngle) + mMinAngle
        }
        val angleInRads = Math.toRadians(angle.toDouble())
        p.mSpeedX = (speed * Math.cos(angleInRads)).toFloat()
        p.mSpeedY = (speed * Math.sin(angleInRads)).toFloat()
        p.mInitialRotation = (angle + 90).toFloat()
    }

}
