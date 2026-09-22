package com.m1.cpen321application

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import okhttp3.OkHttpClient
import okhttp3.Request

fun getWsClient(): OkHttpClient {

  return OkHttpClient()
          .newBuilder()
          // .connectTimeout(10, TimeUnit.SECONDS)
          // .readTimeout(0, TimeUnit.SECONDS)
          // .pingInterval(20, TimeUnit.SECONDS)
          .build()
}

fun getWsRequest(): Request {
  // val wsRequest = Request.Builder().url(BuildConfig.WS_BASE_URL).build()
  return Request.Builder().url("ws://https://cpen321m1-apfxdwdfa7cpf9eq.canadacentral-01.azurewebsites.net").build()
}

