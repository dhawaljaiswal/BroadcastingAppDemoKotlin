package com.ratufa.livebroadcastingdemo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.bambuser.broadcaster.*
import com.plattysoft.leonids.ParticleSystem
import com.ratufa.livebroadcastingdemo.adapters.GiftAdapter
import com.ratufa.livebroadcastingdemo.listeners.RecyclerItemClickListener
import com.ratufa.livebroadcastingdemo.models.Gift
import com.ratufa.livebroadcastingdemo.utils.BillingManager
import com.ratufa.livebroadcastingdemo.utils.UploadHelper
import com.wunderlist.slidinglayer.LayerTransformer
import com.wunderlist.slidinglayer.SlidingLayer
import com.wunderlist.slidinglayer.transformer.AlphaTransformer
import com.wunderlist.slidinglayer.transformer.RotationTransformer
import com.wunderlist.slidinglayer.transformer.SlideJoyTransformer
import kotlinx.android.synthetic.main.activity_live_video.*
import tyrantgit.widget.HeartLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LiveVideoActivity : AppCompatActivity(), View.OnClickListener, Broadcaster.Observer, Broadcaster.TalkbackObserver, Broadcaster.UplinkSpeedObserver, UploadHelper.ProgressCallback, Broadcaster.ViewerCountObserver, BillingManager.BillingUpdatesListener {

    private var billingManager: BillingManager? = null

    private val mRandom = Random()
    private val mTimer = Timer()
    private var mHeartLayout: HeartLayout? = null
    private var mSlidingLayer: SlidingLayer? = null
    private val swipeText: TextView? = null

    private var giftList: MutableList<Gift>? = null
    private var giftAdapter: GiftAdapter? = null
    private var rlGiftLayout: RecyclerView? = null

    private val storageDir: File?
        get() {
            if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) && Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val externalDir = File(Environment.getExternalStorageDirectory(), "LibBambuser")
                externalDir.mkdirs()
                if (externalDir.exists() && externalDir.canWrite())
                    return externalDir
            }
            return null
        }

    private var mInPermissionRequest = false
    private var mDefaultDisplay: Display? = null
    private var mOrientationListener: OrientationEventListener? = null
    private var mBroadcaster: Broadcaster? = null
    private var mBroadcastButton: Button? = null
    private var mSwitchButton: Button? = null
    private var mUploadButton: Button? = null
    //private var openDialog: Button? = null
    private var buttonClose: Button? = null
    private var mTalkbackStatus: TextView? = null
    private var mViewerStatus: TextView? = null
    private var mTalkbackStopButton: Button? = null
    private var mBambuserChatController: ChatController? = null
    private var mExampleChatController: ExampleChatController? = null
    private var mUploadDialog: AlertDialog? = null
    private var mLastUploadStatusUpdateTime: Long = 0
    private var mUploading = false


    private val PRODUCT_ID = "android.test.purchased"

    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)
        mInPermissionRequest = savedInstanceState?.getBoolean(STATE_IN_PERMISSION_REQUEST) ?: false

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_live_video)

        mHeartLayout = findViewById<View>(R.id.heart_layout) as HeartLayout
        mTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                mHeartLayout!!.post { mHeartLayout!!.addHeart(randomColor()) }
            }
        }, 500, 200)

        mDefaultDisplay = windowManager.defaultDisplay
        mOrientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (mBroadcaster != null && mBroadcaster!!.canStartBroadcasting())
                    mBroadcaster!!.setRotation(mDefaultDisplay!!.rotation)
            }
        }

        mSlidingLayer = findViewById<View>(R.id.slidingLayer1) as SlidingLayer
        //swipeText = (TextView) findViewById(R.id.swipeText);

        setupPreviewMode(true)
        setupSlidingLayerPosition("right")
        setupSlidingLayerTransform("none")

        setupShadow(false)
        setupLayerOffset(false)

        mBroadcastButton = findViewById(R.id.BroadcastButton)
        mBroadcastButton!!.setOnClickListener(this)
        mSwitchButton = findViewById(R.id.SwitchCameraButton)
        mSwitchButton!!.setOnClickListener(this)
        //openDialog = findViewById(R.id.openDialog)
        //openDialog!!.setOnClickListener(this)

        buttonClose = findViewById(R.id.buttonClose)
        buttonClose!!.setOnClickListener(this)

        mTalkbackStopButton = findViewById(R.id.TalkbackStopButton)
        mTalkbackStopButton!!.setOnClickListener(this)
        mViewerStatus = findViewById(R.id.ViewerStatus)
        mTalkbackStatus = findViewById(R.id.TalkbackStatus)
        mBroadcaster = Broadcaster(this, APPLICATION_ID, this)
        mBroadcaster!!.setRotation(mDefaultDisplay!!.rotation)
        mBroadcaster!!.setTalkbackObserver(this)
        mBroadcaster!!.setUplinkSpeedObserver(this)
        mBroadcaster!!.setViewerCountObserver(this)
        if (mBroadcaster!!.cameraCount <= 1)
            mSwitchButton!!.visibility = View.INVISIBLE
        if (!mInPermissionRequest) {
            val missingPermissions = ArrayList<String>()
            if (!hasPermission(Manifest.permission.CAMERA))
                missingPermissions.add(Manifest.permission.CAMERA)
            if (!hasPermission(Manifest.permission.RECORD_AUDIO))
                missingPermissions.add(Manifest.permission.RECORD_AUDIO)
            if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (missingPermissions.size > 0)
                requestPermissions(missingPermissions, START_PERMISSIONS_CODE)
        }

        imgGiftCard.setOnClickListener(this)

        // Choose either a ChatController with Bambuser style auto-show&hide or a simpler ExampleChatController.
        mBambuserChatController = mBroadcaster!!.createChatController(findViewById<ListView>(R.id.ChatListView), R.layout.chatline, R.id.chat_line_textview, R.id.chat_line_timeview)
        // mExampleChatController = new ExampleChatController(this, findViewById(R.id.ChatListView), R.layout.chatline, R.id.chat_line_textview, R.id.chat_line_timeview);

        dataLoad()
        rlGiftLayout = findViewById<View>(R.id.rlGiftLayout) as RecyclerView
        // set a GridLayoutManager with default vertical orientation and 2 number of columns
        //GridLayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(),4);
        // set a GridLayoutManager with 3 number of columns , horizontal gravity and false value for reverseLayout to show the items from start to end
        val gridLayoutManager = GridLayoutManager(applicationContext, 4, GridLayoutManager.VERTICAL, false)

        //LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        //rlGiftLayout.setLayoutManager(layoutManager); // set LayoutManager to RecyclerView
        rlGiftLayout!!.layoutManager = gridLayoutManager // set LayoutManager to RecyclerView
        //  call the constructor of CustomAdapter to send the reference and data to Adapter
        giftAdapter = GiftAdapter(this, this!!.giftList!!)
        rlGiftLayout!!.adapter = giftAdapter // set the Adapter to RecyclerView

        rlGiftLayout?.addOnItemTouchListener(RecyclerItemClickListener(this, object : RecyclerItemClickListener.OnItemClickListener{
            override fun onItemClick(view: View, position: Int) {
                val gift : Gift = giftList!!.get(position)
                //startPurchaseFlow(PRODUCT_ID, BillingClient.SkuType.INAPP)
                startPurchaseFlow(gift.giftType, BillingClient.SkuType.INAPP)
            }
        }))

        //In APP Purchase BillingManager call
        billingManager = BillingManager(this, this)

        //New Message send code
        imgSend.setOnClickListener {
            if (etMessage.text.isNotEmpty()) {
                onChatMessage(etMessage.text.toString())
                etMessage.text.clear()
            }
        }

        //fire animation
        btnFire.setOnClickListener{
            background_hook.performClick().apply {
                /*ParticleSystem(this@LiveVideoActivity, 20, R.drawable.ic_action_heart_full, 3000)
                        .setSpeedByComponentsRange(-0.1f, 0.1f, -0.1f, 0.02f)
                        .setAcceleration(0.000003f, 90)
                        .setInitialRotationRange(0, 360)
                        .setRotationSpeed(120f)
                        .setFadeOut(2000)
                        .addModifier(ScaleModifier(0f, 1.5f, 0, 1500))
                        .oneShot(background_hook, 15)*/

                val ps = ParticleSystem(this@LiveVideoActivity, 100, R.drawable.ic_action_heart, 800)
                ps.setScaleRange(0.7f, 1.3f)
                ps.setSpeedRange(0.1f, 0.25f)
                ps.setRotationSpeedRange(90f, 180f)
                ps.setFadeOut(200, AccelerateInterpolator())
                ps.oneShot(background_hook, 15)

                val ps2 = ParticleSystem(this@LiveVideoActivity, 100, R.drawable.ic_action_heart_full, 800)
                ps2.setScaleRange(0.7f, 1.3f)
                ps2.setSpeedRange(0.1f, 0.25f)
                ps.setRotationSpeedRange(90f, 180f)
                ps2.setFadeOut(200, AccelerateInterpolator())
                ps2.oneShot(background_hook, 15)

            }
        }
    }

    /**
     * Override In App Purchase required method start
     */
    override fun onPurchaseUpdated(purchases: List<Purchase>, responseCode: Int) {
        Log.i(TAG, "onPurchaseUpdated, responseCode = $responseCode, size of purchases = ${purchases.size}")
        displayMsgForPurchasesCheck(purchases)
    }

    override fun onConsumeFinished(responseCode: Int, token: String?) {
        Log.i(TAG, "onConsumePurchase Successful > BillingResponseCode = $responseCode, token = $token")
        showToast("Thank You for Donating!\nYou may consider donating a few more times to see consumption of In-app purchases in action")
    }

    override fun onQueryPurchasesFinished(purchases: List<Purchase>) {
        Log.i(TAG, "onQueryPurchasesFinished, size of verified Purchases = ${purchases.size}")
        displayMsgForPurchasesCheck(purchases)
    }

    fun startPurchaseFlow(skuId: String, @BillingClient.SkuType skuType: String) {
        billingManager?.launchPurchaseFlow(skuId, skuType)
    }

    private fun displayMsgForPurchasesCheck(purchases: List<Purchase>) {
        for (purchase in purchases) {
            Log.d(TAG, "Nkp purchase : $purchase")

            if ((giftList!![0].giftType).equals(purchase.sku)) {
                billingManager?.consumePurchase(purchase)
            } else {
                //adView?.visibility = View.GONE
                showToast("Thank You for Purchase! Ads have been Eliminated!")
            }
        }
    }

    /**
     * Override In App Purchase required method end
     */

    private fun showToast(msg: String, length: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, msg, length).show()
    }

    //Add gift detail add in @giftList
    private fun dataLoad() {
        giftList = ArrayList()
        val gift1 = Gift("Teddy", R.drawable.gift1, 1)
        giftList!!.add(gift1)

        val gift2 = Gift("Gift two", R.drawable.gift2, 6)
        giftList!!.add(gift2)

        val gift3 = Gift("Gift Three", R.drawable.gift3, 0)
        giftList!!.add(gift3)

        val gift4 = Gift("Gift Four", R.drawable.gift4, 1)
        giftList!!.add(gift4)

        val gift5 = Gift("Gift Five", R.drawable.gift5, 4)
        giftList!!.add(gift5)

        val gift6 = Gift("Gift Six", R.drawable.gift6, 2)
        giftList!!.add(gift6)

        val gift7 = Gift("Gift Seven", R.drawable.gift7, 5)
        giftList!!.add(gift7)

        val gift8 = Gift("Gift Eight", R.drawable.gift8, 1)
        giftList!!.add(gift8)

        val gift9 = Gift("Gift Nine", R.drawable.gift9, 3)
        giftList!!.add(gift9)

        val gift10 = Gift("Gift Ten", R.drawable.gift10, 2)
        giftList!!.add(gift10)

    }

    /**
     * Sliding overlap layout for showing gift start
     */
    private fun setupPreviewMode(enabled: Boolean) {
        val previewOffset = if (enabled) resources.getDimensionPixelOffset(R.dimen.preview_offset_distance) else -1
        mSlidingLayer!!.setPreviewOffsetDistance(previewOffset)
    }

    private fun setupSlidingLayerTransform(layerTransform: String) {

        val transformer: LayerTransformer

        when (layerTransform) {
            "alpha" -> transformer = AlphaTransformer()
            "rotation" -> transformer = RotationTransformer()
            "slide" -> transformer = SlideJoyTransformer()
            else -> return
        }
        mSlidingLayer!!.setLayerTransformer(transformer)
    }

    private fun setupShadow(enabled: Boolean) {
        if (enabled) {
            mSlidingLayer!!.setShadowSizeRes(R.dimen.shadow_size)
        } else {
            mSlidingLayer!!.shadowSize = 0
            mSlidingLayer!!.setShadowDrawable(null)
        }
    }

    private fun setupLayerOffset(enabled: Boolean) {
        val offsetDistance = if (enabled) resources.getDimensionPixelOffset(R.dimen.offset_distance) else 0
        mSlidingLayer!!.offsetDistance = offsetDistance
    }

    private fun setupSlidingLayerPosition(layerPosition: String) {

        val rlp = mSlidingLayer!!.layoutParams as RelativeLayout.LayoutParams
        val textResource: Int
        val d: Drawable

        when (layerPosition) {
            "right" -> {
                textResource = R.string.swipe_right_label
                //d = getResources().getDrawable(R.drawable.container_rocket_right);

                mSlidingLayer!!.setStickTo(SlidingLayer.STICK_TO_RIGHT)
            }
            "left" -> {
                textResource = R.string.swipe_left_label
                //d = getResources().getDrawable(R.drawable.container_rocket_left);

                mSlidingLayer!!.setStickTo(SlidingLayer.STICK_TO_LEFT)
            }
            "top" -> {
                textResource = R.string.swipe_up_label
                // d = getResources().getDrawable(R.drawable.container_rocket);

                mSlidingLayer!!.setStickTo(SlidingLayer.STICK_TO_TOP)
                rlp.width = RelativeLayout.LayoutParams.MATCH_PARENT
                rlp.height = resources.getDimensionPixelSize(R.dimen.layer_size)
            }
            else -> {
                textResource = R.string.swipe_down_label
                // d = getResources().getDrawable(R.drawable.container_rocket);

                mSlidingLayer!!.setStickTo(SlidingLayer.STICK_TO_BOTTOM)
                rlp.width = RelativeLayout.LayoutParams.MATCH_PARENT
                rlp.height = resources.getDimensionPixelSize(R.dimen.layer_size)
            }
        }

        // d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        //swipeText.setCompoundDrawables(null, d, null, null);
        //swipeText.setText(getResources().getString(textResource));
        mSlidingLayer!!.layoutParams = rlp
    }

    /**
     * Sliding overlap layout for showing gift start
     */

    private fun randomColor(): Int {
        return Color.rgb(mRandom.nextInt(255), mRandom.nextInt(255), mRandom.nextInt(255))
    }

    public override fun onDestroy() {
        super.onDestroy()
        billingManager?.destroyBillingClient()

        mTimer.cancel()
        mBroadcaster!!.onActivityDestroy()
        if (mBambuserChatController != null)
            mBambuserChatController!!.destroy()
        mBambuserChatController = null
        mExampleChatController = null
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(STATE_IN_PERMISSION_REQUEST, mInPermissionRequest)
    }

    public override fun onPause() {
        super.onPause()
        mOrientationListener!!.disable()
        mBroadcaster!!.onActivityPause()
    }

    public override fun onResume() {
        super.onResume()
        mOrientationListener!!.enable()
        mBroadcaster!!.setCameraSurface(findViewById<SurfaceView>(R.id.PreviewSurfaceView))
        mBroadcaster!!.setRotation(mDefaultDisplay!!.rotation)
        mBroadcaster!!.onActivityResume()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.BroadcastButton -> if (mBroadcaster!!.canStartBroadcasting()) {
                if (hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.RECORD_AUDIO)) {
                    lockCurrentOrientation()
                    initLocalRecording()
                    mBroadcaster!!.startBroadcast()
                } else {
                    val permissions = ArrayList<String>()
                    if (!hasPermission(Manifest.permission.CAMERA))
                        permissions.add(Manifest.permission.CAMERA)
                    if (!hasPermission(Manifest.permission.RECORD_AUDIO))
                        permissions.add(Manifest.permission.RECORD_AUDIO)
                    if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permissions, BROADCAST_PERMISSIONS_CODE)
                }
            } else
                mBroadcaster!!.stopBroadcast()

            R.id.SwitchCameraButton -> mBroadcaster!!.switchCamera()
            R.id.TalkbackStopButton -> mBroadcaster!!.stopTalkback()
            R.id.buttonClose -> mSlidingLayer!!.closeLayer(true)
            R.id.imgGiftCard -> mSlidingLayer!!.openLayer(true)
        }
    }

    private fun requestPermissions(missingPermissions: List<String>, code: Int) {
        mInPermissionRequest = true
        val permissions = missingPermissions.toTypedArray<String>()
        try {
            javaClass.getMethod("requestPermissions", Array<String>::class.java, Integer.TYPE).invoke(this, permissions, code)
        } catch (ignored: Exception) {
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        mInPermissionRequest = false
        if (requestCode == START_PERMISSIONS_CODE) {
            if (!hasPermission(Manifest.permission.CAMERA) || !hasPermission(Manifest.permission.RECORD_AUDIO))
                Toast.makeText(applicationContext, "Missing permission to camera or audio", Toast.LENGTH_SHORT).show()
        } else if (requestCode == BROADCAST_PERMISSIONS_CODE) {
            if (hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.RECORD_AUDIO)) {
                lockCurrentOrientation()
                initLocalRecording()
                mBroadcaster!!.startBroadcast()
            } else
                Toast.makeText(applicationContext, "Missing permission to camera or audio", Toast.LENGTH_SHORT).show()
        } else if (requestCode == PHOTO_PERMISSIONS_CODE) {
            if (!hasPermission(Manifest.permission.CAMERA) || !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                Toast.makeText(applicationContext, "Missing permission to camera or storage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateDialog(id: Int, args: Bundle): Dialog? {
        if (id == TALKBACK_DIALOG) {
            return AlertDialog.Builder(this).setTitle("Talkback request pending")
                    .setCancelable(false)
                    .setNegativeButton("Reject") { dialog, which -> mBroadcaster!!.stopTalkback() }
                    .setPositiveButton("Accept", null)
                    .setMessage("Incoming talkback call")
                    .create()
        } else if (id == UPLOAD_PROGRESS_DIALOG) {
            mUploadDialog = AlertDialog.Builder(this).setTitle("Uploading")
                    .setView(layoutInflater.inflate(R.layout.upload_progress_dialog, null))
                    .setCancelable(false)
                    .setNegativeButton("Cancel") { dialog, which -> mUploading = false }
                    .create()
            return mUploadDialog
        }
        return null
    }

    override fun onPrepareDialog(id: Int, dialog: Dialog, args: Bundle) {
        if (id == TALKBACK_DIALOG) {
            val caller = args.getString(TALKBACK_DIALOG_CALLER)
            val request = args.getString(TALKBACK_DIALOG_REQUEST)
            val sessionId = args.getInt(TALKBACK_DIALOG_SESSION_ID)
            val ad = dialog as AlertDialog
            ad.setButton(DialogInterface.BUTTON_POSITIVE, "Accept") { d, which -> mBroadcaster!!.acceptTalkback(sessionId) }
            var msg = "Incoming talkback call"
            if (caller != null && caller.length > 0)
                msg += " from: $caller"
            if (request != null && request.length > 0)
                msg += ": $request"
            msg += "\nPlease plug in your headphones and accept, or reject the call."
            ad.setMessage(msg)
        } else if (id == UPLOAD_PROGRESS_DIALOG) {
            (dialog.findViewById<View>(R.id.UploadProgressBar) as ProgressBar).progress = 0
            (dialog.findViewById<View>(R.id.UploadStatusText) as TextView).text = "Connecting..."
        }
        super.onPrepareDialog(id, dialog)
    }

    private fun takePhoto() {
        val missingPermissions = ArrayList<String>()
        if (!hasPermission(Manifest.permission.CAMERA))
            missingPermissions.add(Manifest.permission.CAMERA)
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (missingPermissions.size > 0) {
            requestPermissions(missingPermissions, PHOTO_PERMISSIONS_CODE)
            return
        }
        val resolutions = mBroadcaster!!.supportedPictureResolutions
        if (resolutions.isEmpty())
            return
        val maxRes = resolutions[resolutions.size - 1]
        val observer = Broadcaster.PictureObserver { file ->
            Toast.makeText(applicationContext, "Stored " + file.name, Toast.LENGTH_SHORT).show()
            MediaScannerConnection.scanFile(applicationContext, arrayOf(file.absolutePath), null, null)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS", Locale.US)
        val fileName = sdf.format(Date()) + ".jpg"
        val storageDir = storageDir
        if (storageDir == null) {
            Toast.makeText(applicationContext, "Can't store picture, external storage unavailable", Toast.LENGTH_LONG).show()
            return
        }
        val file = File(storageDir, fileName)
        mBroadcaster!!.takePicture(file, maxRes, observer)
    }

    private fun initLocalRecording() {
        if (mBroadcaster == null || !mBroadcaster!!.hasLocalMediaCapability())
            return
        val storageDir = storageDir
        if (storageDir == null) {
            Toast.makeText(applicationContext, "Can't store local copy, external storage unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val fileName = sdf.format(Date()) + ".mp4"
        val file = File(storageDir, fileName)
        val obs = object : Broadcaster.LocalMediaObserver {
            override fun onLocalMediaError() {
                Toast.makeText(applicationContext, "Failed to write to video file. Storage/memory full?", Toast.LENGTH_LONG).show()
            }

            override fun onLocalMediaClosed(filePath: String?) {
                if (filePath != null && filePath.endsWith(".mp4")) {
                    Toast.makeText(applicationContext, "Local copy of broadcast stored", Toast.LENGTH_SHORT).show()
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(filePath), null, null)
                }
            }
        }
        val success = mBroadcaster!!.storeLocalMedia(file, obs)
        Toast.makeText(applicationContext, "Writing to " + file.absolutePath + if (success) "" else " failed", Toast.LENGTH_SHORT).show()
    }

    private fun hasPermission(permission: String): Boolean {
        try {
            val result = javaClass.getMethod("checkSelfPermission", String::class.java).invoke(this, permission) as Int
            return result == PackageManager.PERMISSION_GRANTED
        } catch (ignored: Exception) {
        }

        return true
    }

    private fun lockCurrentOrientation() {
        val displayRotation = windowManager.defaultDisplay.rotation
        val configOrientation = resources.configuration.orientation
        val screenOrientation = getScreenOrientation(displayRotation, configOrientation)
        requestedOrientation = screenOrientation
    }


    //About activity about content load show on webview
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

    /**
     * Broadcasting video override methods start
     */

    override fun onCameraPreviewStateChanged() {

    }
    override fun onConnectionStatusChange(status: BroadcastStatus) {
        if (status == BroadcastStatus.STARTING)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (status == BroadcastStatus.IDLE)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (status == BroadcastStatus.IDLE)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (status == BroadcastStatus.IDLE)
            mViewerStatus!!.text = ""
        mBroadcastButton!!.text = if (status == BroadcastStatus.IDLE) "Broadcast" else "Disconnect"
        mSwitchButton!!.isEnabled = status == BroadcastStatus.IDLE
    }

    override fun onStreamHealthUpdate(health: Int) {}

    override fun onConnectionError(type: ConnectionError, message: String?) {
        var str = type.toString()
        if (message != null)
            str += " $message"
        Toast.makeText(applicationContext, str, Toast.LENGTH_LONG).show()
    }

    override fun onCameraError(error: CameraError) {
        Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_LONG).show()
    }

    override fun onChatMessage(message: String) {
        if (mBambuserChatController != null)
            mBambuserChatController!!.add(message)
        if (mExampleChatController != null) {
            mExampleChatController!!.add(message)
            mExampleChatController!!.show()
        }
    }

    override fun onResolutionsScanned() {
        // invoking setResolution() in this callback at every camera change, to possibly switch to a higher resolution.
        mBroadcaster!!.setResolution(0, 0)
    }

    override fun onBroadcastInfoAvailable(videoId: String, url: String) {
        Toast.makeText(applicationContext, "Broadcast with id $videoId published", Toast.LENGTH_LONG).show()
    }

    override fun onBroadcastIdAvailable(broadcastId: String) {}

    override fun onTalkbackStateChanged(state: TalkbackState, id: Int, caller: String, request: String) {
        try {
            removeDialog(TALKBACK_DIALOG)
        } catch (ignored: Exception) {
        }

        when (state) {
            TalkbackState.IDLE -> {
                mTalkbackStopButton!!.visibility = View.GONE
                mTalkbackStatus!!.text = ""
            }
            TalkbackState.NEEDS_ACCEPT -> {
                mTalkbackStatus!!.text = "talkback pending"
                val args = Bundle()
                args.putInt(TALKBACK_DIALOG_SESSION_ID, id)
                args.putString(TALKBACK_DIALOG_CALLER, caller)
                args.putString(TALKBACK_DIALOG_REQUEST, request)
                showDialog(TALKBACK_DIALOG, args)
            }
            TalkbackState.ACCEPTED, TalkbackState.READY -> {
                mTalkbackStopButton!!.visibility = View.VISIBLE
                mTalkbackStatus!!.text = "talkback connecting"
            }
            TalkbackState.PLAYING -> mTalkbackStatus!!.text = "talkback active"
        }
    }

    override fun onUplinkTestComplete(bitrate: Long, recommendation: Boolean) {
        val toast = "Uplink test complete, bandwidth: " + bitrate / 1024 + " kbps, broadcasting recommended: " + recommendation
        Toast.makeText(applicationContext, toast, Toast.LENGTH_LONG).show()
    }

    override fun onProgress(currentBytes: Long, totalBytes: Long): Boolean {
        if (currentBytes == totalBytes || System.currentTimeMillis() > mLastUploadStatusUpdateTime + 500) {
            mLastUploadStatusUpdateTime = System.currentTimeMillis()
            runOnUiThread(Runnable {
                if (mUploadDialog == null)
                    return@Runnable
                val permille = (currentBytes * 1000 / totalBytes).toInt()
                (mUploadDialog!!.findViewById<View>(R.id.UploadProgressBar) as ProgressBar).progress = permille
                val status = "Sent " + currentBytes / 1024 + " KB / " + totalBytes / 1024 + " KB"
                (mUploadDialog!!.findViewById<View>(R.id.UploadStatusText) as TextView).text = status
            })
        }
        return mUploading
    }

    override fun onSuccess(fileName: String?) {
        runOnUiThread {
            Toast.makeText(applicationContext, "Upload of $fileName completed", Toast.LENGTH_SHORT).show()
            mUploadDialog = null
            try {
                removeDialog(UPLOAD_PROGRESS_DIALOG)
            } catch (ignored: Exception) {
            }

            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            mUploadDialog = null
            try {
                removeDialog(UPLOAD_PROGRESS_DIALOG)
            } catch (ignored: Exception) {
            }

            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onCurrentViewersUpdated(viewers: Long) {
        mViewerStatus!!.text = "Viewers: $viewers"
    }

    override fun onTotalViewersUpdated(viewers: Long) {}

    /**
     * Broadcasting video override methods end
     */

    // static constant variable defined
    companion object {

        val TAG = "MainActivity"

        private val FILE_CHOOSER_CODE = 1
        private val START_PERMISSIONS_CODE = 2
        private val BROADCAST_PERMISSIONS_CODE = 3
        private val PHOTO_PERMISSIONS_CODE = 4
        private val TALKBACK_DIALOG = 1
        private val UPLOAD_PROGRESS_DIALOG = 2
        private val TALKBACK_DIALOG_CALLER = "caller"
        private val TALKBACK_DIALOG_REQUEST = "request"
        private val TALKBACK_DIALOG_SESSION_ID = "session_id"
        private val STATE_IN_PERMISSION_REQUEST = "in_permission_request"

        private val APPLICATION_ID = "pVASu4IxIEkAQp3BwbLS2g"//"i4y7TzEvae4Djl0pJNCI1g";

        private fun getScreenOrientation(displayRotation: Int, configOrientation: Int): Int {
            return if (configOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_90)
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            } else {
                if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_270)
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            }
        }
    }

}
