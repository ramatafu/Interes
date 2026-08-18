package com.interes.shared.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Плавающее прозрачное окно с одной фотографией поверх других приложений.
 *
 * Работает только через WindowManager.TYPE_APPLICATION_OVERLAY — это НЕ
 * прозрачность обычного окна Activity (у Activity такого понятия, как
 * "плавает поверх других приложений", нет). Пользователь должен один раз
 * выдать разрешение "Отображение поверх других приложений" — без него
 * addView() выбросит исключение (это проверяется до вызова start(), см.
 * TransparentWindowLauncher.android.kt).
 *
 * Живёт как foreground-сервис (обязательно на Android 8+ для долгоживущих
 * фоновых задач), поэтому показывает несворачиваемое уведомление, пока
 * окно открыто — это требование системы, не наша прихоть.
 */
class PhotoOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val lifecycleOwner = OverlayLifecycleOwner()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val photoPath = intent?.getStringExtra(EXTRA_PHOTO_PATH)
        if (photoPath == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        showOverlay(photoPath)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay(photoPath: String) {
        if (overlayView != null) return // уже показано (например, второй Intent)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT // без этого фон окна будет чёрным, а не прозрачным
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 120
            y = 300
        }
        layoutParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                OverlayPhotoContent(
                    photoPath = photoPath,
                    onDrag = { dx, dy ->
                        // Важно: bare "layoutParams" здесь резолвился бы в
                        // View.layoutParams (ViewGroup.LayoutParams — у него
                        // нет полей x/y) из-за неявного this от apply{} на
                        // ComposeView чуть выше. Нужно явно указать наше
                        // собственное поле класса сервиса.
                        val p = this@PhotoOverlayService.layoutParams ?: return@OverlayPhotoContent
                        p.x += dx.toInt()
                        p.y += dy.toInt()
                        windowManager.updateViewLayout(this, p)
                    },
                    onClose = { stopSelf() }
                )
            }
        }
        overlayView = view
        windowManager.addView(view, params)
    }

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Прозрачное окно Interes",
            NotificationManager.IMPORTANCE_MIN // минимальная важность — без звука и всплытия
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val closeIntent = PendingIntent.getService(
            this, 0,
            Intent(this, PhotoOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Interes")
            .setContentText("Плавающее окно с фото открыто")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Закрыть", closeIntent)
            .build()
    }

    companion object {
        const val EXTRA_PHOTO_PATH = "extra_photo_path"
        private const val ACTION_STOP = "com.interes.overlay.STOP"
        private const val CHANNEL_ID = "interes_overlay"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context, photoPath: String) {
            val intent = Intent(context, PhotoOverlayService::class.java)
                .putExtra(EXTRA_PHOTO_PATH, photoPath)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PhotoOverlayService::class.java))
        }
    }
}
