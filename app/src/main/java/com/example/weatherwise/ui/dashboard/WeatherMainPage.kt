package com.example.weatherwise

import android.app.VoiceInteractor
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.Request
import android.Manifest
import android.location.Location
import org.json.JSONObject
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import java.time.LocalDate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun WeatherMainPage(navController: NavController) {
    val context = LocalContext.current
    val viewModel: WeatherViewModel = viewModel()

    // 页面加载时拉取一次数据
    LaunchedEffect(Unit) {
        viewModel.loadAllData(context, "3a936acc8bb109dcb94017abbc0ec0fb")
    }

    val cityName by viewModel.cityName
    val weatherData by viewModel.weatherData
    val hourlyList by viewModel.hourlyList
    val dailyList by viewModel.dailyList
    val userLat by viewModel.userLat
    val userLon by viewModel.userLon

    // 保持你原有的 UI 渲染逻辑，只需要替换使用 viewModel 提供的数据
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navController.navigate("city") }) {
                Icon(Icons.Default.Menu, contentDescription = "City", tint = Color.White)
            }

            Text(
                text = cityName,
                color = Color.White,
                fontSize = 18.sp
            )

            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        weatherData?.let { data ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${TemperatureSettings.convertTemp(data.temperature).toInt()}${TemperatureSettings.getUnitSymbol()}",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val iconMap = mapOf(
                    "Clear" to R.drawable.sun_icon,
                    "Clouds" to R.drawable.cloudy_icon,
                    "Rain" to R.drawable.rain_icon,
                    "Snow" to R.drawable.snow_icon,
                    "Thunderstorm" to R.drawable.thunder_icon
                )
                val iconRes = iconMap[data.description] ?: R.drawable.unknown

                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = "Weather Icon",
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${data.description}\n↓ ${TemperatureSettings.convertTemp(data.minTemp).toInt()}${TemperatureSettings.getUnitSymbol()} ↑ ${TemperatureSettings.convertTemp(data.maxTemp).toInt()}${TemperatureSettings.getUnitSymbol()}",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    HourlyWeatherGraphCombined(hourlyList)
                }

                NextThreeDaysWeatherSection(
                    dailyWeather = dailyList,
                    onFiveDayClick = {
                        navController.navigate("five_day_forecast/$userLat/$userLon")
                    }
                )
            }
        }
    }
}

fun getLastKnownLocation(context: Context): android.location.Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = locationManager.getProviders(true)

    for (provider in providers.reversed()) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return null

        val location = locationManager.getLastKnownLocation(provider)
        if (location != null) return location
    }
    return null
}

data class WeatherData(
    val temperature: Double,
    val description: String,
    val icon: String,
    val minTemp: Double,
    val maxTemp: Double
)

data class HourlyWeather(
    val time: String,         // 格式如 "15:00"
    val temperature: Double,
    val icon: String,
    val description: String
)

@Composable
fun HourlyWeatherGraphCombined(hourlyList: List<HourlyWeather>) {
    if (hourlyList.isEmpty()) return

    val maxTemp = hourlyList.maxOf { it.temperature }
    val minTemp = hourlyList.minOf { it.temperature }
    val tempRange = (maxTemp - minTemp).coerceAtLeast(1.0)

    val graphHeight = 160.dp // 增高图形区域，避免上下重叠
    val graphWidthPerItem = 60.dp
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 图标 + 温度行
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
        ) {
            hourlyList.forEach { hour ->
                Column(
                    modifier = Modifier.width(graphWidthPerItem),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val iconRes = when (hour.icon) {
                        "Clear" -> R.drawable.sun_icon
                        "Clouds" -> R.drawable.cloudy_icon
                        "Rain" -> R.drawable.rain_icon
                        else -> R.drawable.unknown
                    }
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${hour.temperature.toInt()}°",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 曲线图
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .height(graphHeight)
                    .width(graphWidthPerItem * hourlyList.size)
            ) {
                val spacing = size.width / hourlyList.size
                val halfSpacing = spacing / 2

                val topPadding = 20.dp.toPx()
                val bottomPadding = 20.dp.toPx()
                val usableHeight = size.height - topPadding - bottomPadding

                val points = hourlyList.mapIndexed { index, hour ->
                    val x = spacing * index + halfSpacing
                    val y = topPadding + usableHeight * (1 - (hour.temperature - minTemp) / tempRange)
                    Offset(x.toFloat(), y.toFloat())
                }

                // 曲线
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val current = points[i]
                        val midX = (prev.x + current.x) / 2
                        cubicTo(midX, prev.y, midX, current.y, current.x, current.y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color.Cyan,
                    style = Stroke(width = 4f)
                )

                // 圆点 + 温度
                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = Color.Cyan,
                        radius = 6f,
                        center = point
                    )
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            "${hourlyList[index].temperature.toInt()}°",
                            point.x,
                            point.y - 10f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 28f
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 时间标签行
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
        ) {
            hourlyList.forEach { hour ->
                Box(
                    modifier = Modifier.width(graphWidthPerItem),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hour.time,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

data class DailyWeather(
    val date: String,           // yyyy-MM-dd
    val dayLabel: String,       // Today, Mon, Tue...
    val currentTemp: Double,    // 当前温度（取中午的）
    val maxTemp: Double,
    val minTemp: Double,
    val description: String,
    val icon: String,
    val windSpeed: Double,
    val humidity: Int
)

@Composable
fun NextThreeDaysWeatherSection(
    dailyWeather: List<DailyWeather>,
    onFiveDayClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dailyWeather.take(3)) { day ->
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = day.dayLabel, color = Color.White, fontSize = 14.sp)

                    val iconRes = when (day.icon) {
                        "Clear" -> R.drawable.sun_icon
                        "Clouds" -> R.drawable.cloudy_icon
                        "Rain" -> R.drawable.rain_icon
                        else -> R.drawable.unknown
                    }

                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )

                    Text(
                        text = day.description,
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "↑${day.maxTemp.toInt()}° ↓${day.minTemp.toInt()}°",
                        color = Color.White,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "💨${day.windSpeed} m/s",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    Text(
                        text = "💧${day.humidity}%",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onFiveDayClick() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "View 5-Day Forecast")
        }
    }
}

@Composable
fun FiveDayForecastPage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "5-Day Forecast Page", color = Color.White)
    }
}

suspend fun getNextFiveDaysWeather(lat: Double, lon: Double, apiKey: String): List<DailyWeather> {
    val url = "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
    val request = Request.Builder().url(url).build()

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        val dailyMap = mutableMapOf<String, MutableList<JSONObject>>()
        val dailyList = mutableListOf<DailyWeather>()

        if (response.isSuccessful) {
            val json = JSONObject(response.body?.string())
            val list = json.getJSONArray("list")

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val dtTxt = item.getString("dt_txt")
                val date = dtTxt.substring(0, 10)

                if (!dailyMap.containsKey(date)) {
                    dailyMap[date] = mutableListOf()
                }
                dailyMap[date]?.add(item)
            }

            val today = LocalDate.now()
            for (i in 0..4) {
                val date = today.plusDays(i.toLong()).toString()
                val entries = dailyMap[date] ?: continue

                val temps = entries.map { it.getJSONObject("main").getDouble("temp") }
                val minTemp = temps.minOrNull() ?: continue
                val maxTemp = temps.maxOrNull() ?: continue

                val noon = entries.find { it.getString("dt_txt").contains("12:00:00") } ?: entries[0]
                val main = noon.getJSONObject("main")
                val weather = noon.getJSONArray("weather").getJSONObject(0)
                val wind = noon.getJSONObject("wind")

                val currentTemp = main.getDouble("temp")
                val humidity = main.getInt("humidity")
                val windSpeed = wind.getDouble("speed")
                val description = weather.getString("main")
                val icon = description

                val dayLabel = when (i) {
                    0 -> "Today"
                    else -> today.plusDays(i.toLong()).dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() }
                }

                dailyList.add(
                    DailyWeather(
                        date = date,
                        dayLabel = dayLabel,
                        currentTemp = currentTemp,
                        minTemp = minTemp,
                        maxTemp = maxTemp,
                        description = description,
                        icon = icon,
                        windSpeed = windSpeed,
                        humidity = humidity
                    )
                )
            }
        }

        dailyList
    }
}

@Composable
fun FiveDayForecastPage(
    lat: Double,
    lon: Double,
    apiKey: String,
    onBack: () -> Unit
) {
    val dailyList = remember { mutableStateOf<List<DailyWeather>>(emptyList()) }

    LaunchedEffect(Unit) {
        val data = getNextFiveDaysWeather(lat, lon, apiKey)
        dailyList.value = data
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // 返回按钮
        Text(
            text = "← Back",
            color = Color.Cyan,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable { onBack() }
                .padding(bottom = 16.dp)
        )

        // 天气信息列表（占屏幕 2/3 高度）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.66f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            dailyList.value.forEach { day ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 日期
                        Text(
                            text = day.dayLabel,
                            color = Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )

                        // 图标
                        val iconRes = when (day.icon) {
                            "Clear" -> R.drawable.sun_icon
                            "Clouds" -> R.drawable.cloudy_icon
                            "Rain" -> R.drawable.rain_icon
                            else -> R.drawable.unknown
                        }

                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .weight(1f)
                        )

                        // 温度
                        Text(
                            text = "↑${day.maxTemp.toInt()}° ↓${day.minTemp.toInt()}°",
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(2f)
                        )

                        // 风速
                        Text(
                            text = "💨 ${day.windSpeed} m/s",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(2f)
                        )

                        // 湿度
                        Text(
                            text = "💧 ${day.humidity}%",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun TemperatureTrendGraph(data: List<DailyWeather>, itemWidth: Dp) {
    val graphHeight = 180.dp

    val maxTempList = data.map { it.maxTemp }
    val minTempList = data.map { it.minTemp }

    val overallMax = maxTempList.maxOrNull() ?: return
    val overallMin = minTempList.minOrNull() ?: return
    val tempRange = (overallMax - overallMin).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .height(graphHeight)
            .width(itemWidth * data.size)
            .background(Color.DarkGray, RoundedCornerShape(12.dp))
    ) {
        val spacing = size.width / data.size
        val topPadding = 20.dp.toPx()
        val bottomPadding = 20.dp.toPx()
        val usableHeight = size.height - topPadding - bottomPadding

        val maxPoints = maxTempList.mapIndexed { i, temp ->
            Offset(
                (spacing * i + spacing / 2).toFloat(),
                (topPadding + usableHeight * (1 - (temp - overallMin) / tempRange)).toFloat()
            )
        }

        val minPoints = minTempList.mapIndexed { i, temp ->
            Offset(
                (spacing * i + spacing / 2).toFloat(),
                (topPadding + usableHeight * (1 - (temp - overallMin) / tempRange)).toFloat()
            )
        }

        fun drawLine(points: List<Offset>, color: Color) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val midX = (prev.x + curr.x) / 2
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }
            drawPath(path, color, style = Stroke(width = 4f))
        }

        drawLine(maxPoints, Color.Red)
        drawLine(minPoints, Color.Cyan)

        maxPoints.forEachIndexed { i, point ->
            drawCircle(Color.Red, radius = 6f, center = point)
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    "${maxTempList[i].toInt()}°",
                    point.x,
                    point.y - 12f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        minPoints.forEachIndexed { i, point ->
            drawCircle(Color.Cyan, radius = 6f, center = point)
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    "${minTempList[i].toInt()}°",
                    point.x,
                    point.y + 28f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}