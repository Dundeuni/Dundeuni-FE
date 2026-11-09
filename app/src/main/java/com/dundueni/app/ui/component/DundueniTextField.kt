package com.dundueni.app.ui.component

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dundueni.app.ui.theme.Danger
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.SoftBlue
import com.dundueni.app.ui.theme.SoftDanger

@Composable
fun dundueniOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = SoftBlue,
    disabledContainerColor = SoftBlue,
    errorContainerColor = SoftDanger,
    focusedBorderColor = ProtectiveBlue,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = Danger,
    cursorColor = ProtectiveBlue,
)
