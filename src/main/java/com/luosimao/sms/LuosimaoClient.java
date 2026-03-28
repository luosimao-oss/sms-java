package com.luosimao.sms;

import com.google.gson.Gson;
import com.luosimao.sms.model.BaseResponse;
import com.luosimao.sms.model.SmsResponse;
import com.luosimao.sms.model.StatusResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LuosimaoClient {

    private static final Logger logger = LoggerFactory.getLogger(LuosimaoClient.class);
    
    private static final String DEFAULT_BASE_URL = "http://sms-api.luosimao.com/v1";
    private static final int MAX_BATCH_SIZE = 100000;
    
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;

    /**
     * Creates a new LuosimaoClient with default retry settings (3 retries).
     *
     * @param apiKey Your Luosimao API key (without "key-" prefix)
     */
    public LuosimaoClient(String apiKey) {
        this(apiKey, 3, 1000);
    }

    /**
     * Creates a new LuosimaoClient with custom retry settings.
     *
     * @param apiKey Your Luosimao API key (without "key-" prefix)
     * @param maxRetries Maximum number of retries on network failure
     * @param retryDelayMillis Delay between retries in milliseconds
     */
    public LuosimaoClient(String apiKey, int maxRetries, int retryDelayMillis) {
        this(apiKey, maxRetries, retryDelayMillis, 10, 10, 10);
    }

    /**
     * Creates a new LuosimaoClient with full custom settings.
     *
     * @param apiKey Your Luosimao API key (without "key-" prefix)
     * @param maxRetries Maximum number of retries on network failure
     * @param retryDelayMillis Delay between retries in milliseconds
     * @param connectTimeoutSeconds Connection timeout in seconds
     * @param readTimeoutSeconds Read timeout in seconds
     * @param writeTimeoutSeconds Write timeout in seconds
     */
    public LuosimaoClient(String apiKey, int maxRetries, int retryDelayMillis, 
                          long connectTimeoutSeconds, long readTimeoutSeconds, long writeTimeoutSeconds) {
        this(apiKey, maxRetries, retryDelayMillis, connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds, DEFAULT_BASE_URL);
    }

    /**
     * Creates a new LuosimaoClient, mainly used for testing with custom base URL.
     *
     * @param apiKey Your Luosimao API key (without "key-" prefix)
     * @param maxRetries Maximum number of retries
     * @param retryDelayMillis Delay between retries
     * @param connectTimeoutSeconds Connection timeout in seconds
     * @param readTimeoutSeconds Read timeout in seconds
     * @param writeTimeoutSeconds Write timeout in seconds
     * @param baseUrl Base API URL
     */
    LuosimaoClient(String apiKey, int maxRetries, int retryDelayMillis, 
                   long connectTimeoutSeconds, long readTimeoutSeconds, long writeTimeoutSeconds, 
                   String baseUrl) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }

        this.baseUrl = baseUrl;

        // Encapsulate basic auth by automatically prepending "key-"
        final String credential = Credentials.basic("api", "key-" + apiKey);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .addInterceptor(new RetryInterceptor(maxRetries, retryDelayMillis))
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("Authorization", credential)
                            .build();
                    return chain.proceed(request);
                })
                .build();
        
        this.gson = new Gson();
    }

    /**
     * Send a single SMS.
     *
     * @param mobile  Target mobile number
     * @param message SMS content (must include signature e.g., 【Company】)
     * @return SmsResponse
     * @throws LuosimaoException if the API returns an error
     */
    public SmsResponse send(String mobile, String message) {
        RequestBody body = new FormBody.Builder()
                .add("mobile", mobile)
                .add("message", message)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "/send.json")
                .post(body)
                .build();

        return executeRequest(request, SmsResponse.class);
    }

    /**
     * Send batch SMS.
     *
     * @param mobileList Target mobile numbers
     * @param message    SMS content (must include signature e.g., 【Company】)
     * @return SmsResponse
     * @throws LuosimaoException if the API returns an error
     */
    public SmsResponse sendBatch(List<String> mobileList, String message) {
        return sendBatch(mobileList, message, null);
    }

    /**
     * Send batch SMS with scheduled time.
     *
     * @param mobileList Target mobile numbers
     * @param message    SMS content (must include signature e.g., 【Company】)
     * @param time       Scheduled send time (format: yyyy-MM-dd HH:mm:ss)
     * @return SmsResponse
     * @throws LuosimaoException if the API returns an error
     */
    public SmsResponse sendBatch(List<String> mobileList, String message, String time) {
        if (mobileList == null || mobileList.isEmpty()) {
            throw new IllegalArgumentException("Mobile list cannot be null or empty");
        }
        
        // 限制最大批量发送数量，防止请求体过大（假设官方限制为10万，具体以官方最新文档为准）
        if (mobileList.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size exceeds the maximum limit (" + MAX_BATCH_SIZE + "). Please chunk your list.");
        }

        String mobiles = String.join(",", mobileList);

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("mobile_list", mobiles)
                .add("message", message);

        if (time != null && !time.trim().isEmpty()) {
            formBuilder.add("time", time);
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/send_batch.json")
                .post(formBuilder.build())
                .build();

        return executeRequest(request, SmsResponse.class);
    }

    /**
     * Query account balance.
     *
     * @return StatusResponse
     * @throws LuosimaoException if the API returns an error
     */
    public StatusResponse getStatus() {
        Request request = new Request.Builder()
                .url(baseUrl + "/status.json")
                .get()
                .build();

        return executeRequest(request, StatusResponse.class);
    }

    private <T extends BaseResponse> T executeRequest(Request request, Class<T> responseClass) {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("HTTP request failed: code={}, message={}", response.code(), response.message());
                throw new LuosimaoException("HTTP error " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new LuosimaoException("Empty response body");
            }

            String json = responseBody.string();
            logger.debug("Luosimao API Response: {}", json);

            T result = gson.fromJson(json, responseClass);
            
            if (result == null) {
                throw new LuosimaoException("Failed to parse response JSON");
            }

            if (!result.isSuccess()) {
                LuosimaoErrorCode errorCode = LuosimaoErrorCode.fromCode(result.getError());
                throw new LuosimaoException(errorCode, result.getError(), result.getMsg());
            }

            return result;
        } catch (IOException e) {
            logger.error("Network error executing request", e);
            throw new LuosimaoException("Network error: " + e.getMessage(), e);
        }
    }
}
