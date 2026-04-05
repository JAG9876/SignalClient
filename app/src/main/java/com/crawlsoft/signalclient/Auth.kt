package com.crawlsoft.signalclient

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

// 1. Data classes for the API
data class LoginRequest(val idToken: String, val deviceId: String)
data class TokenResponse(val accessToken: String, val refreshToken: String)

// 2. Retrofit API Interface
interface AuthService {
    @POST("api/v1/auth/loginwithgoogle") // Replace with your actual endpoint
    suspend fun exchangeToken(@Body request: LoginRequest): Response<TokenResponse>
}

// 3. Retrofit Instance (Singleton)
object RetrofitClient {
    val instance: AuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AuthService::class.java)
    }
}