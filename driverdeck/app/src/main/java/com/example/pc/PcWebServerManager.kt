package com.example.pc

import android.content.Context
import android.util.Log
import com.example.data.GarageRepository
import com.example.model.IconStyle
import com.example.model.LogRecord
import com.example.model.RecordType
import com.example.model.ReminderTask
import com.example.model.ReminderTriggerType
import com.example.model.TaskPriority
import com.example.model.Vehicle
import com.example.model.VehicleClass
import com.example.model.normalizeHex
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.random.Random

data class PcServerState(
    val running: Boolean = false,
    val url: String = "",
    val pin: String = "",
    val error: String? = null
)

object PcWebServerManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var currentPin: String = ""

    private val _state = MutableStateFlow(PcServerState())
    val state: StateFlow<PcServerState> = _state.asStateFlow()

    fun start(context: Context) {
        if (_state.value.running) return
        val appContext = context.applicationContext
        val repository = GarageRepository(appContext)
        currentPin = Random.nextInt(1000, 9999).toString()

        acceptJob = scope.launch {
            var boundSocket: ServerSocket? = null
            for (port in 8080..8089) {
                try {
                    boundSocket = ServerSocket(port)
                    break
                } catch (_: Exception) {
                }
            }
            if (boundSocket == null) {
                _state.value = PcServerState(running = false, error = "Не удалось открыть локальный порт 8080-8089")
                return@launch
            }

            serverSocket = boundSocket
            val ip = resolveLocalIpAddress() ?: "127.0.0.1"
            val url = "http://$ip:${boundSocket.localPort}"
            _state.value = PcServerState(running = true, url = url, pin = currentPin, error = null)

            while (isActive && !boundSocket.isClosed) {
                try {
                    val client = boundSocket.accept()
                    launch { handleClient(client, repository) }
                } catch (_: Exception) {
                    if (boundSocket.isClosed) break
                }
            }
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptJob?.cancel()
        acceptJob = null
        currentPin = ""
        _state.value = PcServerState(running = false)
    }

    private fun handleClient(socket: Socket, repository: GarageRepository) {
        socket.use { sock ->
            sock.soTimeout = 10_000
            val input = BufferedInputStream(sock.getInputStream())
            val rawHeaderBytes = readHeaders(input) ?: return
            val headerText = rawHeaderBytes.toString(Charsets.UTF_8)
            val lines = headerText.split("\r\n")
            if (lines.isEmpty()) return

            val requestParts = lines[0].split(" ")
            if (requestParts.size < 2) return
            val method = requestParts[0].uppercase()
            val fullPath = requestParts[1]
            val path = fullPath.substringBefore("?")
            val query = fullPath.substringAfter("?", "")

            val headers = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val idx = lines[i].indexOf(":")
                if (idx > 0) {
                    headers[lines[i].substring(0, idx).trim().lowercase()] =
                        lines[i].substring(idx + 1).trim()
                }
            }

            val contentLength = headers["content-length"]?.toIntOrNull()?.coerceIn(0, 1_000_000) ?: 0
            val bodyBytes = if (contentLength > 0) readFixedBytes(input, contentLength) else ByteArray(0)
            val bodyStr = bodyBytes.toString(Charsets.UTF_8)

            val output = sock.getOutputStream()

            if (method == "GET" && (path == "/" || path == "/index.html")) {
                writeResponse(output, 200, "text/html; charset=utf-8", PcWebHtml.buildHtml())
                return
            }

            if (path.startsWith("/api/")) {
                val reqPin = headers["x-drivedeck-pin"] ?: ""
                if (currentPin.isBlank() || reqPin != currentPin) {
                    writeResponse(output, 401, "application/json; charset=utf-8", """{"error":"Unauthorized"}""")
                    return
                }

                val uid = Firebase.auth.currentUser?.uid
                if (uid.isNullOrBlank()) {
                    writeResponse(output, 403, "application/json; charset=utf-8", """{"error":"No signed-in user"}""")
                    return
                }

                val responsePair = runBlocking {
                    dispatchApi(method, path, query, bodyStr, uid, repository)
                }
                writeResponse(output, responsePair.first, "application/json; charset=utf-8", responsePair.second)
                return
            }

            writeResponse(output, 404, "text/plain; charset=utf-8", "Not Found")
        }
    }

    private suspend fun dispatchApi(
        method: String,
        path: String,
        query: String,
        body: String,
        uid: String,
        repo: GarageRepository
    ): Pair<Int, String> {
        return try {
            when {
                method == "GET" && path == "/api/state" -> {
                    val snap = repo.fetchSnapshotOnce(uid).getOrThrow()
                    val root = JSONObject().apply {
                        put("vehicles", JSONArray().apply {
                            snap.first.forEach { v ->
                                put(JSONObject().apply {
                                    put("id", v.id)
                                    put("name", v.displayTitle())
                                    put("brand", v.brand)
                                    put("model", v.model)
                                    put("year", v.year)
                                    put("vehicleClass", v.safeVehicleClass().name)
                                    put("vehicleClassLabel", v.safeVehicleClass().labelRu)
                                    put("iconStyle", v.safeIconStyle().name)
                                    put("accentColorHex", v.accentColorHex)
                                    put("odometerKm", v.odometerKm)
                                    put("fuelType", v.fuelType)
                                })
                            }
                        })
                        put("records", JSONArray().apply {
                            snap.second.forEach { r ->
                                put(JSONObject().apply {
                                    put("id", r.id)
                                    put("vehicleId", r.vehicleId)
                                    put("recordType", r.safeRecordType().name)
                                    put("title", r.title)
                                    put("categoryTag", r.categoryTag)
                                    put("amountCost", r.amountCost)
                                    put("odometerKm", r.odometerKm)
                                    put("liters", r.liters)
                                    put("notes", r.notes)
                                    put("eventTimestampMs", r.eventTimestampMs)
                                })
                            }
                        })
                        put("reminders", JSONArray().apply {
                            snap.third.forEach { t ->
                                put(JSONObject().apply {
                                    put("id", t.id)
                                    put("vehicleId", t.vehicleId)
                                    put("title", t.title)
                                    put("notes", t.notes)
                                    put("triggerType", t.safeTriggerType().name)
                                    put("dueDateEpochMs", t.dueDateEpochMs)
                                    put("dueOdometerKm", t.dueOdometerKm)
                                    put("priority", t.safePriority().name)
                                    put("completed", t.completed)
                                })
                            }
                        })
                    }
                    200 to root.toString()
                }

                method == "POST" && path == "/api/vehicles" -> {
                    val o = JSONObject(body)
                    val cls = VehicleClass.fromRaw(o.optString("vehicleClass"))
                    val veh = Vehicle(
                        id = o.optString("id"),
                        name = o.optString("name", "Транспорт"),
                        brand = o.optString("brand"),
                        model = o.optString("model"),
                        year = o.optLong("year", 2022L),
                        category = cls.category.name,
                        vehicleClass = cls.name,
                        iconStyle = IconStyle.fromRaw(o.optString("iconStyle")).name,
                        accentColorHex = normalizeHex(o.optString("accentColorHex", "#1D63DC")),
                        odometerKm = o.optLong("odometerKm", 0L),
                        fuelType = o.optString("fuelType", "АИ-95")
                    )
                    val id = repo.saveVehicle(uid, veh).getOrThrow()
                    200 to JSONObject().put("id", id).toString()
                }

                method == "POST" && path == "/api/records" -> {
                    val o = JSONObject(body)
                    val rType = RecordType.fromRaw(o.optString("recordType"))
                    val rec = LogRecord(
                        id = o.optString("id"),
                        vehicleId = o.optString("vehicleId"),
                        recordType = rType.name,
                        title = o.optString("title", rType.labelRu),
                        categoryTag = o.optString("categoryTag", rType.labelRu),
                        amountCost = o.optDouble("amountCost", 0.0),
                        odometerKm = o.optLong("odometerKm", 0L),
                        liters = o.optDouble("liters", 0.0),
                        notes = o.optString("notes"),
                        eventTimestampMs = o.optLong("eventTimestampMs", System.currentTimeMillis())
                    )
                    val id = repo.saveRecord(uid, rec).getOrThrow()
                    200 to JSONObject().put("id", id).toString()
                }

                method == "POST" && path == "/api/reminders" -> {
                    val o = JSONObject(body)
                    val task = ReminderTask(
                        id = o.optString("id"),
                        vehicleId = o.optString("vehicleId"),
                        title = o.optString("title", "Задача"),
                        notes = o.optString("notes"),
                        triggerType = ReminderTriggerType.fromRaw(o.optString("triggerType")).name,
                        dueDateEpochMs = o.optLong("dueDateEpochMs", 0L),
                        dueOdometerKm = o.optLong("dueOdometerKm", 0L),
                        priority = TaskPriority.fromRaw(o.optString("priority")).name
                    )
                    val id = repo.saveReminder(uid, task).getOrThrow()
                    200 to JSONObject().put("id", id).toString()
                }

                method == "POST" && path == "/api/reminders/toggle" -> {
                    val o = JSONObject(body)
                    repo.toggleReminderCompletedById(uid, o.getString("id"), o.getBoolean("completed")).getOrThrow()
                    200 to """{"ok":true}"""
                }

                method == "DELETE" && path == "/api/records" -> {
                    val id = parseQueryParam(query, "id")
                    repo.deleteRecord(uid, id).getOrThrow()
                    200 to """{"ok":true}"""
                }

                method == "DELETE" && path == "/api/reminders" -> {
                    val id = parseQueryParam(query, "id")
                    repo.deleteReminder(uid, id).getOrThrow()
                    200 to """{"ok":true}"""
                }

                else -> 404 to """{"error":"Unknown route"}"""
            }
        } catch (e: Exception) {
            Log.w("PcWebServer", "API error on $path", e)
            400 to JSONObject().put("error", e.message ?: "Request failed").toString()
        }
    }

    private fun parseQueryParam(query: String, key: String): String {
        return query.split("&")
            .mapNotNull {
                val p = it.split("=", limit = 2)
                if (p.size == 2 && p[0] == key) URLDecoder.decode(p[1], "UTF-8") else null
            }
            .firstOrNull() ?: ""
    }

    private fun readHeaders(input: BufferedInputStream): ByteArray? {
        val buf = ArrayList<Byte>(512)
        var matched = 0
        while (buf.size < 16384) {
            val b = input.read()
            if (b == -1) return null
            buf.add(b.toByte())
            matched = when {
                (matched == 0 || matched == 2) && b == '\r'.code -> matched + 1
                (matched == 1 || matched == 3) && b == '\n'.code -> matched + 1
                else -> if (b == '\r'.code) 1 else 0
            }
            if (matched == 4) break
        }
        return buf.toByteArray()
    }

    private fun readFixedBytes(input: BufferedInputStream, length: Int): ByteArray {
        val result = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(result, offset, length - offset)
            if (count <= 0) break
            offset += count
        }
        return if (offset == length) result else result.copyOf(offset)
    }

    private fun writeResponse(out: OutputStream, status: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val statusText = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            else -> "Not Found"
        }
        val headers = "HTTP/1.1 $status $statusText\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        out.write(headers.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun resolveLocalIpAddress(): String? {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            interfaces
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress
        }.getOrNull()
    }
}
