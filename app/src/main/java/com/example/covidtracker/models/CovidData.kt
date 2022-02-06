package com.example.covidtracker.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

data class CovidData(
    val dateChecked: Date,
    val positiveIncrease: Int,
    val negativeIncrease: Int,
    val deathIncrease: Int,
    val state: String,
) : Serializable
