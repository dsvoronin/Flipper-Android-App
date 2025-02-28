package com.flipperdevices.bridge.service.api

import android.bluetooth.BluetoothDevice
import com.flipperdevices.bridge.api.manager.FlipperRequestApi
import com.flipperdevices.bridge.api.manager.delegates.FlipperConnectionInformationApi
import com.flipperdevices.bridge.api.manager.service.FlipperInformationApi

/**
 * Provides access to the API operation of the device
 * Underhood creates a service and connects to it
 *
 * You can get instance by FlipperServiceProvider
 */
interface FlipperServiceApi {

    /**
     * Provide information about flipper name, device id
     */
    val flipperInformationApi: FlipperInformationApi

    /**
     * Provide information about current connection state
     */
    val connectionInformationApi: FlipperConnectionInformationApi

    /**
     * Returns an API for communicating with Flipper via a request-response structure.
     */
    val requestApi: FlipperRequestApi

    suspend fun reconnect(deviceId: String)

    suspend fun reconnect(device: BluetoothDevice)
}
