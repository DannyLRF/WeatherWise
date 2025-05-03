package com.example.weatherwise.ui.screens

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
import androidx.compose.material3.TextButton
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
import com.example.weatherwise.ui.theme.PasswordInput
import com.example.weatherwise.ui.theme.StyledTextField
import com.example.weatherwise.ui.theme.WeatherWiseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.sun),
            contentDescription = stringResource(R.string.login),
            modifier = Modifier
                .size(250.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.email))
            StyledTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.password))
            PasswordInput(
                password = password,
                onPasswordChange = { password = it },
                modifier = Modifier.fillMaxWidth()
            )        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onLoginClick(email, password) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(R.string.login))
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(R.string.register))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onForgotPasswordClick) {
            Text(stringResource(R.string.forgot_password))
        }
    }
}



@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    WeatherWiseTheme(darkTheme = true) { // 👈 Force dark mode for preview
        LoginScreen(
            onLoginClick = { _, _ -> },
            onGoogleSignInClick = {},
            onForgotPasswordClick = {}
        )
    }
}





