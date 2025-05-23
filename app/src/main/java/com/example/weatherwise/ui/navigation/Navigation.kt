package com.example.weatherwise.ui.navigation

enum class Screen(val routeName: String) { // 可选：添加 routeName 属性
    Login("login_screen"), // 示例路由名称
    Register("register_screen"),
    WeatherHome("weather_home_screen"), // 通常 NavHost 的 startDestination 是这个
    MfaSetup("mfa_setup_screen"),
    MfaVerifyLogin("mfa_verify_login_screen");

    // 如果不添加 routeName，并依赖 Screen.name, 确保 Screen.Login.name 等符合路由格式
}