package com.dundueni.app.feature.payment

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ResetInspectionQuotaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESET_INSPECTION_QUOTA) return
        val reset = BasicInspectionQuota(context).resetForDebug()
        resultCode = if (reset) Activity.RESULT_OK else Activity.RESULT_CANCELED
    }

    companion object {
        const val ACTION_RESET_INSPECTION_QUOTA = "com.dundueni.app.action.RESET_INSPECTION_QUOTA"
    }
}
