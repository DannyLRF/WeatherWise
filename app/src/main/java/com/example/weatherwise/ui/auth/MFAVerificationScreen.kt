package com.example.weatherwise.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.weatherwise.R
import com.example.weatherwise.ui.theme.StyledTextField
import com.example.weatherwise.ui.theme.WeatherWiseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MFAVerificationScreen(onVerifyCode: (String) -> Unit, onResendCode: () -> Unit) {
    var verificationCode by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.lock),
            contentDescription = stringResource(R.string.MFA),
            modifier = Modifier
                .size(250.dp)
        )//        Text(stringResource(R.string.mfa_verification), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.verification_code))
            StyledTextField(
                value = verificationCode,
                onValueChange = { verificationCode = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onVerifyCode(verificationCode) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.verify))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onResendCode,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.resend_code))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MFAVerificationScreenPreview() {
    WeatherWiseTheme(darkTheme = true) {
        MFAVerificationScreen(onVerifyCode = {},
            onResendCode = {})
    }
}