package io.github.tuguzt.getusdrate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class RateCheckService : Service() {
    companion object {
        private const val TAG = "RateCheckService"
        private const val NOTIFICATION_CHANNEL_ID = "usd_rate"
        private const val RATE_CHECK_INTERVAL = 5000L
        private const val RATE_CHECK_ATTEMPTS_MAX = 100

        private const val ARG_START_RATE = "ARG_START_RATE"
        private const val ARG_TARGET_RATE = "ARG_TARGET_RATE"

        fun startService(context: Context, startRate: String, targetRate: String) {
            val startIntent = Intent(context, RateCheckService::class.java).apply {
                putExtra(ARG_START_RATE, startRate)
                putExtra(ARG_TARGET_RATE, targetRate)
            }
            context.startService(startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, RateCheckService::class.java)
            context.stopService(stopIntent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var rateCheckAttempt = 0
    private lateinit var startRate: BigDecimal
    private lateinit var targetRate: BigDecimal
    private val rateCheckInteractor = RateCheckInteractor()

    private val rateCheckRunnable = Runnable {
        rateCheckAttempt++

        Log.d(TAG, "rateCheckAttempt = $rateCheckAttempt")
        if (rateCheckAttempt > RATE_CHECK_ATTEMPTS_MAX) {
            sendNotification()
            stopSelf()
            Log.d(TAG, "Max attempts count reached, stopping service")
            return@Runnable
        }
        requestAndCheckRate()
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private fun requestAndCheckRate() {
        coroutineScope.launch {
            val rate = rateCheckInteractor.requestRate()
            Log.d(TAG, "new rate = $rate")
            val rateBigDecimal = BigDecimal(rate)
            if (
                targetRate in rateBigDecimal..startRate
                || (startRate < targetRate && rateBigDecimal >= targetRate)
            ) {
                sendNotification(rate)
                stopSelf()
            } else {
                handler.postDelayed(rateCheckRunnable, RATE_CHECK_INTERVAL)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        startRate = BigDecimal(intent?.getStringExtra(ARG_START_RATE))
        targetRate = BigDecimal(intent?.getStringExtra(ARG_TARGET_RATE))

        Log.d(TAG, "onStartCommand startRate = $startRate targetRate = $targetRate")

        handler.post(rateCheckRunnable)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(rateCheckRunnable)
    }

    private fun sendNotification(rate: String? = null) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val flag = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> PendingIntent.FLAG_IMMUTABLE
            else -> 0
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flag)

        val title = when (rate) {
            null -> "Max attempts count reached, stopping service"
            else -> "Current USD rate is $rate RUB"
        }
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Channel"
            val channelDescription = "Channel Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = channelDescription
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
