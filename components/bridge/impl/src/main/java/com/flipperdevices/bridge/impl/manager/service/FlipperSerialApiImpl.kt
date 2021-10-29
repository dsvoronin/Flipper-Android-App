package com.flipperdevices.bridge.impl.manager.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.flipperdevices.bridge.api.manager.service.FlipperSerialApi
import com.flipperdevices.bridge.api.utils.Constants
import com.flipperdevices.bridge.impl.manager.UnsafeBleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class FlipperSerialApiImpl(
    private val scope: CoroutineScope
) : FlipperSerialApi, BluetoothGattServiceWrapper {
    private val receiveBytesFlow = MutableSharedFlow<ByteArray>()

    // Store bytes which pending for sending to Flipper Zero device
    private val pendingBytes = mutableListOf<ByteArray>()

    private var bleManagerInternal: UnsafeBleManager? = null

    private var serialTxCharacteristic: BluetoothGattCharacteristic? = null
    private var serialRxCharacteristic: BluetoothGattCharacteristic? = null

    override fun onServiceReceived(service: BluetoothGattService) {
        serialTxCharacteristic = service.getCharacteristic(Constants.BLESerialService.TX)
        serialRxCharacteristic = service.getCharacteristic(Constants.BLESerialService.RX)
    }

    override fun initialize(bleManager: UnsafeBleManager) {
        bleManagerInternal = bleManager
        bleManager.setNotificationCallbackUnsafe(serialRxCharacteristic).with { _, data ->
            Timber.i("Receive serial data")
            val bytes = data.value ?: return@with
            scope.launch {
                receiveBytesFlow.emit(bytes)
            }
        }
        bleManager.enableNotificationsUnsafe(serialRxCharacteristic).enqueue()
        bleManager.enableIndicationsUnsafe(serialRxCharacteristic).enqueue()
        pendingBytes.forEach { data ->
            bleManager.writeCharacteristicUnsafe(serialTxCharacteristic, data).enqueue()
        }
    }

    override fun reset(bleManager: UnsafeBleManager) {
        // Not exist states in this api
    }

    override fun receiveBytesFlow() = receiveBytesFlow

    override fun sendBytes(data: ByteArray) {
        val bleManager = bleManagerInternal
        if (bleManager == null) {
            pendingBytes.add(data)
            return
        }
        if (data.size > 200) {
            var endIndex = 0
            var startIndex = 0
            do {
                startIndex = endIndex
                endIndex = Math.min(endIndex + 200, data.size)
                bleManager.writeCharacteristicUnsafe(
                    serialTxCharacteristic,
                    data.copyOfRange(startIndex, endIndex)
                ).enqueue()
            } while (endIndex != data.size)
        }
        bleManager.writeCharacteristicUnsafe(
            serialTxCharacteristic,
            data
        ).enqueue()
    }
}
