package com.restify.courierapp

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ==========================================
// 1. МОДЕЛИ ДАННЫХ (Data Classes)
// ==========================================

// Модель для списка свободных заказов
data class OpenOrder(
    val id: Int,
    @SerializedName("restaurant_name") val restaurantName: String,
    @SerializedName("restaurant_address") val restaurantAddress: String,
    @SerializedName("dropoff_address") val dropoffAddress: String,
    val fee: Double,
    val price: Double,
    @SerializedName("dist_to_rest") val distToRest: Double?,
    @SerializedName("dist_trip") val distTrip: String?,
    @SerializedName("payment_type") val paymentType: String,
    @SerializedName("is_return") val isReturn: Boolean,
    val comment: String?
)

// Ответ на запрос "какой заказ сейчас активен?"
data class ActiveJobResponse(
    val active: Boolean,
    val job: ActiveJobDetail?
)

// Детальная информация об активном заказе
data class ActiveJobDetail(
    val id: Int,
    val status: String,
    @SerializedName("server_status") val serverStatus: String,
    @SerializedName("is_ready") val isReady: Boolean,
    @SerializedName("partner_name") val partnerName: String,
    @SerializedName("partner_address") val partnerAddress: String,
    @SerializedName("partner_phone") val partnerPhone: String?,
    @SerializedName("customer_address") val customerAddress: String,
    @SerializedName("customer_lat") val customerLat: Double?,
    @SerializedName("customer_lon") val customerLon: Double?,
    @SerializedName("customer_phone") val customerPhone: String,
    @SerializedName("customer_name") val customerName: String?,
    val comment: String?,
    @SerializedName("order_price") val orderPrice: Double,
    @SerializedName("delivery_fee") val deliveryFee: Double,
    @SerializedName("payment_type") val paymentType: String,
    @SerializedName("is_return_required") val isReturnRequired: Boolean
)

// Ответ для простых запросов
data class StatusResponse(
    val status: String,
    val message: String? = null
)

// --- МОДЕЛИ ДЛЯ ЧАТА (Синхронизировано с app.py) ---

data class ChatMessage(
    @SerializedName("role") val role: String, // "courier" или "partner"
    @SerializedName("text") val text: String, // Текст сообщения
    @SerializedName("time") val time: String  // Время в формате HH:mm
)

data class SendMessageResponse(
    val status: String
)

// ==========================================
// 2. ИНТЕРФЕЙС API (Retrofit)
// ==========================================

interface ApiService {

    // ЛОГИН
    @FormUrlEncoded
    @POST("/api/courier/login")
    suspend fun login(
        @Field("phone") phone: String,
        @Field("password") password: String
    ): retrofit2.Response<ResponseBody>

    // СПИСОК СВОБОДНЫХ ЗАКАЗОВ
    @GET("/api/courier/open_orders")
    suspend fun getOpenOrders(
        @Header("Cookie") cookie: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): List<OpenOrder>

    // АКТИВНЫЙ ЗАКАЗ КУРЬЕРА
    @GET("/api/courier/active_job")
    suspend fun getActiveJob(
        @Header("Cookie") cookie: String
    ): ActiveJobResponse

    // ПРИНЯТЬ ЗАКАЗ
    @FormUrlEncoded
    @POST("/api/courier/accept_order")
    suspend fun acceptOrder(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int
    ): retrofit2.Response<StatusResponse>

    // ПРИБЫЛ В ЗАКЛАД
    @FormUrlEncoded
    @POST("/api/courier/arrived_pickup")
    suspend fun arrivedAtPickup(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int
    ): StatusResponse

    // ОБНОВЛЕНИЕ СТАТУСА (picked_up, delivered)
    @FormUrlEncoded
    @POST("/api/courier/update_job_status")
    suspend fun updateJobStatus(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int,
        @Field("status") status: String
    ): StatusResponse

    // ОТПРАВКА GPS-КООРДИНАТ
    @FormUrlEncoded
    @POST("/api/courier/location")
    suspend fun sendLocation(
        @Header("Cookie") cookie: String,
        @Field("lat") lat: Double,
        @Field("lon") lon: Double
    ): StatusResponse

    // ОТПРАВКА FIREBASE ТОКЕНА
    @FormUrlEncoded
    @POST("/api/courier/fcm_token")
    suspend fun sendFcmToken(
        @Header("Cookie") cookie: String,
        @Field("token") token: String
    ): StatusResponse

    // ПЕРЕКЛЮЧИТЬ СТАТУС ОНЛАЙН/ОФЛАЙН
    @POST("/api/courier/toggle_status")
    suspend fun toggleStatus(
        @Header("Cookie") cookie: String
    ): ResponseBody

    // --- МЕТОДЫ ЧАТА ---

    // ПОЛУЧИТЬ ИСТОРИЮ ЧАТА ПО ID ЗАКАЗА
    @GET("/api/chat/history/{job_id}")
    suspend fun getChatMessages(
        @Header("Cookie") cookie: String,
        @Path("job_id") jobId: Int
    ): List<ChatMessage>

    // ОТПРАВИТЬ СООБЩЕНИЕ В ЧАТ
    @FormUrlEncoded
    @POST("/api/chat/send")
    suspend fun sendChatMessage(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int,
        @Field("message") message: String,
        @Field("role") role: String = "courier"
    ): SendMessageResponse
}

// ==========================================
// 3. МЕНЕДЖЕР WEBSOCKET
// ==========================================

class WebSocketManager(private val client: OkHttpClient) {
    private var webSocket: WebSocket? = null

    // Flow для прослушивания входящих сообщений (JSON строк) в UI
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messages = _messages.asSharedFlow()

    fun connect(cookie: String) {
        // Убедитесь, что используете правильный домен и протокол (wss:// для https)
        // Если ваш сервер на http://, используйте ws://
        val request = Request.Builder()
            .url("wss://restify.site/ws/courier")
            .addHeader("Cookie", cookie) // Сервер ожидает куки для авторизации
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Message received: $text")
                // Отправляем сообщение в Flow (игнорируем простые "pong")
                if (text != "pong") {
                    _messages.tryEmit(text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error", t)
                // Опционально: здесь можно добавить логику переподключения (reconnect)
            }
        })
    }

    // Отправка GPS координат (как ожидает бэкенд)
    fun sendLocation(lat: Double, lon: Double) {
        val json = JSONObject().apply {
            put("type", "init_location")
            put("lat", lat)
            put("lon", lon)
        }
        webSocket?.send(json.toString())
    }

    // Отправка пинга для поддержания соединения
    fun sendPing() {
        webSocket?.send(JSONObject().apply { put("type", "ping") }.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed/Logout")
        webSocket = null
    }
}

// ==========================================
// 4. КЛИЕНТ RETROFIT (Singleton)
// ==========================================

object RetrofitClient {
    private const val BASE_URL = "https://restify.site" // Укажите актуальный домен

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(false) // Важно для обработки кастомной авторизации
        .followSslRedirects(false)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Наш новый WebSocket менеджер, использующий тот же OkHttpClient
    val webSocketManager = WebSocketManager(okHttpClient)
}