package com.flipperdevices.bridge.impl.manager

import com.flipperdevices.bridge.impl.utils.ByteEndlessInputStream
import com.flipperdevices.core.utils.newSingleThreadExecutor
import com.flipperdevices.protobuf.Flipper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

const val TAG = "PeripheralResponseReader"

@Suppress("BlockingMethodInNonBlockingContext")
class PeripheralResponseReader(
    private val scope: CoroutineScope
) {
    private val byteInputStream = ByteEndlessInputStream()
    private val responses = MutableSharedFlow<Flipper.Main>()
    private val responseReaderDispatcher = newSingleThreadExecutor(TAG)
        .asCoroutineDispatcher()

    init {
        scope.launch {
            withContext(responseReaderDispatcher) {
                while (this.isActive) {
                    try {
                        val main = Flipper.Main.parseDelimitedFrom(byteInputStream)
                        scope.launch {
                            responses.emit(main)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun onReceiveBytes(byteArray: ByteArray) {
        Timber.i("Receive proto array with size: ${byteArray.size}")
        byteInputStream.write(byteArray)
    }

    fun getResponses(): Flow<Flipper.Main> = responses
}
