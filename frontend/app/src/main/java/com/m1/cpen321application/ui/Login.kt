package com.m1.cpen321application.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.m1.cpen321application.AuthState
import com.m1.cpen321application.AuthViewModel

fun Login(viewModel: AuthViewModel) {
  val state by viewModel.state.collectAsState()

  val context = LocalContext.current

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (val currect = state) {
      AuthState.Out -> {}
      AuthState.Load -> CircularProgressIndicator()
      is AuthState.Err -> {
        Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
        ) {}
      }
      AuthState.In -> {}
    }
  }
}
