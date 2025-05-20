package com.example.weatherwise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.weatherwise.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()

    val units = listOf("Celsius", "Fahrenheit")
    val windSpeeds = listOf("KM/h", "MPH", "M/s")
    val pressures = listOf("mbar", "inHG", "hPa")
    val about = listOf("Terms of Service", "Privacy Notice", "Share Feedback")

    val aboutRoutes = listOf("terms", "privacy", "feedback")




    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Settings",
                fontSize = 32.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 8.dp)
            )
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Unit
            item {
                Text("Unit", fontSize = 24.sp, color = Color.White)
            }
            items(units.size) { i ->
                SettingOptionItem(
                    title = units[i],
                    selected = settings.unit == units[i],
                    onClick = { viewModel.updateUnit(units[i]) }
                )
            }

            // Wind Speed
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Wind Speed", fontSize = 24.sp, color = Color.White)
            }
            items(windSpeeds.size) { i ->
                SettingOptionItem(
                    title = windSpeeds[i],
                    selected = settings.windSpeed == windSpeeds[i],
                    onClick = { viewModel.updateWindSpeed(windSpeeds[i]) }
                )
            }

            // Pressure
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Pressure", fontSize = 24.sp, color = Color.White)
            }
            items(pressures.size) { i ->
                SettingOptionItem(
                    title = pressures[i],
                    selected = settings.pressure == pressures[i],
                    onClick = { viewModel.updatePressure(pressures[i]) }
                )
            }

            // Alerts
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Alerts", fontSize = 24.sp, color = Color.White)
            }
            item {
                SettingSwitchItem(
                    title = "Weather Notifications",
                    description = "Get notified about daily weather changes",
                    checked = settings.weatherNotifications,
                    onCheckedChange = { viewModel.setWeatherNotifications(it) }
                )
            }
            item {
                SettingSwitchItem(
                    title = "Weather Warnings",
                    description = "Get notified about extreme conditions",
                    checked = settings.weatherWarnings,
                    onCheckedChange = { viewModel.setWeatherWarnings(it) }
                )
            }

            // About
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "About",
                    fontSize = 24.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(about.size) { index ->
                val title = about[index]
                val route = aboutRoutes[index]

                Surface(
                    color = Color.DarkGray,
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("about/$route")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = title, color = Color.White)
                    }
                }
            }

            // Logout
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Log out", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingOptionItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = Color.DarkGray,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White)
            RadioButton(
                selected = selected,
                onClick = { onClick() },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.White,
                    unselectedColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Color.DarkGray,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController, title: String) {
    val termprivacy = """
      You have the following rights concerning your data:

Right to Access: You can request information about the personal data we hold about you.
Right to Rectification: You have the right to request correction of inaccurate personal data we have about you.
Right to Erasure: You can request that we delete your personal data.
Right to Restrict Processing: You have the right to request the restriction of processing your personal data.
Right to Data Portability: You can request a copy of your personal data in a structured, commonly used, and machine-readable format.
Right to Object: You have the right to object to the processing of your personal data.
To exercise any of these rights, please contact us using the information provided below.  
    """.trimIndent()

    val privacynotice = """
        Security of Your Information
        We use administrative, technical, and physical security measures to help protect your personal information. 
        While we have taken reasonable steps to secure the personal information you provide to us, please be aware that despite our efforts, no security measures are perfect or impenetrable, and no method of data transmission can be guaranteed against any interception or other type of misuse.
    """.trimIndent()

    val FEEDBACKTEXT = """
We’d love to hear from you!

Please email us at group@student.monash.edu.com.

Thank you for helping us improve!
""".trimIndent()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = title,
                fontSize = 32.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 8.dp)
            )
        }

        /*
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "This is the $title screen.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        */

        Spacer(modifier = Modifier.height(24.dp))

        val content = when (title) {
            "Terms of Service" -> termprivacy
            "Privacy Notice" -> privacynotice
            "Share Feedback" -> FEEDBACKTEXT
            else -> "No content available."
        }

        Text(
            text = content,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp
        )
    }

}
