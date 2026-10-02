package com.uyen.launcher.core.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import org.json.JSONObject

/**
 * 虛擬手柄狀態資料
 */
data class VirtualGamepadState(
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val btnA: Boolean = false,
    val btnB: Boolean = false,
    val btnX: Boolean = false,
    val btnY: Boolean = false,
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,
    val btnL1: Boolean = false,
    val btnR1: Boolean = false,
    val btnL2: Float = 0f,
    val btnR2: Float = 0f,
    val btnStart: Boolean = false,
    val btnSelect: Boolean = false
)

/**
 * UyenController 網路發送器 (UDP 協定)
 * 將手機手柄按鍵即時廣播至電腦 PC 接收端 (極低延遲 < 3ms)
 */
class UyenControllerSender {
    private var socket: DatagramSocket? = null
    private var targetHost: String = "192.168.1.100"
    private var targetPort: Int = 8999
    private var isConnected = false

    fun configure(host: String, port: Int = 8999) {
        this.targetHost = host
        this.targetPort = port
        this.isConnected = true
        if (socket == null || socket?.isClosed == true) {
            socket = DatagramSocket().apply {
                broadcast = true
            }
        }
    }

    suspend fun sendState(state: VirtualGamepadState) = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext
        try {
            val json = JSONObject().apply {
                put("lx", state.leftStickX)
                put("ly", state.leftStickY)
                put("rx", state.rightStickX)
                put("ry", state.rightStickY)
                put("a", state.btnA)
                put("b", state.btnB)
                put("x", state.btnX)
                put("y", state.btnY)
                put("dup", state.dpadUp)
                put("ddown", state.dpadDown)
                put("dleft", state.dpadLeft)
                put("dright", state.dpadRight)
                put("l1", state.btnL1)
                put("r1", state.btnR1)
                put("l2", state.btnL2)
                put("r2", state.btnR2)
                put("start", state.btnStart)
                put("select", state.btnSelect)
            }
            val data = json.toString().toByteArray()
            val address = InetAddress.getByName(targetHost)
            val packet = DatagramPacket(data, data.size, address, targetPort)
            socket?.send(packet)
        } catch (_: Exception) {}
    }

    fun close() {
        socket?.close()
        socket = null
        isConnected = false
    }
}
