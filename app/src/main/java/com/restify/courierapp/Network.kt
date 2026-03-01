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
// 1. МОДЕЛІ ДАНИХ (Data Classes)
// ==========================================

// Модель для списку вільних замовлень
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

// Відповідь на запит "яке замовлення зараз активне?"
data class ActiveJobResponse(
    val active: Boolean,
    val job: ActiveJobDetail?
)

// Детальна інформація про активне замовлення
data class ActiveJobDetail(
    val id: Int,
    val status: String,
    @SerializedName("server_status") val serverStatus: String,
    @SerializedName("is_ready") val isReady: Boolean,

    // --- НОВІ ПОЛЯ ДЛЯ ТАЙМЕРІВ ---
    @SerializedName("assigned_at") val assignedAt: String?,
    @SerializedName("picked_up_at") val pickedUpAt: String?,
    @SerializedName("delivered_at") val deliveredAt: String?,
    @SerializedName("completed_at") val completedAt: String?,
    // ---------------------------------

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

// Відповідь для простих запитів
data class StatusResponse(
    val status: String,
    val message: String? = null
)

// --- МОДЕЛІ ДЛЯ ЧАТУ (Синхронізовано з app.py) ---

data class ChatMessage(
    @SerializedName("role") val role: String, // "courier" або "partner"
    @SerializedName("text") val text: String, // Текст повідомлення
    @SerializedName("time") val time: String  // Час у форматі HH:mm
)

data class SendMessageResponse(
    val status: String
)

// --- НОВА МОДЕЛЬ ДЛЯ ІСТОРІЇ ЗАМОВЛЕНЬ ---
data class HistoryOrder(
    val id: Int,
    val date: String,
    val address: String,
    val price: Double,
    val status: String
)

// ==========================================
// 2. ІНТЕРФЕЙС API (Retrofit)
// ==========================================

interface ApiService {

    // ЛОГІН
    @FormUrlEncoded
    @POST("/api/courier/login")
    suspend fun login(
        @Field("phone") phone: String,
        @Field("password") password: String
    ): retrofit2.Response<ResponseBody>

    // СПИСОК ВІЛЬНИХ ЗАМОВЛЕНЬ
    @GET("/api/courier/open_orders")
    suspend fun getOpenOrders(
        @Header("Cookie") cookie: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): List<OpenOrder>

    // АКТИВНЕ ЗАМОВЛЕННЯ КУР'ЄРА
    @GET("/api/courier/active_job")
    suspend fun getActiveJob(
        @Header("Cookie") cookie: String
    ): ActiveJobResponse

    // ПРИЙНЯТИ ЗАМОВЛЕННЯ
    @FormUrlEncoded
    @POST("/api/courier/accept_order")
    suspend fun acceptOrder(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int
    ): retrofit2.Response<StatusResponse>

    // ПРИБУВ У ЗАКЛАД
    @FormUrlEncoded
    @POST("/api/courier/arrived_pickup")
    suspend fun arrivedAtPickup(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int
    ): StatusResponse

    // ОНОВЛЕННЯ СТАТУСУ (picked_up, delivered)
    @FormUrlEncoded
    @POST("/api/courier/update_job_status")
    suspend fun updateJobStatus(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int,
        @Field("status") status: String
    ): StatusResponse

    // ВІДПРАВКА GPS-КООРДИНАТ
    @FormUrlEncoded
    @POST("/api/courier/location")
    suspend fun sendLocation(
        @Header("Cookie") cookie: String,
        @Field("lat") lat: Double,
        @Field("lon") lon: Double
    ): StatusResponse

    // ВІДПРАВКА FIREBASE ТОКЕНА
    @FormUrlEncoded
    @POST("/api/courier/fcm_token")
    suspend fun sendFcmToken(
        @Header("Cookie") cookie: String,
        @Field("token") token: String
    ): StatusResponse

    // ПЕРЕКЛЮЧИТИ СТАТУС ОНЛАЙН/ОФЛАЙН
    @POST("/api/courier/toggle_status")
    suspend fun toggleStatus(
        @Header("Cookie") cookie: String
    ): ResponseBody

    // --- МЕТОДИ ЧАТУ ---

    // ОТРИМАТИ ІСТОРІЮ ЧАТУ ЗА ID ЗАМОВЛЕННЯ
    @GET("/api/chat/history/{job_id}")
    suspend fun getChatMessages(
        @Header("Cookie") cookie: String,
        @Path("job_id") jobId: Int
    ): List<ChatMessage>

    // ВІДПРАВИТИ ПОВІДОМЛЕННЯ В ЧАТ
    @FormUrlEncoded
    @POST("/api/chat/send")
    suspend fun sendChatMessage(
        @Header("Cookie") cookie: String,
        @Field("job_id") jobId: Int,
        @Field("message") message: String,
        @Field("role") role: String = "courier"
    ): SendMessageResponse

    // --- НОВИЙ МЕТОД ДЛЯ ІСТОРІЇ ЗАМОВЛЕНЬ ---
    @GET("/api/courier/history")
    suspend fun getHistory(
        @Header("Cookie") cookie: String
    ): List<HistoryOrder>
}

// ==========================================
// 3. МЕНЕДЖЕР WEBSOCKET
// ==========================================

class WebSocketManager(private val client: OkHttpClient) {
    private var webSocket: WebSocket? = null

    // Flow для прослуховування вхідних повідомлень (JSON рядків) у UI
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messages = _messages.asSharedFlow()

    fun connect(cookie: String) {
        val request = Request.Builder()
            .url("wss://restify.site/ws/courier")
            .addHeader("Cookie", cookie)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Message received: $text")
                if (text != "pong") {
                    _messages.tryEmit(text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error", t)
            }
        })
    }

    fun sendLocation(lat: Double, lon: Double) {
        val json = JSONObject().apply {
            put("type", "init_location")
            put("lat", lat)
            put("lon", lon)
        }
        webSocket?.send(json.toString())
    }

    fun sendPing() {
        webSocket?.send(JSONObject().apply { put("type", "ping") }.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed/Logout")
        webSocket = null
    }
}

// ==========================================
// 4. КЛІЄНТ RETROFIT (Singleton)
// ==========================================

object RetrofitClient {
    private const val BASE_URL = "https://restify.site"

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
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

    val webSocketManager = WebSocketManager(okHttpClient)
}