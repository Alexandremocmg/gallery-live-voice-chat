/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import kotlinx.coroutines.delay

private const val DISMISS_DELAY_SECONDS = 5

@Composable
fun PromoScreenGm4(onDismiss: () -> Unit) {
  LaunchedEffect(Unit) {
    delay(DISMISS_DELAY_SECONDS * 1000L)
    onDismiss()
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
            brush =
            Brush.verticalGradient(
              colors = listOf(Color(0xFF6A2118), Color(0xFF18110F))
            )
        ),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(0.6f),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier.size(72.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Mic,
          contentDescription = "Kabem Voice",
          modifier = Modifier.size(36.dp),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
      Text(
        stringResource(R.string.introducing),
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
        color = Color.White,
      )
      Text(
        "Gemma 4",
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 38.sp),
        color = Color.White,
      )
      Text(
        "Experience the world’s most capable open models, designed to run frontier-level intelligence directly on your hardware.",
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp, lineHeight = 21.sp),
        textAlign = TextAlign.Center,
        color = Color(0xfff2f2f2),
      )

      // Dismiss button.
      TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 24.dp)) {
        Text(stringResource(R.string.dismiss), color = MaterialTheme.colorScheme.primary)
      }
    }
  }
}
