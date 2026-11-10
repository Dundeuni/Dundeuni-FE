package com.dundueni.app.feature.analysis

/** Main-thread bookkeeping retained with the inspection ViewModel. */
class InspectionCompletionDelivery {
    private val delivered = mutableSetOf<String>()
    fun claim(id: String): Boolean = delivered.add(id)
}
