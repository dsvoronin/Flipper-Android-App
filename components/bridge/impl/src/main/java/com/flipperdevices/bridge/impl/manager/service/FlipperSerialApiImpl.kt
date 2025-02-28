package com.flipperdevices.bridge.impl.manager.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.flipperdevices.bridge.api.manager.service.FlipperSerialApi
import com.flipperdevices.bridge.api.model.FlipperSerialSpeed
import com.flipperdevices.bridge.api.utils.Constants
import com.flipperdevices.bridge.impl.manager.UnsafeBleManager
import com.flipperdevices.bridge.impl.utils.SpeedMeter
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.core.log.info
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlipperSerialApiImpl(
    private val scope: CoroutineScope
) : FlipperSerialApi, BluetoothGattServiceWrapper, LogTagProvider {
    override val TAG = "FlipperSerialApi"
    private val receiveBytesFlow = MutableSharedFlow<ByteArray>()

    // Store bytes which pending for sending to Flipper Zero device
    private val pendingBytes = mutableListOf<ByteArray>()

    private var bleManagerInternal: UnsafeBleManager? = null

    private var serialTxCharacteristic: BluetoothGattCharacteristic? = null
    private var serialRxCharacteristic: BluetoothGattCharacteristic? = null

    private val txSpeed = SpeedMeter()
    private val rxSpeed = SpeedMeter()

    override fun onServiceReceived(service: BluetoothGattService) {
        serialTxCharacteristic = service.getCharacteristic(Constants.BLESerialService.TX)
        serialRxCharacteristic = service.getCharacteristic(Constants.BLESerialService.RX)
    }

    override fun initialize(bleManager: UnsafeBleManager) {
        bleManagerInternal = bleManager
        bleManager.setNotificationCallbackUnsafe(serialRxCharacteristic).with { _, data ->
            info { "Receive serial data ${data.value?.size}" }
            val bytes = data.value ?: return@with
            rxSpeed.onReceiveBytes(bytes.size)
            scope.launch {
                receiveBytesFlow.emit(bytes)
            }
        }
        bleManager.enableNotificationsUnsafe(serialRxCharacteristic).enqueue()
        bleManager.enableIndicationsUnsafe(serialRxCharacteristic).enqueue()
        pendingBytes.forEach { data ->
            bleManager.writeCharacteristicUnsafe(serialTxCharacteristic, data)
                .done {
                    txSpeed.onReceiveBytes(data.size)
                }.enqueue()
        }
    }

    override fun reset(bleManager: UnsafeBleManager) {
        // Not exist states in this api
    }

    override fun receiveBytesFlow() = receiveBytesFlow
    override suspend fun getSpeed() = rxSpeed.getSpeed()
        .combine(txSpeed.getSpeed()) { rxBPS, txBPS ->
            FlipperSerialSpeed(receiveBytesInSec = rxBPS, transmitBytesInSec = txBPS)
        }.stateIn(scope)

    override fun sendBytes(data: ByteArray) {
        if (data.isEmpty()) {
            return
        }
        info { "Send bytes to flipper with size: ${data.size}" }
        val bleManager = bleManagerInternal
        if (bleManager == null) {
            pendingBytes.add(data)
            return
        }
        bleManager.writeCharacteristicUnsafe(serialTxCharacteristic, data)
            .split(FixedSizeDataSplitter())
            .done {
                txSpeed.onReceiveBytes(data.size)
            }
            .enqueue()
    }
}
