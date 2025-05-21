package com.example.weatherwise

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.weatherwise.ui.dashboard.CityInfo
import com.example.weatherwise.ui.dashboard.CityViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.net.URLEncoder

@Composable
fun CityScreen(navController: NavController, viewModel: CityViewModel = viewModel()) {
    val cities = viewModel.cityList
    val searchText = viewModel.searchText
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Top bar
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
                    .clickable { navController.popBackStack() } // ✅ 返回上一页
            )
            Text(
                text = "City",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search
        TextField(
            value = searchText,
            onValueChange = { viewModel.onSearchTextChange(it) },
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

        Spacer(modifier = Modifier.height(8.dp))

        // 添加按钮（测试用，可改为根据搜索建议添加）
        Button(
            onClick = {
                // 示例：根据搜索框添加城市
                val newCity = CityInfo(
                    name = viewModel.searchText,
                    description = "Manual Add",
                    temperature = (5..35).random(),
                    iconResId = R.drawable.sun_icon
                )
                viewModel.addCity(newCity)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add")
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(cities) { city ->
                CityItem(city = city) {
                    //  click city item to MainPage
                    navController.navigate("main_page/${city.lat}/${city.lon}")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


@Composable
fun CityItem(city: CityInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
            .clickable { onClick() }, // 支持点击事件
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
                Text(city.name, color = Color.White, fontSize = 18.sp)
                Text(city.description, color = Color.Gray, fontSize = 14.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(64.dp)) {
                Image(
                    painter = painterResource(id = city.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "${city.temperature}°",
                    color = Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}


