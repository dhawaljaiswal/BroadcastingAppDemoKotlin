/*
 * Copyright (C) 2015 tyrantgit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tyrantgit.widget;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class AbstractPathAnimator {
    private final Random mRandom;
    protected final Config mConfig;

    public static int mInitX;

    public static int mInitY;

    public static int mXRand;

    public static int mAnimLengthRand;

    public static int mBezierFactor;

    public static int mXPointFactor;

    public static int mAnimLength;

    public static int mAnimDuration;


    public AbstractPathAnimator(Config config) {
        mConfig = config;
        mRandom = new Random();
    }

    public float randomRotation() {
        return mRandom.nextFloat() * 28.6F - 14.3F;
    }

    public Path createPath(AtomicInteger counter, View view, int factor) {
        Random r = mRandom;
        int x = r.nextInt(mConfig.xRand);
        int x2 = r.nextInt(mConfig.xRand);
        int y = view.getHeight() - mConfig.initY;
        int y2 = counter.intValue() * 15 + mConfig.animLength * factor + r.nextInt(mConfig.animLengthRand);
        factor = y2 / mConfig.bezierFactor;
        x = mConfig.xPointFactor + x;
        x2 = mConfig.xPointFactor + x2;
        int y3 = y - y2;
        y2 = y - y2 / 2;

        Path p = new Path();
        p.moveTo(mConfig.initX, y);
        p.cubicTo(mConfig.initX, y - factor, x, y2 + factor, x, y2);
        p.moveTo(x, y2);
        p.cubicTo(x, y2 - factor, x2, y3 + factor, x2, y3);
        return p;
       /* int x1, x2, y1, y2, y3;

        // y1 > y2 > y3
        // x1, x2 random
        x1 = mRandom.nextInt(mXRand * 2);
        x2 = mRandom.nextInt(mXRand);
        y1 = view.getHeight() - mInitY;
        y2 = counter.intValue() * 15 + mAnimLength * factor + mRandom.nextInt(mAnimLengthRand);
        factor = y2 / mBezierFactor;
        x1 = x1 + (mXRand - 50);
        x2 = mXPointFactor + x2;
        y3 = y1 - y2;
        y2 = y1 - y2 / 2;

        Path p = new Path();
        p.moveTo(x1, y1);
        System.out.println("mInitX " + mInitX);
        p.cubicTo(mInitX, y1, x1, y1 - 100, x2, y2);
        p.moveTo(x1, y2);
        p.cubicTo(x1, y2 - factor, x2, y3 + factor, x2, y3);

        return p;*/
    }

    private static void init(Resources resources, TypedArray array) {
        mInitX = (int) array.getDimension(R.styleable.HeartLayout_initX, resources.getDimensionPixelOffset(R.dimen.heart_anim_init_x));
        mInitY = (int) array.getDimension(R.styleable.HeartLayout_initY, resources.getDimensionPixelOffset(R.dimen.heart_anim_init_y));
        mXRand = (int) array.getDimension(R.styleable.HeartLayout_xRand, resources.getDimensionPixelOffset(R.dimen.heart_anim_bezier_x_rand));
        mAnimLength = (int) array.getDimension(R.styleable.HeartLayout_animLength, resources.getDimensionPixelOffset(R.dimen.heart_anim_length));
        mAnimLengthRand = (int) array.getDimension(R.styleable.HeartLayout_animLengthRand, resources.getDimensionPixelOffset(R.dimen.heart_anim_length_rand));
        mBezierFactor = array.getInteger(R.styleable.HeartLayout_bezierFactor, resources.getInteger(R.integer.heart_anim_bezier_factor));
        mXPointFactor = (int) array.getDimension(R.styleable.HeartLayout_xPointFactor, resources.getDimensionPixelOffset(R.dimen.heart_anim_x_point_factor));

        mAnimDuration = array.getInteger(R.styleable.HeartLayout_anim_duration, resources.getInteger(R.integer.anim_duration));
    }

    public abstract void start(View child, ViewGroup parent);

    public static class Config {
        public int initX;
        public int initY;
        public int xRand;
        public int animLengthRand;
        public int bezierFactor;
        public int xPointFactor;
        public int animLength;
        public int heartWidth;
        public int heartHeight;
        public int animDuration;

        static Config fromTypeArray(TypedArray typedArray) {

            Config config = new Config();
            Resources res = typedArray.getResources();

            init(res, typedArray);

            config.initX = (int) typedArray.getDimension(R.styleable.HeartLayout_initX,
                    res.getDimensionPixelOffset(R.dimen.heart_anim_init_x));
            config.initY = (int) typedArray.getDimension(R.styleable.HeartLayout_initY,
                    res.getDimensionPixelOffset(R.dimen.heart_anim_init_y));
            config.xRand = (int) typedArray.getDimension(R.styleable.HeartLayout_xRand,
                    res.getDimensionPixelOffset(R.dimen.heart_anim_bezier_x_rand));
            config.animLength = (int) typedArray.getDimension(R.styleable.HeartLayout_animLength,
                    res.getDimensionPixelOffset(R.dimen.heart_anim_length));
            config.animLengthRand = (int) typedArray.getDimension(R.styleable.HeartLayout_animLengthRand,
                    res.getDimensionPixelOffset(R.dimen.heart_anim_length_rand));
            config.bezierFactor = typedArray.getInteger(R.styleable.HeartLayout_bezierFactor,
                    res.getInteger(R.integer.heart_anim_bezier_factor));
            config.xPointFactor = (int) typedArray.getDimension(R.styleable.HeartLayout_xPointFactor,
                    res.getDimensionPixelOffset(R.dimen.heart_anim_x_point_factor));
            config.heartWidth = (int) typedArray.getDimension(R.styleable.HeartLayout_heart_width,
                    res.getDimensionPixelOffset(R.dimen.heart_size_width));
            config.heartHeight = (int) typedArray.getDimension(R.styleable.HeartLayout_heart_height,
                    res.getDimensionPixelOffset(R.dimen.heart_size_height));
            config.animDuration = typedArray.getInteger(R.styleable.HeartLayout_anim_duration,
                    res.getInteger(R.integer.anim_duration));
            return config;
        }


    }


}

