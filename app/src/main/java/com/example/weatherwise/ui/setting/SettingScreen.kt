package com.example.weatherwise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Updated import for ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.platform.LocalContext // Not directly used in this snippet for logic
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.weatherwise.viewmodel.SettingsViewModel // ViewModel for app settings (units, etc.)
import com.example.weatherwise.ui.auth.AuthViewModel // ViewModel for authentication

@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = viewModel(), // Specific ViewModel for UI settings like units
    authViewModel: AuthViewModel,               // Passed in for auth actions
    navigateToMfaSetup: () -> Unit          // Callback to navigate to MFA setup
) {
    val settings by settingsViewModel.settings.collectAsState()

    val units = listOf("Celsius", "Fahrenheit")
    val windSpeeds = listOf("KM/h", "MPH", "M/s")
    val pressures = listOf("mbar", "inHG", "hPa")
    val about = listOf("Terms of Service", "Privacy Notice", "Share Feedback")
    // Ensure your NavHost in WeatherAppNavigation has "about_app/{page_type}"
    val aboutRoutes = listOf("Terms of Service", "Privacy Notice", "Share Feedback") // Match text or use keys


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Assuming a dark theme from your layout
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 8.dp) // Added bottom padding
        ) {
            IconButton(
                onClick = { navController.navigateUp() }, // Navigates up within WeatherAppNavigation
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Use AutoMirrored
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Settings",
                fontSize = 28.sp, // Slightly adjusted from 32sp for better balance potentially
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp), // Adjusted padding
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Unit settings section
            item {
                Text("Unit", fontSize = 20.sp, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            items(units.size) { i ->
                SettingOptionItem(
                    title = units[i],
                    selected = settings.unit == units[i],
                    onClick = { settingsViewModel.updateUnit(units[i]) }
                )
            }

            // Wind Speed settings section
            item {
                Spacer(modifier = Modifier.height(12.dp)) // Consistent spacing
                Text("Wind Speed", fontSize = 20.sp, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            items(windSpeeds.size) { i ->
                SettingOptionItem(
                    title = windSpeeds[i],
                    selected = settings.windSpeed == windSpeeds[i],
                    onClick = { settingsViewModel.updateWindSpeed(windSpeeds[i]) }
                )
            }

            // Pressure settings section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Pressure", fontSize = 20.sp, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            items(pressures.size) { i ->
                SettingOptionItem(
                    title = pressures[i],
                    selected = settings.pressure == pressures[i],
                    onClick = { settingsViewModel.updatePressure(pressures[i]) }
                )
            }

            // Alerts settings section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Alerts", fontSize = 20.sp, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            item {
                SettingSwitchItem(
                    title = "Weather Notifications",
                    description = "Get notified about daily weather changes",
                    checked = settings.weatherNotifications,
                    onCheckedChange = { settingsViewModel.setWeatherNotifications(it) }
                )
            }
            item {
                SettingSwitchItem(
                    title = "Weather Warnings",
                    description = "Get notified about extreme conditions",
                    checked = settings.weatherWarnings,
                    onCheckedChange = { settingsViewModel.setWeatherWarnings(it) }
                )
            }

            // Security Section (New)
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Security", fontSize = 20.sp, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            item {
                Surface( // Wrap button in Surface for consistent styling if needed, or use Button directly
                    color = Color.DarkGray, // Match other items
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navigateToMfaSetup() } // Use the passed lambda
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), // Standard padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Setup/Manage Phone MFA", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        // Optionally add an icon >
                    }
                }
            }


            // About section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("About", fontSize = 20.sp, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            items(about.size) { index ->
                val title = about[index]
                // Use a consistent key for routing if `aboutRoutes` were intended as keys
                // For now, using title directly as part of the route string for simplicity with current nav setup.
                val routeSegment = title.replace(" ", "_").lowercase() // e.g. "terms_of_service"
                Surface(
                    color = Color.DarkGray,
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("about_app/$routeSegment")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(24.dp)) // More space before logout
                Button(
                    onClick = {
                        authViewModel.logout()
                        // MainActivity's LaunchedEffect(currentUser) will handle navigation to Login screen.
                        // No explicit navController.navigate("login") here as MainActivity controls this.
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red, // Or MaterialTheme.colorScheme.error
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Log out", style = MaterialTheme.typography.labelLarge) // Use theme typography
                }
            }
        }
    }
}

// SettingOptionItem and SettingSwitchItem composables remain the same as in your provided code.
// Ensure they are correctly defined or imported.
// Example stubs if they are not defined elsewhere:
@Composable
fun SettingOptionItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun SettingSwitchItem(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = Color.DarkGray,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.Gray,
                )
            )
        }
    }
}

// AboutScreen also remains as you've defined it.
// @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController, title: String) {
    // ... (implementation of AboutScreen from your file)
    // For brevity, not re-pasting the full AboutScreen text content here.
    // Ensure its package and imports are correct.
    val content = when (title.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) { // Normalize title back
        "Terms Of Service" -> "You have the following rights..." // Actual content
        "Privacy Notice" -> "Security of Your Information..." // Actual content
        "Share Feedback" -> "We’d love to hear from you!..." // Actual content
        else -> "Content for $title not found."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, // Display title
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = content, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}
