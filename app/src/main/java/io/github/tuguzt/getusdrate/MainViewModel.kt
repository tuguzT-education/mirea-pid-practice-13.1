package io.github.tuguzt.getusdrate

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    companion object {
        const val TAG = "MainViewModel"
        const val USD_RATE_URL = "https://www.freeforexapi.com/api/live?pairs=USDRUB"
    }

    private val _usdRate = MutableLiveData<String>()
    val usdRate: LiveData<String> get() = _usdRate

    private val rateCheckInteractor = RateCheckInteractor()

    fun refreshRate() {
        viewModelScope.launch {
            val rate = rateCheckInteractor.requestRate()
            Log.d(TAG, "usdRate = $rate")
            _usdRate.value = rate
        }
    }
}
