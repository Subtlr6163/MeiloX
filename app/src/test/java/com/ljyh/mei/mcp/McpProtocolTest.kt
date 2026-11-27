package com.ljyh.mei.mcp

import com.google.gson.JsonObject
import com.ljyh.mei.source.api.MusicProvider
import com.ljyh.mei.source.api.SourceCapability
import com.ljyh.mei.source.core.ProviderRegistry
import com.ljyh.mei.source.core.SourceMutationAuthorization
import com.ljyh.mei.source.core.SourceMutationCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.net.Socket

class McpProtocolTest {
    @Test
    fun initializationAndNotificationsUseJsonRpcIds() = runBlocking {
        val protocol = protocol()
        val request = """{"jsonrpc":"2.0","id":"init","method":"initialize","params":{"protocolVersion":"unsupported"}}"""
        val response = protocol.handle(request).message!!
        assertEquals("init", response.get("id").asString)
        assertEquals(McpHandshakeVersion, response.getAsJsonObject("result").get("protocolVersion").asString)
        assertEquals(McpLegacyVersion, protocol.handle(request, legacySse = true).message!!
            .getAsJsonObject("result").get("protocolVersion").asString)
        assertNull(protocol.handle("""{"jsonrpc":"2.0","method":"notifications/initialized"}""").message)
        assertNull(protocol.handle("""{"jsonrpc":"2.0","method":"tools/call","params":{"name":"skip_next"}}""").message)
        assertEquals(-32600, protocol.handle("""{"jsonrpc":"2.0","id":null,"method":"ping"}""").message!!.errorCode())
        assertEquals(-32700, protocol.handle("""{'jsonrpc':'2.0'}""").message!!.errorCode())
    }

    @Test
    fun revokedToolsAreHiddenAndCannotBeCalledDirectly() = runBlocking {
        val permissions = mutableSetOf(McpPermission.Read, McpPermission.Playback)
        val calls = mutableListOf<String>()
        val protocol = protocol(permissions::contains) { tool, _ ->
            calls += tool.name
            JsonObject()
        }
        val listed = protocol.handle("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""").message!!
            .getAsJsonObject("result").getAsJsonArray("tools").map { it.asJsonObject.get("name").asString }
        assertEquals(18, listed.size)
        assertTrue("skip_next" in listed)
        assertFalse("create_playlist" in listed)
        assertEquals(-32602, protocol.handle(call("like_track", """{"trackId":"123","liked":true}""")).message!!.errorCode())
        assertEquals(-32602, protocol.handle(call("set_volume", """{"percent":"100"}""")).message!!.errorCode())
        assertEquals(-32602, protocol.handle(call("set_volume", """{"percent":101}""")).message!!.errorCode())
        assertFalse(protocol.handle(call("set_volume", """{"percent":100}""")).message!!
            .getAsJsonObject("result").get("isError").asBoolean)
        permissions.remove(McpPermission.Playback)
        assertEquals(-32602, protocol.handle(call("skip_next", "{}")).message!!.errorCode())
        assertEquals(listOf("set_volume"), calls)
    }

    @Test
    fun providerCursorIsOpaqueAndOnlyAvailableOnProviderPages() = runBlocking {
        val cursor = "page:+/opaque=="
        val protocol = protocol { _, args ->
            assertEquals(cursor, args.get("cursor").asString)
            JsonObject()
        }
        val search = protocol.handle(call("search_catalog", """{"query":"Track","cursor":"$cursor"}""")).message!!
        assertFalse(search.getAsJsonObject("result").get("isError").asBoolean)
        assertEquals(-32602, protocol.handle(call("get_queue", """{"cursor":"$cursor"}""")).message!!.errorCode())
    }

    @Test
    fun modernDiscoveryValidatesMirroredHeadersAndVersions() {
        McpServer("127.0.0.1", 0, Token, protocol()) {}.use { server ->
            server.start()
            val port = server.listeningPort
            fun discover(version: String, headerMethod: String) = exchange(
                port, "POST", "/mcp",
                body = """{"jsonrpc":"2.0","id":2,"method":"server/discover","params":{"_meta":{"io.modelcontextprotocol/protocolVersion":"$version","io.modelcontextprotocol/clientCapabilities":{}}}}""",
                extraHeaders = "MCP-Protocol-Version: $version\r\nMcp-Method: $headerMethod\r\n",
            )
            val discovery = discover(McpVersion, "server/discover")
            assertTrue(discovery.startsWith("HTTP/1.1 200"))
            assertTrue(discovery.contains("\"resultType\":\"complete\""))
            assertTrue(discovery.contains("\"supportedVersions\":[\"$McpVersion\",\"$McpHandshakeVersion\""))
            val mismatch = discover(McpVersion, "tools/call")
            assertTrue(mismatch.startsWith("HTTP/1.1 400"))
            assertTrue(mismatch.contains("\"code\":-32020"))
            val unsupported = discover("2100-01-01", "server/discover")
            assertTrue(unsupported.startsWith("HTTP/1.1 400"))
            assertTrue(unsupported.contains("\"code\":-32022"))
        }
    }

    @Test
    fun closingListenerCancelsAnInFlightTool() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val protocol = protocol { _, _ ->
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        McpServer("127.0.0.1", 0, Token, protocol) {}.use { server ->
            server.start()
            Socket("127.0.0.1", server.listeningPort).use { client ->
                val request = request(server.listeningPort, "POST", "/mcp", body = call("skip_next", "{}"))
                client.getOutputStream().write(request.toByteArray())
                withTimeout(5_000) { started.await() }
                server.close()
                withTimeout(5_000) { cancelled.await() }
            }
        }
        Unit
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun queuedWriteChecksAuthorizationAfterPreviousWriteCompletes() = runTest {
        val provider = object : MusicProvider {
            override val id = "test"
            override val displayName = id
            override val capabilities = emptySet<SourceCapability>()
        }
        val coordinator = SourceMutationCoordinator(ProviderRegistry(listOf(provider)), backgroundScope)
        val owner = coordinator.capture(provider.id)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val previous = async {
            coordinator.mutate(owner, "playlist") {
                started.complete(Unit)
                release.await()
            }
        }
        started.await()
        var allowed = true
        var dispatched = false
        val remote = async {
            withContext(SourceMutationAuthorization { if (!allowed) throw CancellationException("Permission revoked") }) {
                coordinator.mutate(owner, "playlist") { dispatched = true }
            }
        }
        runCurrent()
        allowed = false
        release.complete(Unit)
        previous.await()
        try {
            remote.await()
            throw AssertionError("Revoked write was accepted")
        } catch (_: CancellationException) {
            assertFalse(dispatched)
        }
    }

    @Test
    fun authenticatedHttpRejectsBrowserAndUnknownHostAndSupportsBothTransports() {
        McpServer("127.0.0.1", 0, Token, protocol()) {}.use { server ->
            server.start()
            val port = server.listeningPort
            assertTrue(exchange(port, "POST", "/mcp", authorization = "invalid").startsWith("HTTP/1.1 401"))
            assertTrue(exchange(port, "POST", "/mcp", extraHeaders = "Origin: http://127.0.0.1\r\n").startsWith("HTTP/1.1 403"))
            assertTrue(exchange(port, "POST", "/mcp", host = "rebound.test:$port").startsWith("HTTP/1.1 403"))
            assertTrue(exchange(port, "GET", "/mcp").startsWith("HTTP/1.1 405"))
            assertTrue(exchange(port, "POST", "/mcp", body = Ping).contains("\r\n\r\n{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}"))
            val modern = exchange(port, "POST", "/mcp", body = Ping, accept = "text/event-stream")
            assertTrue(modern.contains("event: message\ndata: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}"))
            assertTrue(exchange(port, "POST", "/mcp", body = Ping, chunked = true).contains("\"result\":{}"))
            Socket("127.0.0.1", port).use { stream ->
                stream.soTimeout = 5_000
                stream.getOutputStream().write(request(port, "GET", "/sse", accept = "text/event-stream").toByteArray())
                val reader = stream.getInputStream().bufferedReader()
                assertTrue(reader.readLine().startsWith("HTTP/1.1 200"))
                while (!reader.readLine().isNullOrEmpty()) {
                    // Skip the remaining HTTP response headers.
                }
                assertEquals("event: endpoint", reader.nonemptyLine())
                val endpoint = reader.nonemptyLine().removePrefix("data: ")
                assertTrue(exchange(port, "POST", endpoint, body = Ping).startsWith("HTTP/1.1 202"))
                assertEquals("event: message", reader.nonemptyLine())
                assertTrue(reader.nonemptyLine().contains("\"result\":{}"))
            }
        }
    }

    private fun protocol(
        permitted: (McpPermission) -> Boolean = { true },
        execute: suspend (McpTool, JsonObject) -> JsonObject = { _, _ -> JsonObject() },
    ) = McpProtocol(MeiloXMcpTools, permitted, execute, "test")

    private fun JsonObject.errorCode() = getAsJsonObject("error").get("code").asInt

    private fun call(name: String, arguments: String) =
        """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"$name","arguments":$arguments}}"""

    private fun BufferedReader.nonemptyLine(): String = generateSequence(::readLine).first(String::isNotEmpty)

    private fun exchange(
        port: Int,
        method: String,
        path: String,
        body: String = "",
        accept: String = "application/json, text/event-stream",
        authorization: String = Token,
        host: String = "127.0.0.1:$port",
        extraHeaders: String = "",
        chunked: Boolean = false,
    ): String = Socket("127.0.0.1", port).use { socket ->
        socket.soTimeout = 5_000
        socket.getOutputStream().write(request(port, method, path, body, accept, authorization, host, extraHeaders, chunked).toByteArray())
        socket.getInputStream().bufferedReader().readText()
    }

    private fun request(
        port: Int,
        method: String,
        path: String,
        body: String = "",
        accept: String = "application/json, text/event-stream",
        authorization: String = Token,
        host: String = "127.0.0.1:$port",
        extraHeaders: String = "",
        chunked: Boolean = false,
    ): String = buildString {
        append("$method $path HTTP/1.1\r\nHost: $host\r\nAccept: $accept\r\nAuthorization: Bearer $authorization\r\n")
        append("Content-Type: application/json\r\n$extraHeaders")
        if (chunked) append("Transfer-Encoding: chunked\r\n\r\n${body.length.toString(16)}\r\n$body\r\n0\r\n\r\n")
        else append("Content-Length: ${body.length}\r\n\r\n$body")
    }

    private companion object {
        const val Token = "test-bearer-token"
        const val Ping = """{"jsonrpc":"2.0","id":1,"method":"ping"}"""
    }
}
