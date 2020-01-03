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
        const val FILTER_DATA_KEY = "filterData"
    }

    private var mFilterData : FilterData = FilterData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)
        configureSeekBarChangeListener()
        this.configureApplyFiltersButton()
        this.configureOnlyTDSTestedWaterMachineSwitch()
        this.configureSafeWaterMachineSwitch()
    }

    private fun configureSafeWaterMachineSwitch() {
        this.safeWaterMachineSwitch.setOnCheckedChangeListener { _, isChecked ->
            this.mFilterData.onlySafeWaterMachine = isChecked
        }
    }

    private fun configureOnlyTDSTestedWaterMachineSwitch() {
        this.onlyTDSTestedWaterMachineSwitch.setOnCheckedChangeListener { _, isChecked ->
            this.mFilterData.onlyTDSTestedWaterMachine = isChecked
        }
    }

    private fun configureSeekBarChangeListener() {
        distanceSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int,
                                           fromUser: Boolean) {
                // write custom code for progress is changed
                updateTitleFromValue(progress)
                mFilterData.distance = maxOf(progress * 50, MINIMUM_DISTANCE)
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

    private fun updateTitleFromValue(seekBarValue : Int) {
        // TODO internationalization here
        val meters : Int = maxOf(seekBarValue * 50, MINIMUM_DISTANCE)
        val titleText = "Within $meters meters"
        titleTextView.text = titleText
    }

    private fun sendFilterDataBackToPreviousActivity() {
        val intent = Intent().apply {
            putExtra(FILTER_DATA_KEY, mFilterData)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
