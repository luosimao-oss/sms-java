package com.luosimao.sms;

/**
 * Exception thrown when the Luosimao API returns an error or when a network issue occurs.
 */
public class LuosimaoException extends RuntimeException {

    private final LuosimaoErrorCode errorCode;
    private final int rawCode;

    public LuosimaoException(String message) {
        super(message);
        this.errorCode = LuosimaoErrorCode.UNKNOWN_ERROR;
        this.rawCode = -99;
    }

    public LuosimaoException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = LuosimaoErrorCode.UNKNOWN_ERROR;
        this.rawCode = -99;
    }

    public LuosimaoException(LuosimaoErrorCode errorCode, int rawCode, String message) {
        super(String.format("Luosimao API Error: %d - %s (%s)", rawCode, errorCode.getDescription(), message));
        this.errorCode = errorCode;
        this.rawCode = rawCode;
    }

    public LuosimaoErrorCode getErrorCode() {
        return errorCode;
    }

    public int getRawCode() {
        return rawCode;
    }
}
