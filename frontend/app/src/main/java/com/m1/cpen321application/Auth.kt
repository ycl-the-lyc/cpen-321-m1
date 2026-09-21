package com.m1.cpen321application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
  @GET("auth/google") suspend fun google(): GoogleResponse

  @POST("auth/google/done")
  suspend fun googleDone(@Body request: GoogleDoneRequest): GoogleDoneResponse
}

data class GoogleResponse(val url: String)

data class GoogleDoneRequest(val ticket: String)

data class GoogleDoneResponse(val sessionId: String, val user: User)

data class User(val id: String, val email: String, val familyName: String, val givenName: String)

object AuthClient {
  private val rf =
          Retrofit.Builder()
                  .baseUrl(BuildConfig.API_BASE_URL)
                  .addConverterFactory(GsonConverterFactory.create())
                  .build()

  val authApi: AuthApi = rf.create(AuthApi::class.java)
}

class AuthSession(private val api: AuthApi) {
  private var id: String? = null

  private var user: User? = null

  suspend fun getGoogleUrl(): String {
    return api.google().url
  }

  suspend fun doneGoogle(ticket: String) {
    val res = api.googleDone(GoogleDoneRequest(ticket))
    id = res.sessionId
    user = res.user
  }

  fun set(id: String) {
    this.id = id
  }

  fun get(): String? {
    return id
  }

  fun del() {
    this.id = null
    this.user = null
  }

  fun isIn(): Boolean {
    return id != null
  }

  fun getUser(): User? {
    return user
  }
}
