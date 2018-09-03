package com.ratufa.livebroadcastingdemo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Go to broadcasting video screen when click on button @btnLiveBroadcaster
        btnLiveBroadcaster.setOnClickListener {
            startActivity(Intent(this, LiveVideoActivity::class.java))
        }

        //Go to Video Player screen when click on button @btnVideoPlayer
        btnVideoPlayer.setOnClickListener {
            startActivity(Intent(this, VideoPlayerActivity::class.java))
        }
    }

}