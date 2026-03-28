package com.luosimao.sms;

import com.luosimao.sms.model.SmsResponse;
import com.luosimao.sms.model.StatusResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LuosimaoClientTest {

    private MockWebServer mockWebServer;
    private LuosimaoClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1").toString();
        // Use a shorter delay for tests
        client = new LuosimaoClient("test-api-key", 2, 100, 10, 10, 10, baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testSendSuccess() throws InterruptedException {
        String mockResponseJson = "{\"error\":0,\"msg\":\"ok\",\"batch_id\":\"12345\"}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponseJson));

        SmsResponse response = client.send("13800138000", "验证码：123456【铁壳测试】");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(0, response.getError());
        assertEquals("ok", response.getMsg());
        assertEquals("12345", response.getBatchId());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/v1/send.json", request.getPath());
        assertEquals("POST", request.getMethod());
        
        // Verify Basic Auth
        String authHeader = request.getHeader("Authorization");
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Basic "));
        
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("api:key-test-api-key".getBytes());
        assertEquals(expectedAuth, authHeader);
    }

    @Test
    void testSendBatchSuccess() throws InterruptedException {
        String mockResponseJson = "{\"error\":0,\"msg\":\"ok\",\"batch_id\":\"67890\"}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponseJson));

        SmsResponse response = client.sendBatch(
                Arrays.asList("13800138000", "13800138001"),
                "您的账户已欠费【铁壳测试】"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("67890", response.getBatchId());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/v1/send_batch.json", request.getPath());
        assertEquals("POST", request.getMethod());
        assertTrue(request.getBody().readUtf8().contains("mobile_list=13800138000%2C13800138001"));
    }

    @Test
    void testSendBatchEmptyListThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            client.sendBatch(new ArrayList<>(), "测试内容");
        });
    }

    @Test
    void testSendBatchExceedsLimitThrowsException() {
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 100001; i++) {
            largeList.add("13800000000");
        }
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.sendBatch(largeList, "测试内容");
        });
    }

    @Test
    void testStatusSuccess() throws InterruptedException {
        String mockResponseJson = "{\"error\":0,\"deposit\":1000}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponseJson));

        StatusResponse response = client.getStatus();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1000, response.getDeposit());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/v1/status.json", request.getPath());
        assertEquals("GET", request.getMethod());
    }

    @Test
    void testApiErrorThrowsException() {
        // -20: Insufficient balance
        String mockResponseJson = "{\"error\":-20,\"msg\":\"短信余额不足\"}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponseJson));

        LuosimaoException exception = assertThrows(LuosimaoException.class, () -> {
            client.send("13800138000", "测试【铁壳测试】");
        });

        assertEquals(LuosimaoErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
        assertEquals(-20, exception.getRawCode());
        assertTrue(exception.getMessage().contains("短信余额不足"));
    }

    @Test
    void testNetworkRetryOn500() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(502));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"error\":0,\"deposit\":50}"));

        StatusResponse response = client.getStatus();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(50, response.getDeposit());
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    void testNetworkRetryFailure() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500)); // 3rd attempt fails too

        assertThrows(LuosimaoException.class, () -> {
            client.getStatus();
        });
        
        assertEquals(3, mockWebServer.getRequestCount());
    }
}
