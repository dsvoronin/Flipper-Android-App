package com.flipperdevices.screenstreaming.impl.composable

import android.text.format.Formatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flipperdevices.screenstreaming.impl.R
import com.flipperdevices.screenstreaming.impl.model.StreamingState
import com.flipperdevices.screenstreaming.impl.viewmodel.FLIPPER_SCREEN_RATIO
import com.flipperdevices.screenstreaming.impl.viewmodel.ScreenStreamingViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ComposableScreen(
    viewModel: ScreenStreamingViewModel = viewModel(),
    onPressButton: (ButtonEnum) -> Unit = {},
    onLongPressButton: (ButtonEnum) -> Unit = {},
    onScreenStreamingSwitch: (StreamingState) -> Unit = {}
) {
    val flipperScreen by viewModel.getFlipperScreen().collectAsState()
    val streamingState by viewModel.getStreamingState().collectAsState()
    val speedState by viewModel.getSpeed().collectAsState()

    Column() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp)
                .weight(weight = 1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val imageDrawPaint = remember {
                Paint().apply {
                    filterQuality = FilterQuality.None
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(FLIPPER_SCREEN_RATIO),
                contentDescription = stringResource(R.string.flipper_display)
            ) {
                drawContext.canvas.drawImageRect(
                    image = flipperScreen.asImageBitmap(),
                    dstSize = IntSize(
                        size.width.roundToInt(),
                        size.height.roundToInt()
                    ),
                    paint = imageDrawPaint
                )
            }
            ComposableControlButtons(onPressButton, onLongPressButton)
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onScreenStreamingSwitch(
                        when (streamingState) {
                            StreamingState.DISABLED -> StreamingState.ENABLED
                            StreamingState.ENABLED -> StreamingState.DISABLED
                        }
                    )
                }
            ) {
                Text(
                    text = stringResource(
                        id = when (streamingState) {
                            StreamingState.DISABLED -> R.string.screen_streaming_enable
                            StreamingState.ENABLED -> R.string.screen_streaming_disable
                        }
                    ),
                )
            }
        }
        Column {
            val rx = Formatter.formatFileSize(LocalContext.current, speedState.receiveBytesInSec)
            val tx = Formatter.formatFileSize(LocalContext.current, speedState.transmitBytesInSec)
            Text(stringResource(R.string.screen_streaming_speed_receive, rx))
            Text(stringResource(R.string.screen_streaming_speed_send, tx))
        }
    }
}
