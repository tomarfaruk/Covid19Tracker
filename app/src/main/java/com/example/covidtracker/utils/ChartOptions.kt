package com.example.covidtracker.utils

enum class Metric{
    NEGATIVE,POSITIVE,DEATH
}
enum class TimeScale(val num:Int){
    WEEK(7),
    MONTH(30),
    MAX(-1),
}