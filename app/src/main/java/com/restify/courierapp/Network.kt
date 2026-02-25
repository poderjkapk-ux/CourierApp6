package com.restify.courierapp

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
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
    ): Response<ResponseBody>

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
    ): Response<StatusResponse>

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

    // --- МЕТОДЫ ЧАТА (Исправлены пути и параметры) ---

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
        @Field("message") message: String, // В app.py поле называется "message"
        @Field("role") role: String = "courier" // Указываем роль отправителя
    ): SendMessageResponse
}

// ==========================================
// 3. КЛИЕНТ RETROFIT (Singleton)
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
}