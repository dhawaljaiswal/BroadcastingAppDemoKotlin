package com.ratufa.livebroadcastingdemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.SeekBar
import android.widget.TextView
import com.bambuser.broadcaster.BroadcastPlayer
import com.bambuser.broadcaster.PlayerState
import com.bambuser.broadcaster.SurfaceViewWithAutoAR

class VideoPlayerActivity : AppCompatActivity() {

    private val mVolumeSeekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            updateVolume(seekBar.progress / seekBar.max.toFloat())
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    private val mPlayerObserver = object : BroadcastPlayer.Observer {
        override fun onStateChange(state: PlayerState) {
            if (mPlayerStatusTextView != null)
                mPlayerStatusTextView!!.text = "Status: $state"
            if (mBroadcastLiveTextView != null) {
                val live = (mBroadcastPlayer != null && mBroadcastPlayer!!.isTypeLive
                        && mBroadcastPlayer!!.isPlaying)
                mBroadcastLiveTextView!!.visibility = if (live) View.VISIBLE else View.GONE
            }
            if (state == PlayerState.PLAYING || state == PlayerState.PAUSED || state == PlayerState.COMPLETED) {
                if (mMediaController == null && mBroadcastPlayer != null && !mBroadcastPlayer!!.isTypeLive) {
                    mMediaController = MediaController(this@VideoPlayerActivity)
                    mMediaController!!.setAnchorView(mVideoSurfaceView)
                    mMediaController!!.setMediaPlayer(mBroadcastPlayer)
                }
                if (mMediaController != null) {
                    mMediaController!!.isEnabled = true
                    mMediaController!!.show()
                }
            } else if (state == PlayerState.ERROR || state == PlayerState.CLOSED) {
                if (mMediaController != null) {
                    mMediaController!!.isEnabled = false
                    mMediaController!!.hide()
                }
                mMediaController = null
                if (mViewerStatusTextView != null)
                    mViewerStatusTextView!!.text = ""
            }
        }

        override fun onBroadcastLoaded(live: Boolean, width: Int, height: Int) {
            if (mBroadcastLiveTextView != null)
                mBroadcastLiveTextView!!.visibility = if (live) View.VISIBLE else View.GONE
        }
    }

    private val mViewerCountObserver = object : BroadcastPlayer.ViewerCountObserver {
        override fun onCurrentViewersUpdated(viewers: Long) {
            if (mViewerStatusTextView != null)
                mViewerStatusTextView!!.text = "Viewers: $viewers"
        }

        override fun onTotalViewersUpdated(viewers: Long) {}
    }

    private var mBroadcastPlayer: BroadcastPlayer? = null
    private var mVideoSurfaceView: SurfaceViewWithAutoAR? = null
    private var mVolumeSeekBar: SeekBar? = null
    private var mPlayerStatusTextView: TextView? = null
    private var mViewerStatusTextView: TextView? = null
    private var mBroadcastLiveTextView: TextView? = null
    private var mMediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        mPlayerStatusTextView = findViewById(R.id.PlayerStatusTextView)
        mBroadcastLiveTextView = findViewById(R.id.BroadcastLiveTextView)
        mVideoSurfaceView = findViewById(R.id.VideoSurfaceView)
        mVolumeSeekBar = findViewById(R.id.PlayerVolumeSeekBar)
        mVolumeSeekBar!!.setOnSeekBarChangeListener(mVolumeSeekBarListener)
        mViewerStatusTextView = findViewById(R.id.ViewerStatusTextView)
    }

    override fun onResume() {
        super.onResume()
        //String resourceUri = "PLEASE INSERT A RESOURCE URI, FOR EXAMPLE RETRIEVED FROM IRIS METADATA API";
        val resourceUri = "https://cdn.bambuser.net/broadcasts/6ead3999-b030-4421-936f-9558f45588f9?da_signature_method=HMAC-SHA256&da_id=9e1b1e83-657d-7c83-b8e7-0b782ac9543a&da_timestamp=1535538907&da_static=1&da_ttl=0&da_signature=7a6de19b078a4ed3cf5dab993354d310ba220bc340c5d344287eed7fd88c03d3"
        //val resourceUri = "https://cdn.bambuser.net/broadcasts/d68b89f4-c75c-4046-9545-3badad9c1666?da_signature_method=HMAC-SHA256&da_id=9e1b1e83-657d-7c83-b8e7-0b782ac9543a&da_timestamp=1531139535&da_static=1&da_ttl=0&da_signature=9c71c9bf69499d3a3875d10d783c1b12176115422d39aed2a93023438a8f7f8a"
        //String resourceUri = "https://api.irisplatform.io/broadcasts/94599df6-cc5a-63c4-b6c3-3151d6ede722";
        mBroadcastPlayer = BroadcastPlayer(this, resourceUri, APPLICATION_ID, mPlayerObserver)
        mBroadcastPlayer!!.setSurfaceView(mVideoSurfaceView)
        mBroadcastPlayer!!.setAcceptType(BroadcastPlayer.AcceptType.ANY)
        mBroadcastPlayer!!.setViewerCountObserver(mViewerCountObserver)
        updateVolume(mVolumeSeekBar!!.progress / mVolumeSeekBar!!.max.toFloat())
        mBroadcastPlayer!!.load()
    }

    override fun onPause() {
        super.onPause()
        if (mBroadcastPlayer != null)
            mBroadcastPlayer!!.close()
        mBroadcastPlayer = null
        if (mMediaController != null)
            mMediaController!!.hide()
        mMediaController = null
        if (mBroadcastLiveTextView != null)
            mBroadcastLiveTextView!!.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about_menu_item -> {
                startActivity(Intent(this, AboutActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_UP && mBroadcastPlayer != null && mMediaController != null) {
            val state = mBroadcastPlayer!!.state
            if (state == PlayerState.PLAYING ||
                    state == PlayerState.BUFFERING ||
                    state == PlayerState.PAUSED ||
                    state == PlayerState.COMPLETED) {
                if (mMediaController!!.isShowing)
                    mMediaController!!.hide()
                else
                    mMediaController!!.show()
            } else {
                mMediaController!!.hide()
            }
        }
        return false
    }

    class AboutActivity : Activity() {
        public override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val webView = WebView(this)
            webView.loadUrl("file:///android_asset/licenses.html")
            // WebViewClient necessary since Android N to handle links in the license document
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return false
                }
            }
            setContentView(webView)
        }
    }

    private fun updateVolume(progress: Float) {
        // Output volume should optimally increase logarithmically, but Android media player APIs
        // respond linearly. Producing non-linear scaling between 0.0 and 1.0 by using x^4.
        // Not exactly logarithmic, but has the benefit of satisfying the end points exactly.
        if (mBroadcastPlayer != null)
            mBroadcastPlayer!!.setAudioVolume(progress * progress * progress * progress)
    }

    //Defined application id of iris account
    companion object {
        private val APPLICATION_ID = "pVASu4IxIEkAQp3BwbLS2g"
        //private val APPLICATION_ID = "i4y7TzEvae4Djl0pJNCI1g"
    }
}
