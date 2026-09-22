package com.m1.cpen321application

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

data class UserRequest(val id: String)

interface UserApi {
  @POST("ip") suspend fun getIp(@Body request: UserRequest): IpResponse

  @POST("time") suspend fun getTime(@Body request: UserRequest): TimeResponse

  @POST("name") suspend fun getName(@Body request: UserRequest): NameResponse
}

data class IpResponse(val ip: String)

data class TimeResponse(val time: String)

data class NameResponse(val first: String, val last: String)

object UserClient {
  private val rf =
          Retrofit.Builder()
                  .baseUrl(BuildConfig.API_BASE_URL)
                  .addConverterFactory(GsonConverterFactory.create())
                  .build()

  val api: UserApi = rf.create(UserApi::class.java)
}

class UserSession(private val api: UserApi, val id: String, val user: User) {
  private val req = UserRequest(id)

  suspend fun getIp(): String {
    return api.getIp(req).ip
  }

  suspend fun getTime(): String {
    return api.getTime(req).time
  }

  suspend fun getName(): NameResponse {
    return api.getName(req)
  }
}
