package com.m1.cpen321application

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.m1.cpen321application.ui.theme.CPEN321ApplicationTheme
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.URL
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class MainActivity : ComponentActivity() {
  private val session = AuthSession(AuthClient.authApi)
  private var isIn by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CPEN321ApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Greeting(
                  apiBaseUrl = BuildConfig.API_BASE_URL,
                  modifier = Modifier.padding(innerPadding),
                  auth = session,
                  isIn = isIn
          )
        }
      }
    }

    handleTicket(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleTicket(intent)
  }

  private fun handleTicket(intent: Intent?) {
    val ticket = intent?.data?.getQueryParameter("ticket") ?: return

    lifecycleScope.launch {
      session.doneGoogle(ticket)
      isIn = session.isIn()
    }
  }
}

@Composable
fun Greeting(apiBaseUrl: String, modifier: Modifier = Modifier, auth: AuthSession, isIn: Boolean) {
  val status = remember { mutableStateOf("Checking backend at $apiBaseUrl/health...") }

  var statusText by status

  var page by remember { mutableIntStateOf(0) }

  val scope = rememberCoroutineScope()

  val context = LocalContext.current

  var localIp by remember { mutableStateOf("") }
  var serverIp by remember { mutableStateOf("") }
  var serverTime by remember { mutableStateOf("") }
  var devFirstName by remember { mutableStateOf("") }
  var devLastName by remember { mutableStateOf("") }

  suspend fun reloadInfo(session: UserSession) {
    statusText = "Getting info..."

    try {
      serverIp = session.getIp()
      localIp = getLocalIp(context)
      serverTime = session.getTime()
      val devName = session.getName()
      devFirstName = devName.first
      devLastName = devName.last
      statusText = "Getting info: done"
    } catch (e: Exception) {
      statusText = "Getting info: ${e.message ?: "failed"}"
    }
  }

  LaunchedEffect(apiBaseUrl) { statusText = fetchHealthStatus(apiBaseUrl) }

  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    Text(text = statusText)

    Spacer(modifier = Modifier.height(24.dp))

    if (isIn) {
      val session: UserSession =
              UserSession(UserClient.api, auth.get() as String, auth.getUser() as User)

      Button(
              onClick = {
                page = 1
                scope.launch { reloadInfo(session) }
              }
      ) { Text("Info") }

      Button(onClick = { page = 2 }) { Text("Pixel Art") }

      Button(onClick = { page = 3 }) { Text("Timer") }

      Spacer(modifier = Modifier.height(16.dp))

      when (page) {
        0 -> Text("Press a button")
        1 ->
                Text(
                        """
  Server IP address: ${serverIp}
  Client IP address: ${localIp}

  Server local time: ${serverTime}
  Client local time: ${LocalTime.now()}

  Developer: ${devFirstName} ${devLastName}
  """.trimIndent()
                )
        2 -> {
          val mainHandler = remember { Handler(Looper.getMainLooper()) }

          val wsClient = remember { getWsClient() }

          val pixels = remember { List(16 * 16) { mutableStateOf(Color(0x000000)) } }

          var artText by remember { mutableStateOf("") }

          DisposableEffect(Unit) {
            val wsRequest = getWsRequest()
            val ws =
                    wsClient.newWebSocket(
                            wsRequest,
                            object : WebSocketListener() {
                              override fun onMessage(webSocket: WebSocket, text: String) {
                                val jo = JSONObject(text)
                                val x = jo.getInt("x")
                                val y = jo.getInt("y")
                                val color =
                                        Color(jo.getString("color").removePrefix("#").toULong(16))

                                mainHandler.post {
                                  pixels[y * 16 + x].value = color
                                  artText = "Got: ${color} at (${x}, ${y})"
                                }
                              }

                              override fun onOpen(webSocket: WebSocket, response: Response) {
                                statusText = "WebSocket connected"
                                artText = "Hello"
                              }

                              override fun onClosed(
                                      webSocket: WebSocket,
                                      code: Int,
                                      reason: String
                              ) {
                                statusText = "WebSocket closed: ${code} (${reason})"
                                artText = "..."
                              }

                              override fun onFailure(
                                      webSocket: WebSocket,
                                      t: Throwable,
                                      response: Response?
                              ) {
                                statusText = "WebSocket failed: ${t.message?: ""}"
                              }
                            }
                    )

            onDispose { ws.close(1000, "leaving pixel art tab") }
          }

          Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("Art: ${artText}")

            Spacer(Modifier.height(8.dp))

            Canvas(modifier = modifier.fillMaxSize()) {
              val w = size.width / 16f
              val h = size.height / 16f

              for (y in 0..15) for (x in 0..15) {
                drawRect(pixels[y * 16 + x].value, Offset(x * w, y * h), Size(w, h))
              }
            }
          }
        }
        3 -> Text("Some 3")
      }
    } else {
      Button(
              onClick = {
                scope.launch {
                  try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(auth.getGoogleUrl()))
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    statusText = "Login failed: ${e.message}"
                  }
                }
              }
      ) { Text("Login with Google") }
    }
  }
}

private suspend fun getLocalIp(context: Context): String {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val net = cm.activeNetwork
  val link = cm.getLinkProperties(net)
  return link?.linkAddresses
          ?.map { it.address }
          ?.filter { !it.isLoopbackAddress }
          ?.firstOrNull { it is Inet4Address || it is Inet6Address }
          ?.hostAddress
          ?: ""
}

private suspend fun fetchHealthStatus(apiBaseUrl: String): String =
        withContext(Dispatchers.IO) {
          val healthUrl = "${apiBaseUrl.trimEnd('/')}/health"
          try {
            val connection =
                    (URL(healthUrl).openConnection() as HttpURLConnection).apply {
                      requestMethod = "GET"
                      connectTimeout = 5_000
                      readTimeout = 5_000
                    }

            when (val code = connection.responseCode) {
              HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                "Backend healthy ($healthUrl): $body"
              }
              else -> {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "Backend error ($healthUrl): HTTP $code${errorBody?.let { " — $it" } ?: ""}"
              }
            }
          } catch (e: Exception) {
            "Backend unreachable ($healthUrl): ${e.message ?: e.javaClass.simpleName}"
          }
        }
