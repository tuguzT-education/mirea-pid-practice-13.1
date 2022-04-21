package io.github.tuguzt.getusdrate

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
            context.startService(Intent(context, RateCheckService::class.java).apply {
                putExtra(ARG_START_RATE, startRate)
                putExtra(ARG_TARGET_RATE, targetRate)
            })
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, RateCheckService::class.java))
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
            Toast.makeText(applicationContext, "Max attempts count reached, stopping service", Toast.LENGTH_LONG).show()
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
                Toast.makeText(applicationContext, "Rate = $rate", Toast.LENGTH_LONG).show()
                sendNotification(rate)
                stopSelf()
            } else {
                handler.postDelayed(rateCheckRunnable, RATE_CHECK_INTERVAL)
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

    private fun sendNotification(rate: String) {
        createNotificationChannel()
        // Write your code here
    }

    private fun createNotificationChannel() {
        // Write your code here
    }
}
