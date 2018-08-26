package com.pierdr.tramontana.io

import android.content.Context
import android.net.wifi.WifiManager
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteOrder

fun wifiIpAddress(context: Context): String? {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val connectionInfo = wifiManager.connectionInfo
    val ipNetworkByteOrder = connectionInfo?.ipAddress
    if (ipNetworkByteOrder == null || ipNetworkByteOrder == 0) return null
    val ipNativeOrder = ipNetworkByteOrder.bigToNativeOrder()
    val ipLong = ipNativeOrder.toLong()
    val ipBigInteger = ipLong.toBigInteger()
    val ipByteArray = ipBigInteger.toByteArray()
    val inetAddress = ipByteArray.toInetAddress()
    return inetAddress.hostAddress
}

private fun Int.bigToNativeOrder(): Int {
    return if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) Integer.reverseBytes(this) else this
}

private fun Long.toBigInteger(): BigInteger {
    return BigInteger.valueOf(this)
}

private fun ByteArray.toInetAddress(): InetAddress {
    return InetAddress.getByAddress(this)
}