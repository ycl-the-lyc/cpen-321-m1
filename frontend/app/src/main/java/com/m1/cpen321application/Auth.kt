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

val emptyUser = User("", "", "", "")

object AuthClient {
  private val rf =
          Retrofit.Builder()
                  .baseUrl("http://localhost:3000/")
                  .addConverterFactory(GsonConverterFactory.create())
                  .build()

  val authApi: AuthApi = rf.create(AuthApi::class.java)
}

class AuthSession(private val api: AuthApi) {
  private var id: String? = null

  private var user: User = emptyUser

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
  }

  fun isIn(): Boolean {
    return id != null
  }

  fun getUser(): User {
    return user
  }
}

sealed interface AuthState {
  data object Out : AuthState
  data object Load : AuthState
  data object In : AuthState
  data class Err(val msg: String) : AuthState
}

class AuthViewModel(private val session: AuthSession) : ViewModel() {
  private val state_mut =
          MutableStateFlow<AuthState>(
                  if (session.isIn()) {
                    AuthState.In
                  } else {
                    AuthState.Out
                  }
          )

  val state: StateFlow<AuthState> = state_mut.asStateFlow()

  fun google(onUrl: (String) -> Unit) {
    viewModelScope.launch {
      state_mut.value = AuthState.Load

      try {
        val url = session.getGoogleUrl()
        onUrl(url)
        state_mut.value = AuthState.Out
      } catch (e: Exception) {
        state_mut.value = AuthState.Err(e.message ?: "Google Login failed")
      }
    }
  }

  fun doneGoogle(ticket: String) {
    viewModelScope.launch {
      state_mut.value = AuthState.Load

      try {
        session.doneGoogle(ticket)
        state_mut.value = AuthState.In
      } catch (e: Exception) {
        state_mut.value = AuthState.Err(e.message ?: "Google Login failed")
      }
    }
  }

  fun out() {
    session.del()
    state_mut.value = AuthState.Out
  }
}
