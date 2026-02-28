package com.restify.courierapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.osmdroid.config.Configuration
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Функція для запуску служби геолокації
    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationTracker::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // Допоміжна функція для отримання координат (безпечно для корутин)
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location -> cont.resume(location) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізація OpenStreetMap (потрібно для відображення карти)
        Configuration.getInstance().userAgentValue = packageName

        // Ініціалізація клієнта геолокації
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val sharedPref = getSharedPreferences("CourierPrefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val coroutineScope = rememberCoroutineScope()
                    val savedCookie = sharedPref.getString("cookie", null)

                    // --- Управління життєвим циклом WebSocket та FCM-токеном ---
                    LaunchedEffect(Unit) {
                        if (savedCookie != null) {
                            // Підключаємо WebSocket
                            RetrofitClient.webSocketManager.connect(savedCookie)

                            // ОНОВЛЕННЯ: Відправляємо FCM-токен на сервер при кожному старті додатку
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result
                                    coroutineScope.launch {
                                        try {
                                            RetrofitClient.apiService.sendFcmToken(savedCookie, token)
                                            Log.d("FCM_TOKEN", "Токен успішно оновлено при старті: $token")
                                        } catch (e: Exception) {
                                            Log.e("FCM_TOKEN", "Помилка відправки токена при старті: ${e.message}")
                                        }
                                    }
                                } else {
                                    Log.e("FCM_TOKEN", "Не вдалося отримати токен від Firebase", task.exception)
                                }
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            RetrofitClient.webSocketManager.disconnect()
                        }
                    }
                    // ---------------------------------------------------

                    // БЛОК ЗАПИТУ ДОЗВОЛІВ (GPS та Сповіщення)
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

                    // Запитуємо дозволи при старті
                    LaunchedEffect(permissionsState.allPermissionsGranted) {
                        if (!permissionsState.allPermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                        } else {
                            if (savedCookie != null) {
                                startLocationService()
                            }
                        }
                    }

                    val startDestination = if (savedCookie != null) "orders" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {

                        // РОУТ 1: ЛОГІН
                        composable("login") {
                            var isLoading by remember { mutableStateOf(false) }
                            var errorMessage by remember { mutableStateOf<String?>(null) }

                            LoginScreen(
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                onLoginClick = { phone, password ->
                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            val response = RetrofitClient.apiService.login(phone, password)
                                            if (response.isSuccessful || response.code() == 302 || response.code() == 303) {
                                                val tokenCookie = response.headers().values("Set-Cookie").firstOrNull { it.contains("courier_token") }

                                                if (tokenCookie != null) {
                                                    val cookieValue = tokenCookie.split(";")[0]
                                                    sharedPref.edit().putString("cookie", cookieValue).apply()

                                                    // Підключаємо WebSocket одразу після успішного входу
                                                    RetrofitClient.webSocketManager.connect(cookieValue)

                                                    // Відправляємо FCM токен після успішного логіну
                                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            val token = task.result
                                                            coroutineScope.launch {
                                                                try {
                                                                    RetrofitClient.apiService.sendFcmToken(cookieValue, token)
                                                                    Log.d("FCM_TOKEN", "Токен успішно відправлено після логіну: $token")
                                                                } catch (e: Exception) {
                                                                    Log.e("FCM_TOKEN", "Помилка відправки токена після логіну: ${e.message}")
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (permissionsState.allPermissionsGranted) {
                                                        startLocationService()
                                                    }

                                                    navController.navigate("orders") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                } else {
                                                    errorMessage = "Помилка: Немає токена"
                                                }
                                            } else {
                                                errorMessage = "Невірний телефон або пароль"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Помилка мережі"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            )
                        }

                        // РОУТ 2: СПИСОК ЗАМОВЛЕНЬ
                        composable("orders") {
                            var ordersList by remember { mutableStateOf<List<OpenOrder>>(emptyList()) }
                            var isLoading by remember { mutableStateOf(true) }
                            var isOnline by remember { mutableStateOf(true) }
                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            // ДОДАНО: параметр isSilent для "тихого" оновлення
                            fun fetchData(isSilent: Boolean = false) {
                                if (!isSilent) isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val activeJobRes = RetrofitClient.apiService.getActiveJob(currentCookie)
                                        if (activeJobRes.active) {
                                            navController.navigate("active_order") {
                                                popUpTo("orders") { inclusive = true }
                                            }
                                            return@launch
                                        }

                                        // Отримуємо реальні координати
                                        var currentLat = 0.0
                                        var currentLon = 0.0

                                        if (permissionsState.allPermissionsGranted) {
                                            val location = getLastKnownLocation()
                                            if (location != null) {
                                                currentLat = location.latitude
                                                currentLon = location.longitude
                                            }
                                        }

                                        ordersList = RetrofitClient.apiService.getOpenOrders(
                                            currentCookie,
                                            lat = currentLat,
                                            lon = currentLon
                                        )
                                    } catch (e: Exception) {
                                        if (!isSilent) Toast.makeText(this@MainActivity, "Помилка завантаження", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        if (!isSilent) isLoading = false
                                    }
                                }
                            }

                            LaunchedEffect(Unit) { fetchData(isSilent = false) }

                            // ДОДАНО: Фонове тихе оновлення кожні 5 секунд
                            LaunchedEffect(Unit) {
                                while (true) {
                                    kotlinx.coroutines.delay(5000)
                                    fetchData(isSilent = true)
                                }
                            }

                            // Слухаємо WebSocket події для миттєвого оновлення списку
                            LaunchedEffect(Unit) {
                                RetrofitClient.webSocketManager.messages.collect { messageJson ->
                                    try {
                                        val json = JSONObject(messageJson)
                                        val type = json.getString("type")
                                        if (type == "new_order" || type == "job_update") {
                                            fetchData(isSilent = true) // Оновлюємо список тихо
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            OrdersListScreen(
                                orders = ordersList,
                                isLoading = isLoading,
                                isOnline = isOnline,
                                onNavigateToHistory = {
                                    navController.navigate("history")
                                },
                                onToggleStatus = { newStatus ->
                                    isOnline = newStatus
                                    coroutineScope.launch {
                                        try {
                                            RetrofitClient.apiService.toggleStatus(currentCookie)

                                            if (newStatus) {
                                                if (permissionsState.allPermissionsGranted) {
                                                    startLocationService()
                                                }
                                            } else {
                                                val serviceIntent = Intent(this@MainActivity, LocationTracker::class.java)
                                                stopService(serviceIntent)
                                            }
                                        } catch (e: Exception) {
                                            isOnline = !newStatus
                                        }
                                    }
                                },
                                onRefresh = { fetchData(isSilent = false) },
                                onAcceptOrder = { jobId ->
                                    coroutineScope.launch {
                                        try {
                                            val res = RetrofitClient.apiService.acceptOrder(currentCookie, jobId)
                                            if (res.isSuccessful) fetchData(isSilent = false)
                                        } catch (e: Exception) {}
                                    }
                                }
                            )
                        }

                        // РОУТ 3: АКТИВНЕ ЗАМОВЛЕННЯ
                        composable("active_order") {
                            var activeJob by remember { mutableStateOf<ActiveJobDetail?>(null) }
                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            fun fetchActiveJob() {
                                coroutineScope.launch {
                                    try {
                                        val res = RetrofitClient.apiService.getActiveJob(currentCookie)
                                        if (res.active && res.job != null) activeJob = res.job
                                        else navController.navigate("orders") { popUpTo("active_order") { inclusive = true } }
                                    } catch (e: Exception) {}
                                }
                            }

                            LaunchedEffect(Unit) { fetchActiveJob() }

                            LaunchedEffect(Unit) {
                                RetrofitClient.webSocketManager.messages.collect { messageJson ->
                                    try {
                                        val json = JSONObject(messageJson)
                                        val type = json.getString("type")
                                        if (type == "job_update" || type == "job_ready") {
                                            fetchActiveJob()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            activeJob?.let { job ->
                                ActiveOrderScreen(
                                    job = job,
                                    cookie = currentCookie,
                                    onRefresh = { fetchActiveJob() },
                                    onArrivedPickup = { jobId ->
                                        coroutineScope.launch {
                                            try { RetrofitClient.apiService.arrivedAtPickup(currentCookie, jobId); fetchActiveJob() } catch (e: Exception) {}
                                        }
                                    },
                                    onUpdateStatus = { jobId, status ->
                                        coroutineScope.launch {
                                            try { RetrofitClient.apiService.updateJobStatus(currentCookie, jobId, status); fetchActiveJob() } catch (e: Exception) {}
                                        }
                                    }
                                )
                            }
                        }

                        // РОУТ 4: ІСТОРІЯ ЗАМОВЛЕНЬ
                        composable("history") {
                            var historyList by remember { mutableStateOf<List<HistoryOrder>>(emptyList()) }
                            var isLoading by remember { mutableStateOf(true) }
                            val currentCookie = sharedPref.getString("cookie", "") ?: ""

                            fun fetchHistory() {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        historyList = RetrofitClient.apiService.getHistory(currentCookie)
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Помилка завантаження історії", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }

                            LaunchedEffect(Unit) { fetchHistory() }

                            HistoryScreen(
                                history = historyList,
                                isLoading = isLoading,
                                onBack = { navController.popBackStack() },
                                onRefresh = { fetchHistory() }
                            )
                        }
                    }
                }
            }
        }
    }
}