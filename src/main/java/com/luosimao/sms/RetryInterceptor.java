package com.luosimao.sms;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * An OkHttp interceptor that retries requests on network failures.
 */
public class RetryInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(RetryInterceptor.class);
    
    private final int maxRetries;
    private final int retryDelayMillis;

    public RetryInterceptor(int maxRetries, int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException exception = null;

        int tryCount = 0;
        while (tryCount <= maxRetries) {
            if (response != null) {
                Util.closeQuietly(response);
                response = null;
            }

            try {
                response = chain.proceed(request);
                
                // Retry only if it's a server error (5xx)
                if (response.isSuccessful() || response.code() < 500) {
                    return response;
                }
                logger.warn("Server error: {}", response.code());
            } catch (IOException e) {
                exception = e;
                logger.warn("Request failed (attempt {}/{}): {}", tryCount + 1, maxRetries + 1, e.getMessage());
            }

            tryCount++;
            if (tryCount <= maxRetries) {
                logger.info("Retrying in {} ms...", retryDelayMillis);
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", ie);
                }
            }
        }

        if (response != null) {
            return response;
        }

        if (exception != null) {
            throw exception;
        }

        throw new IOException("Failed to execute request after " + maxRetries + " retries");
    }
}
