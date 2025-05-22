package com.example.weatherwise

// UI components and layout imports from Jetpack Compose
import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.weatherwise.ui.dashboard.CityInfo
import com.example.weatherwise.ui.dashboard.CityViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherwise.ui.dashboard.CityEntity
import com.example.weatherwise.ui.dashboard.CityViewModelFactory


/**
 * CityScreen is the main UI for searching and managing a list of cities.
 * It includes a top bar, a search field, a list of search suggestions,
 * and a list of added city weather cards.
 */
@Composable
fun CityScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val factory = remember { CityViewModelFactory(context.applicationContext as Application, userId) }
    val viewModel: CityViewModel = viewModel(factory = factory)

    val cities by viewModel.cityList.collectAsState()
    val searchText = viewModel.searchText

    var cityToDelete by remember { mutableStateOf<CityEntity?>(null) }
    if (cityToDelete != null) {
        AlertDialog(
            onDismissRequest = { cityToDelete = null },
            title = { Text("Delete city") },
            text = { Text("Are you sure you want to delete ${cityToDelete!!.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeCity(cityToDelete!!)
                    cityToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { cityToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)           // Page background color
            .padding(16.dp)                    // General page padding
    ) {
        // Top bar with back button and title
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable { navController.popBackStack() } // Go back on click
            )
            Text(
                text = "City",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // Space between top bar and search

        // Search text field
        TextField(
            value = searchText,
            onValueChange = {
                viewModel.onSearchTextChange(it) // Update search input state
                viewModel.searchCity(it)         // Trigger search
            },
            placeholder = { Text("Search city", color = Color.Gray) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.DarkGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                textColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)) // Rounded corners
        )

        val searchResults = viewModel.searchResults

        // Display search suggestions if available
        if (searchResults.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                searchResults.forEach { result ->
                    Text(
                        text = "${result.name}, ${result.state ?: result.country}",
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // When clicked: add city to list and reset search field
                                viewModel.fetchCityInfoAndAdd(result.name)
                                viewModel.onSearchTextChange("")
                                viewModel.clearSearchResults()
                            }
                            .padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp)) // Space before city list

        // Show the list of added cities using LazyColumn
        LazyColumn {
            itemsIndexed(cities) { index, cityEntity ->
                // manually convert cityEntity to cityInfo
                val city = CityInfo(
                    name = cityEntity.name,
                    description = cityEntity.description,
                    temperature = cityEntity.temperature,
                    iconResId = cityEntity.iconResId,
                    lat = cityEntity.lat,
                    lon = cityEntity.lon
                )

                val isCurrentLocation = city.description == "My Location" && index == 0

                CityItem(city = city,
                    onClick = {
                        // Navigate to main page with selected city's coordinates
                        navController.navigate("main_page/${city.lat}/${city.lon}")
                    },
                    onLongPress = {
                        if (!isCurrentLocation) {
                            cityToDelete = cityEntity
                        }
                    })

                Spacer(modifier = Modifier.height(8.dp)) // Spacing between city items
            }
        }
    }
}

/**
 * CityItem is a UI component that shows one city’s name, weather, and icon.
 * When clicked, it triggers the provided onClick callback.
 */
@Composable
fun CityItem(
    city: CityInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        backgroundColor = Color.DarkGray,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray, shape = RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left column: city name and description
            Column {
                Text(city.name, color = Color.White, fontSize = 18.sp)
                Text(city.description, color = Color.Gray, fontSize = 14.sp)
            }

            // Right box: icon and temperature
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(64.dp)) {
                    Image(
                        painter = painterResource(id = city.iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp) // Weather icon
                    )
                    Text(
                        "${city.temperature}°",
                        color = Color.White,
                        fontSize = 22.sp,
                        modifier = Modifier.align(Alignment.CenterEnd) // Align temp to end
                    )
                }
            }
        }
    }
}
