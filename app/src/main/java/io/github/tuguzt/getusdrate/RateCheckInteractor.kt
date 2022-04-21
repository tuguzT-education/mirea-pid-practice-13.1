package io.github.tuguzt.getusdrate

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RateCheckInteractor {
    private val networkClient = NetworkClient()

    suspend fun requestRate(): String = withContext(Dispatchers.IO) {
        val result = networkClient.request(MainViewModel.USD_RATE_URL)
        if (!result.isNullOrEmpty()) parseRate(result) else ""
    }

    private fun parseRate(jsonString: String): String = try {
        JSONObject(jsonString)
            .getJSONObject("rates")
            .getJSONObject("USDRUB")
            .getString("rate")
    } catch (e: Exception) {
        Log.e("RateCheckInteractor", "", e)
        ""
    }
}
