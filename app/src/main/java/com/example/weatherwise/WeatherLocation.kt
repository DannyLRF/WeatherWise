package com.example.weatherwise


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.res.painterResource


@Composable
fun WeatherLocationsScreen(modifier: Modifier = Modifier) {
    val locations = listOf(
        LocationInfo("My location", "Clayton | Sunny", 25, R.drawable.sun_icon),
        LocationInfo("Changsha", "Overcast", 28, R.drawable.cloudy_icon),
        LocationInfo("London", "Rainy", 10, R.drawable.rain_icon),
        LocationInfo("Amsterdam", "Overcast", 12,  R.drawable.cloudy_icon),
        LocationInfo("Alaska", "Snow", -1,  R.drawable.snow_icon),
        LocationInfo("New York", "Cloudy Night", 12,  R.drawable.night_icon)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            //.padding(top = 32.dp)
    ) {
        // Top Bar (back arrow + title)
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            //modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // back arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopStart)
            )
            // title, "Location"
            Text(
                text = "Location",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).
                padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        TextField(
            value = "",
            onValueChange = {},
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
                .clip(RoundedCornerShape(12.dp))
        )
        // made a 16dp height space
        Spacer(modifier = Modifier.height(16.dp))

        // Location List
        LazyColumn {
            items(locations) { location ->
                WeatherItem(location)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun WeatherItem(location: LocationInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // left one
        Column {
            Text(location.name, color = Color.White, fontSize = 18.sp)
            Text(location.description, color = Color.Gray, fontSize = 14.sp)
        }
        // right one
        Row(
            // align the icon and temperature
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Box 的元素默认全部重叠在TopStart
            Box(
                modifier = Modifier.width(64.dp)
            ){
                Image(
                    painter = painterResource(id = location.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                //Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "${location.temperature}°",
                    color = Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

data class LocationInfo(
    val name: String,
    val description: String,
    val temperature: Int,
    val iconResId: Int // drawable resource ID
)
