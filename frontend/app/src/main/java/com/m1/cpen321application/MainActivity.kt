package com.m1.cpen321application

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.m1.cpen321application.ui.theme.CPEN321ApplicationTheme
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                  session = session,
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
fun Greeting(
        apiBaseUrl: String,
        modifier: Modifier = Modifier,
        session: AuthSession,
        isIn: Boolean
) {
  var statusText by remember { mutableStateOf("Checking backend at $apiBaseUrl/health...") }

  var page by remember { mutableIntStateOf(1) }

  val scope = rememberCoroutineScope()

  val context = LocalContext.current

  LaunchedEffect(apiBaseUrl) { statusText = fetchHealthStatus(apiBaseUrl) }

  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    Text(text = statusText)

    Spacer(modifier = Modifier.height(8.dp))
    Text(text = "${isIn}")

    Spacer(modifier = Modifier.height(24.dp))

    if (isIn) {
      Text("Welcome")

      Spacer(modifier = Modifier.height(8.dp))

      Button(onClick = { page = 1 }) { Text("1") }
      Button(onClick = { page = 2 }) { Text("2") }
      Button(onClick = { page = 3 }) { Text("3") }

      Spacer(modifier = Modifier.height(24.dp))

      when (page) {
        1 -> Text("sessionId: ${session.get()}")
        2 -> Text("user: ${session.getUser()}")
        3 -> Text("Some 3")
      }
    } else {
      Button(
              onClick = {
                scope.launch {
                  try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(session.getGoogleUrl()))
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
