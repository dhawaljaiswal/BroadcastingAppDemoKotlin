package com.plattysoft.leonids.modifiers

import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

import com.plattysoft.leonids.Particle

class ScaleModifier @JvmOverloads constructor(private val mInitialValue: Float, private val mFinalValue: Float, private val mStartTime: Long, private val mEndTime: Long, private val mInterpolator: Interpolator = LinearInterpolator()) : ParticleModifier {
    private val mDuration: Long
    private val mValueIncrement: Float

    init {
        mDuration = mEndTime - mStartTime
        mValueIncrement = mFinalValue - mInitialValue
    }

    override fun apply(particle: Particle, miliseconds: Long) {
        if (miliseconds < mStartTime) {
            particle.mScale = mInitialValue
        } else if (miliseconds > mEndTime) {
            particle.mScale = mFinalValue
        } else {
            val interpolaterdValue = mInterpolator.getInterpolation((miliseconds - mStartTime) * 1f / mDuration)
            val newScale = mInitialValue + mValueIncrement * interpolaterdValue
            particle.mScale = newScale
        }
    }

}
