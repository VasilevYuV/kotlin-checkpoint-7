package app.server

import app.controller.RouterController
import app.util.RequestLogger
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors

class MiniHttpServer(
    private val port: Int,
    private val routerController: RouterController,
    private val requestLogger: RequestLogger
) {
    private val executor = Executors.newCachedThreadPool()

    fun start() {
        val serverSocket = ServerSocket(port)
        while (true) {
            try {
                val client = serverSocket.accept()
                executor.submit { handleClient(client) }
            } catch (_: Exception) {
                // Keep server alive even if one accept iteration fails.
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 10_000
            try {
                val request = HttpParser.parse(client.getInputStream()) ?: return
                val result = routerController.handle(request)
                requestLogger.log(request.method, request.path, result.userForLog)

                val output = client.getOutputStream()
                output.write(result.response.toBytes())
                output.flush()
            } catch (_: SocketException) {
                // Ignore broken pipe and abrupt disconnects.
            } catch (_: Exception) {
                try {
                    val output = client.getOutputStream()
                    output.write(
                        HttpResponse(
                            statusCode = 500,
                            statusText = "Internal Server Error",
                            body = """{"error":"Internal server error"}"""
                        ).toBytes()
                    )
                    output.flush()
                } catch (_: Exception) {
                    // Connection can already be closed by client.
                }
            }
        }
    }
}
