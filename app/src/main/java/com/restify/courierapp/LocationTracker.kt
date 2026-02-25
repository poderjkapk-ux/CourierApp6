package com.restify.courierapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationTracker : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // ID для нашого сповіщення
    private val NOTIFICATION_CHANNEL_ID = "courier_location_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        // Що робити, коли телефон отримує нові координати від супутника
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationToServer(location.latitude, location.longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // Дозволи ми будемо запитувати в MainActivity
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Запускаємо службу на передньому плані (з постійним сповіщенням)
        startForeground(NOTIFICATION_ID, createNotification())

        // Налаштовуємо частоту оновлення GPS (кожні 15 секунд)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
            .setMinUpdateIntervalMillis(10000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // START_STICKY означає, що якщо Android випадково вб'є службу через нестачу пам'яті, він спробує її перезапустити
        return START_STICKY
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        try {
            // --- НОВОЕ: Відправляємо координати через швидкий WebSocket замість REST API ---
            RetrofitClient.webSocketManager.sendLocation(lat, lon)
            Log.d("LocationTracker", "Відправлено GPS через WS: $lat, $lon")
        } catch (e: Exception) {
            Log.e("LocationTracker", "Помилка відправки GPS через WS: ${e.message}")
        }
    }

    // Створюємо канал сповіщень (обов'язково для Android 8.0 і вище)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Геолокація кур'єра",
                NotificationManager.IMPORTANCE_LOW // Низька важливість, щоб не пікало постійно
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Створюємо саме сповіщення, яке буде "висіти" в шторці
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Restify Кур'єр")
            .setContentText("Відстеження геолокації активно")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Стандартна іконка прицілу
            .setOngoing(true) // Користувач не може змахнути це сповіщення
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Цей метод потрібен для архітектури Service, але в нашому випадку він не використовується
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}