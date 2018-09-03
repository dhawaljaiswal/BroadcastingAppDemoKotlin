package com.plattysoft.leonids

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.plattysoft.leonids.modifiers.ParticleModifier

open class Particle protected constructor() {

    protected lateinit var mImage: Bitmap

    var mCurrentX: Float = 0.toFloat()
    var mCurrentY: Float = 0.toFloat()

    var mScale = 1f
    var mAlpha = 255

    var mInitialRotation = 0f

    var mRotationSpeed = 0f

    var mSpeedX = 0f
    var mSpeedY = 0f

    var mAccelerationX: Float = 0.toFloat()
    var mAccelerationY: Float = 0.toFloat()

    private val mMatrix: Matrix
    private val mPaint: Paint

    private var mInitialX: Float = 0.toFloat()
    private var mInitialY: Float = 0.toFloat()

    private var mRotation: Float = 0.toFloat()

    private var mTimeToLive: Long = 0

    protected var mStartingMilisecond: Long = 0

    private var mBitmapHalfWidth: Int = 0
    private var mBitmapHalfHeight: Int = 0

    private var mModifiers: List<ParticleModifier>? = null


    init {
        mMatrix = Matrix()
        mPaint = Paint()
    }

    constructor(bitmap: Bitmap) : this() {
        mImage = bitmap
    }

    fun init() {
        mScale = 1f
        mAlpha = 255
    }

    fun configure(timeToLive: Long, emiterX: Float, emiterY: Float) {
        mBitmapHalfWidth = mImage.width / 2
        mBitmapHalfHeight = mImage.height / 2

        mInitialX = emiterX - mBitmapHalfWidth
        mInitialY = emiterY - mBitmapHalfHeight
        mCurrentX = mInitialX
        mCurrentY = mInitialY

        mTimeToLive = timeToLive
    }

    open fun update(miliseconds: Long): Boolean {
        val realMiliseconds = miliseconds - mStartingMilisecond
        if (realMiliseconds > mTimeToLive) {
            return false
        }
        mCurrentX = mInitialX + mSpeedX * realMiliseconds + mAccelerationX * realMiliseconds.toFloat() * realMiliseconds.toFloat()
        mCurrentY = mInitialY + mSpeedY * realMiliseconds + mAccelerationY * realMiliseconds.toFloat() * realMiliseconds.toFloat()
        mRotation = mInitialRotation + mRotationSpeed * realMiliseconds / 1000
        for (i in mModifiers!!.indices) {
            mModifiers!![i].apply(this, realMiliseconds)
        }
        return true
    }

    fun draw(c: Canvas) {
        mMatrix.reset()
        mMatrix.postRotate(mRotation, mBitmapHalfWidth.toFloat(), mBitmapHalfHeight.toFloat())
        mMatrix.postScale(mScale, mScale, mBitmapHalfWidth.toFloat(), mBitmapHalfHeight.toFloat())
        mMatrix.postTranslate(mCurrentX, mCurrentY)
        mPaint.alpha = mAlpha
        c.drawBitmap(mImage, mMatrix, mPaint)
    }

    fun activate(startingMilisecond: Long, modifiers: List<ParticleModifier>): Particle {
        mStartingMilisecond = startingMilisecond
        // We do store a reference to the list, there is no need to copy, since the modifiers do not carte about states
        mModifiers = modifiers
        return this
    }
}
