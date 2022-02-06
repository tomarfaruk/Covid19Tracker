package com.example.covidtracker

import android.graphics.RectF
import com.example.covidtracker.models.CovidData
import com.example.covidtracker.utils.Metric
import com.example.covidtracker.utils.TimeScale
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<CovidData>) : SparkAdapter() {
    var metric = Metric.POSITIVE
    var timeScale =TimeScale.MAX
    override fun getCount() = dailyData.size

    override fun getItem(index: Int): CovidData {
        return  dailyData[index]
    }

    override fun getY(index: Int): Float {
        val chosenItem = dailyData[index]
        return  when(metric){
            Metric.POSITIVE->chosenItem.positiveIncrease.toFloat()
            Metric.NEGATIVE->chosenItem.negativeIncrease.toFloat()
            Metric.DEATH->chosenItem.deathIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if(timeScale!=TimeScale.MAX){
            bounds.left = count-timeScale.num.toFloat()
        }
        return bounds
    }

}
