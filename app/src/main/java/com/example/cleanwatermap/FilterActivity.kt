package com.example.cleanwatermap

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_filter.*


class FilterActivity : AppCompatActivity() {

    /*
        UI Names
        applyFiltersButton
        safeWaterMachineSwitch
        onlyTDSTestedWaterMachineSwitch
        distanceSeekBar
        titleTextView
     */

    companion object {
        const val MINIMUM_DISTANCE = 50
        const val MAXIMUM_SEEK_BAR_VALUE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)
        configureSeekBarChangeListener()
        this.configureApplyFiltersButton()
        this.configureOnlyTDSTestedWaterMachineSwitch()
        this.configureSafeWaterMachineSwitch()
        this.updateUIFromFilter()
    }

    private fun updateUIFromFilter() {
        if (Settings.userSpecifiedFilter) {
            distanceSeekBar.progress = this.seekBarValueFromFilterDistance(Settings.filterDistance)
            onlyTDSTestedWaterMachineSwitch.isChecked = Settings.onlyTDSTestedWaterMachine
            safeWaterMachineSwitch.isChecked = Settings.onlySafeWaterMachine
            updateTitleFromSeekBarValue(distanceSeekBar.progress)
        }
    }

    private fun configureSafeWaterMachineSwitch() {
        this.safeWaterMachineSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.onlySafeWaterMachine = isChecked
        }
    }

    private fun configureOnlyTDSTestedWaterMachineSwitch() {
        this.onlyTDSTestedWaterMachineSwitch.setOnCheckedChangeListener { _, isChecked ->
            Settings.onlyTDSTestedWaterMachine = isChecked
        }
    }

    private fun configureSeekBarChangeListener() {
        distanceSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int,
                                           fromUser: Boolean) {
                // write custom code for progress is changed
                updateTitleFromSeekBarValue(progress)
                if (progress >= MAXIMUM_SEEK_BAR_VALUE) {
                    Settings.resetDistanceFilter()
                }
                else {
                    Settings.filterDistance = maxOf(progress * 50, MINIMUM_DISTANCE)
                }
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
            }
        })
    }

    private fun configureApplyFiltersButton() {
        applyFiltersButton.setOnClickListener {
            this.sendFilterDataBackToPreviousActivity()
        }
    }

    private fun seekBarValueFromFilterDistance(filterDistance : Int) : Int {
        assert(filterDistance >= 0)
        return maxOf(filterDistance / 50, MINIMUM_DISTANCE / 50)
    }

    private fun filterDistanceFromSeekBarValue(seekBarValue : Int) : Int {
        return maxOf(seekBarValue * 50, MINIMUM_DISTANCE)
    }

    private fun updateTitleFromSeekBarValue(seekBarValue : Int) {
        // TODO internationalization here
        if (seekBarValue >= MAXIMUM_SEEK_BAR_VALUE) {
            titleTextView.text = "No filter distance"
        }
        else {
            val meters : Int = this.filterDistanceFromSeekBarValue(seekBarValue)
            if (meters >= 1000) {
                val km : Double = (meters.toDouble() / 1000.0)
                val extraS : String = if (meters >= 2000) "s" else ""
                titleTextView.text = "Within %.1f km${extraS}".format(km)
            }
            else {
                titleTextView.text = "Within $meters meters"
            }

        }
    }

    private fun sendFilterDataBackToPreviousActivity() {
        setResult(Activity.RESULT_OK, Intent())
        finish()
    }
}
