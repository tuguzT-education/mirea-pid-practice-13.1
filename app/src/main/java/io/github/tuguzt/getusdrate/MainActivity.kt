package io.github.tuguzt.getusdrate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import io.github.tuguzt.getusdrate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val textRate = binding.textUsdRubRate
        val textTargetRate = binding.textTargetRate

        binding.btnRefresh.setOnClickListener { viewModel.refreshRate() }

        binding.btnSubscribeToRate.setOnClickListener {
            val targetRate = textTargetRate.text.toString()
            val startRate = viewModel.usdRate.value

            when {
                targetRate.isNotEmpty() && startRate?.isNotEmpty() == true -> {
                    RateCheckService.stopService(this)
                    RateCheckService.startService(this, startRate, targetRate)
                }
                targetRate.isEmpty() -> {
                    Snackbar.make(binding.root, R.string.target_rate_empty, Snackbar.LENGTH_SHORT).show()
                }
                startRate.isNullOrEmpty() -> {
                    Snackbar.make(binding.root, R.string.current_rate_empty, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.usdRate.observe(this) {
            textRate.text = getString(R.string.rub_count, it)
        }
        viewModel.refreshRate()
    }
}
