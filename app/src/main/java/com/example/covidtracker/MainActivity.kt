package com.example.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.covidtracker.models.CovidData
import com.example.covidtracker.services.CovidService
import com.example.covidtracker.utils.BASE_URL
import com.example.covidtracker.utils.Metric
import com.example.covidtracker.utils.TimeScale
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import org.angmarch.views.NiceSpinner
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShowData: List<CovidData>
    private lateinit var sparkAdapter: CovidSparkAdapter
    private lateinit var stateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalData: List<CovidData>
    private lateinit var tvState: TextView
    private lateinit var tvDateView: TextView
    private lateinit var tvMatrixLabel: TickerView
    private lateinit var rbNegative: RadioButton
    private lateinit var rbPositive: RadioButton
    private lateinit var rbDeath: RadioButton
    private lateinit var rbWeek: RadioButton
    private lateinit var rbMonth: RadioButton
    private lateinit var rbMax: RadioButton
    private lateinit var sparkView: SparkView
    private lateinit var rgStatus: RadioGroup
    private lateinit var rgTime: RadioGroup
    private lateinit var spinnerSelect: NiceSpinner

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMatrixLabel = findViewById(R.id.tvMatrixLabel)
        tvDateView = findViewById(R.id.tvDateView)
        tvState = findViewById(R.id.tvState)
        rbMax = findViewById(R.id.rbMax)
        rbMonth = findViewById(R.id.rbMonth)
        rbWeek = findViewById(R.id.rbWeek)
        rbDeath = findViewById(R.id.rbDeath)
        rbPositive = findViewById(R.id.rbPositive)
        rbNegative = findViewById(R.id.rbNegative)
        sparkView = findViewById(R.id.sparkView)
        rgTime = findViewById(R.id.rgTime)
        rgStatus = findViewById(R.id.rgStatus)
        spinnerSelect = findViewById(R.id.spinnerSelect)


        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

        val service = retrofit.create(CovidService::class.java)

        service.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse ${response}")
                val body = response.body()
                if (body == null) {
                    Log.i(TAG, "response body empty")
                    return
                }
                setUpEnvironmentEvent()
                nationalData = body.reversed()
                updateDisplayData(nationalData)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure ${t.localizedMessage}")
            }
        })

        //state api call
        service.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, " serviceState onResponse ${response}")
                val body = response.body()
                if (body == null) {
                    Log.i(TAG, "serviceState response body empty")
                    return
                }
                stateDailyData = body.reversed().groupBy { it.state }
                updateStateDailyData(stateDailyData.keys)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "serviceState onFailure ${t.localizedMessage}")
            }
        })
    }

    private fun updateStateDailyData(stateName: Set<String>) {
        val stateAbbreviationList = stateName.toMutableList()
        stateAbbreviationList.sorted()
        stateAbbreviationList.add(0, "ALL")

        spinnerSelect.attachDataSource(stateAbbreviationList)

        spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            val selectedDataList = stateDailyData[selectedItem] ?: nationalData
            updateDisplayData(selectedDataList)
        }
    }

    private fun setUpEnvironmentEvent() {
        tvMatrixLabel.setCharacterLists(TickerUtils.provideNumberList())
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener {
            if (it is CovidData) {
                updateInfoForDate(it)
            }
        }

        rgTime.setOnCheckedChangeListener { _, checkId ->
            sparkAdapter.timeScale = when (checkId) {
                R.id.rbWeek -> TimeScale.WEEK
                R.id.rbMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            sparkAdapter.notifyDataSetChanged()
        }
        rgStatus.setOnCheckedChangeListener { _, checkId ->
            when (checkId) {
                R.id.rbPositive -> updateMetricInfo(Metric.POSITIVE)
                R.id.rbNegative -> updateMetricInfo(Metric.NEGATIVE)
                else -> updateMetricInfo(Metric.DEATH)
            }
        }
    }

    private fun updateMetricInfo(metric: Metric) {
        val color = when (metric) {
            Metric.POSITIVE -> R.color.colorPositive
            Metric.NEGATIVE -> R.color.colorNegative
            else -> R.color.colorDeath
        }
        val colorInt = ContextCompat.getColor(this, color)
        sparkView.lineColor = colorInt

        tvMatrixLabel.setTextColor(colorInt)

        sparkAdapter.metric = metric
        sparkAdapter.notifyDataSetChanged()

        updateInfoForDate(currentlyShowData.last())
    }

    private fun updateDisplayData(dailyData: List<CovidData>) {
        currentlyShowData = dailyData
        sparkAdapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = sparkAdapter

        rbPositive.isChecked = true
        rbMax.isChecked = true

        updateMetricInfo(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCase = when (sparkAdapter.metric) {
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        tvMatrixLabel.text = NumberFormat.getInstance().format(numCase)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateView.text = dateFormat.format(covidData.dateChecked)
    }
}