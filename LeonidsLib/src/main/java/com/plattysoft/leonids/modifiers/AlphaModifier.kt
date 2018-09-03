package com.plattysoft.leonids.modifiers

import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

import com.plattysoft.leonids.Particle

class AlphaModifier @JvmOverloads constructor(private val mInitialValue: Int, private val mFinalValue: Int, private val mStartTime: Long, private val mEndTime: Long, private val mInterpolator: Interpolator = LinearInterpolator()) : ParticleModifier {
    private val mDuration: Float
    private val mValueIncrement: Float

    init {
        mDuration = (mEndTime - mStartTime).toFloat()
        mValueIncrement = (mFinalValue - mInitialValue).toFloat()
    }

    override fun apply(particle: Particle, miliseconds: Long) {
        if (miliseconds < mStartTime) {
            particle.mAlpha = mInitialValue
        } else if (miliseconds > mEndTime) {
            particle.mAlpha = mFinalValue
        } else {
            val interpolaterdValue = mInterpolator.getInterpolation((miliseconds - mStartTime) * 1f / mDuration)
            val newAlphaValue = (mInitialValue + mValueIncrement * interpolaterdValue).toInt()
            particle.mAlpha = newAlphaValue
        }
    }

}
