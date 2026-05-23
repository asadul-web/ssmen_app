package com.v2ray.ang.util

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Log
import androidx.annotation.Keep
import com.v2ray.ang.AppConfig
import kotlinx.coroutines.isActive
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

object SpeedtestUtil {

    private val tcpTestingSockets = ArrayList<Socket?>()

    suspend fun tcping(url: String, port: Int): Long {
        var time = -1L
        for (k in 0 until 2) {
            val one = socketConnectTime(url, port)
            if (!coroutineContext.isActive) {
                break
            }
            if (one != -1L && (time == -1L || one < time)) {
                time = one
            }
        }
        return time
    }

   /* @SuppressLint("LongLogTag")
    fun realPing(config: String): Long {
        return try {
            Libv2ray.measureOutboundDelay(config)
        } catch (e: Exception) {
            Log.d(AppConfig.ANG_PACKAGE, "realPing: $e")
            -1L
        }
    }*/

    @Keep
    @JvmStatic
    fun getPing(url: String,count: String): Long {
        try {
            val command = "/system/bin/ping -c $count $url"
            val process = Runtime.getRuntime().exec(command)
            val allText = process.inputStream.bufferedReader().use { it.readText() }
            if (!TextUtils.isEmpty(allText)) {
                val tempInfo = allText.substring(allText.indexOf("min/avg/max/mdev") + 19)
                val temps = tempInfo.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (temps.isNotEmpty() && temps[0].length < 10) {
                    return temps[0].toFloat().toInt().toLong()
                }
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return -1
    }

    fun ping(url: String): String {
        try {
            val command = "/system/bin/ping -c 3 $url"
            val process = Runtime.getRuntime().exec(command)
            val allText = process.inputStream.bufferedReader().use { it.readText() }
            if (!TextUtils.isEmpty(allText)) {
                val tempInfo = allText.substring(allText.indexOf("min/avg/max/mdev") + 19)
                val temps = tempInfo.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (temps.isNotEmpty() && temps[0].length < 10) {
                    return temps[0].toFloat().toInt().toString() + "ms"
                }
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return "-1ms"
    }

    @SuppressLint("LongLogTag")
    fun socketConnectTime(url: String, port: Int): Long {
        try {
            val socket = Socket()
            synchronized(this) {
                tcpTestingSockets.add(socket)
            }
            val start = System.currentTimeMillis()
            socket.connect(InetSocketAddress(url, port),3000)
            val time = System.currentTimeMillis() - start
            synchronized(this) {
                tcpTestingSockets.remove(socket)
            }
            socket.close()
            return time
        } catch (e: UnknownHostException) {
            //e.printStackTrace()
        } catch (e: IOException) {
            Log.d(AppConfig.ANG_PACKAGE, "socketConnectTime IOException: $e")
        } catch (e: Exception) {
            //e.printStackTrace()
        }
        return -1
    }

    fun closeAllTcpSockets() {
        synchronized(this) {
            tcpTestingSockets.forEach {
                it?.close()
            }
            tcpTestingSockets.clear()
        }
    }



}
