package com.example.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.VoxDeckApp
import com.example.data.db.SoundCardEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isCollapsed = false

    private var padsContainer: LinearLayout? = null
    private var bodyContainer: LinearLayout? = null
    private var collapseBtn: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP_ALL_AUDIO -> {
                (application as VoxDeckApp).container.soundEngine.stopAll()
                return START_STICKY
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val container = (application as VoxDeckApp).container
        container.soundEngine.setOverlayRunning(true)

        if (overlayView == null) {
            attachOverlayWindow()
            observeCards()
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachOverlayWindow() {
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EC0D1321"))
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.parseColor("#6622D3EE"))
            }
        }

        // Draggable Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(2), dp(6))
        }

        val titleView = TextView(this).apply {
            text = "VoxDeck"
            setTextColor(Color.parseColor("#22D3EE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val toggleBtn = TextView(this).apply {
            text = "—"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            minimumWidth = dp(34)
            minimumHeight = dp(34)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener {
                isCollapsed = !isCollapsed
                bodyContainer?.visibility = if (isCollapsed) View.GONE else View.VISIBLE
                text = if (isCollapsed) "+" else "—"
            }
        }
        collapseBtn = toggleBtn

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FCA5A5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            minimumWidth = dp(34)
            minimumHeight = dp(34)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3B1624"))
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener { stopSelf() }
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(6), dp(1))
        }

        header.addView(titleView)
        header.addView(toggleBtn)
        header.addView(spacer)
        header.addView(closeBtn)

        // Body Container
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        bodyContainer = body

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                dp(250),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, dp(8))
        }
        padsContainer = row
        scroll.addView(row)

        val stopAllBtn = TextView(this).apply {
            text = "■ ALL STOP"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F43F5E"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                (application as VoxDeckApp).container.soundEngine.stopAll()
            }
        }

        body.addView(scroll)
        body.addView(stopAllBtn)

        root.addView(header)
        root.addView(body)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(20)
            y = dp(160)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(root, params)
                    true
                }
                else -> false
            }
        }

        overlayView = root
        layoutParams = params
        windowManager.addView(root, params)
    }

    private fun observeCards() {
        val container = (application as VoxDeckApp).container
        scope.launch {
            combine(
                container.repository.cards,
                container.soundEngine.playingCardId,
                container.repository.voiceProfiles
            ) { cards, playingId, voices ->
                Triple(cards.take(10), playingId, voices.firstOrNull())
            }.collect { (cards, playingId, defaultVoice) ->
                renderPadChips(cards, playingId) { card ->
                    container.soundEngine.toggleCard(card, defaultVoice)
                }
            }
        }
    }

    private fun renderPadChips(
        cards: List<SoundCardEntity>,
        playingId: Long?,
        onTap: (SoundCardEntity) -> Unit
    ) {
        val row = padsContainer ?: return
        row.removeAllViews()
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        if (cards.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No cards"
                setTextColor(Color.parseColor("#94A3B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(8), dp(6), dp(8), dp(6))
            }
            row.addView(empty)
            return
        }

        cards.forEach { card ->
            val isPlaying = card.id == playingId
            val chip = TextView(this).apply {
                text = if (isPlaying) "▶ ${card.title}" else card.title
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                maxWidth = dp(120)
                setTextColor(if (isPlaying) Color.parseColor("#090D16") else Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                paint.isFakeBoldText = isPlaying
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = GradientDrawable().apply {
                    setColor(
                        if (isPlaying) Color.parseColor("#22D3EE")
                        else Color.parseColor("#1E293B")
                    )
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.parseColor("#334155"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(6)
                }
                setOnClickListener { onTap(card) }
            }
            row.addView(chip)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopAudioIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_ALL_AUDIO
        }
        val stopAudioPending = PendingIntent.getService(
            this,
            1,
            stopAudioIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeOverlayIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val closeOverlayPending = PendingIntent.getService(
            this,
            2,
            closeOverlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(openPending)
            .setOngoing(true)
            .addAction(0, getString(R.string.action_stop_all), stopAudioPending)
            .addAction(0, "Close", closeOverlayPending)
            .build()
    }

    override fun onDestroy() {
        (application as VoxDeckApp).container.soundEngine.setOverlayRunning(false)
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Throwable) {
            }
        }
        overlayView = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "voxdeck_overlay_channel"
        private const val NOTIFICATION_ID = 9001
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"
        const val ACTION_STOP_ALL_AUDIO = "com.example.action.STOP_ALL_AUDIO"
    }
}
