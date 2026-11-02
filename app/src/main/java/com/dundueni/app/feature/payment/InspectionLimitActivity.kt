package com.dundueni.app.feature.payment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dundueni.app.feature.floatingbutton.FloatingButtonManager
import com.dundueni.app.ui.component.InspectionLimitDialog
import com.dundueni.app.ui.theme.DundueniFETheme
import java.util.UUID

/** Transparent host for the same Compose popup from Activities and the floating service. */
class InspectionLimitActivity : ComponentActivity() {
    private val overlayOwner = "inspection-limit:${UUID.randomUUID()}"

    override fun onStart() {
        super.onStart()
        FloatingButtonManager.suspendForActivity(overlayOwner)
    }

    override fun onStop() {
        FloatingButtonManager.resumeForActivity(overlayOwner)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DundueniFETheme {
                InspectionLimitDialog(
                    onUpgrade = {
                        startActivity(PaymentActivity.createIntent(this@InspectionLimitActivity))
                        finish()
                    },
                    onLater = ::finish,
                )
            }
        }
    }
}
