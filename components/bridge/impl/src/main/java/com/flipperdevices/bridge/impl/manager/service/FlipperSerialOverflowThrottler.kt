package com.flipperdevices.bridge.impl.manager.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.flipperdevices.bridge.api.manager.service.FlipperSerialApi
import com.flipperdevices.bridge.api.utils.Constants
import com.flipperdevices.bridge.impl.manager.UnsafeBleManager
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue
import java.nio.ByteBuffer
import no.nordicsemi.android.ble.data.Data
import timber.log.Timber

class FlipperSerialOverflowThrottler(
    private val serialApi: FlipperSerialApi
) : FlipperSerialApi, BluetoothGattServiceWrapper {
    private var overflowCharacteristics: BluetoothGattCharacteristic? = null

    private var remainBufferSize = 0
    private val outputBuffer = ByteArrayFIFOQueue()

    override fun receiveBytesFlow() = serialApi.receiveBytesFlow()

    override fun sendBytes(data: ByteArray) = synchronized(outputBuffer) {
        if (remainBufferSize == 0) {
            data.forEach { outputBuffer.enqueue(it) }
            return@synchronized
        }

        if (remainBufferSize > data.size) {
            remainBufferSize -= data.size
            serialApi.sendBytes(data)
            return@synchronized
        }

        if (remainBufferSize < data.size) {
            serialApi.sendBytes(data.copyOf(remainBufferSize))
            val pending = data.copyOfRange(remainBufferSize, data.size)
            remainBufferSize = 0
            pending.forEach { outputBuffer.enqueue(it) }
            return@synchronized
        }
    }

    override fun onServiceReceived(service: BluetoothGattService) {
        overflowCharacteristics = service.getCharacteristic(Constants.BLESerialService.OVERFLOW)
    }

    override fun initialize(bleManager: UnsafeBleManager) {
        bleManager.setNotificationCallbackUnsafe(overflowCharacteristics).with { _, data ->
            updateRemainingBuffer(data)
        }
        bleManager.enableNotificationsUnsafe(overflowCharacteristics).enqueue()
        bleManager.enableIndicationsUnsafe(overflowCharacteristics).enqueue()
        bleManager.readCharacteristicUnsafe(overflowCharacteristics).with { _, data ->
            updateRemainingBuffer(data)
        }.enqueue()
    }

    override fun reset(bleManager: UnsafeBleManager) {
        synchronized(outputBuffer) {
            remainBufferSize = 0
        }
        bleManager.readCharacteristicUnsafe(overflowCharacteristics).with { _, data ->
            updateRemainingBuffer(data)
        }.enqueue()
    }

    private fun updateRemainingBuffer(data: Data) {
        Timber.i("Receive remaining buffer info")
        val bytes = data.value ?: return
        // val totalBufferSize = ByteBuffer.wrap(byteArrayOf(0, 0, bytes[0], bytes[1])).int
        val remainingInternal = ByteBuffer.wrap(byteArrayOf(0, 0, bytes[3], bytes[2])).int
        synchronized(outputBuffer) {
            remainBufferSize = remainingInternal
            if (outputBuffer.isEmpty) {
                Timber.i("Output buffer empty")
                return@synchronized
            }

            val pendingSize = Math.min(remainBufferSize, outputBuffer.size())
            val pendingBytes = ByteBuffer.allocate(pendingSize)
            repeat(pendingSize) {
                pendingBytes.put(outputBuffer.dequeueByte())
            }
            serialApi.sendBytes(pendingBytes.array())
            remainBufferSize -= pendingSize
            if (outputBuffer.isEmpty) {
                Timber.i("Output buffer empty")
                return@synchronized
            }
        }
    }
}
