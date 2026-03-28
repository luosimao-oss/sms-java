package com.luosimao.sms;

/**
 * Luosimao SMS API Error Codes.
 * Maps to the unified error codes returned by the API.
 */
public enum LuosimaoErrorCode {
    SUCCESS(0, "请求成功"),
    AUTH_FAILED(-10, "验证信息失败"),
    USER_DISABLED(-11, "用户接口被禁用"),
    BALANCE_FROZEN(-12, "余额冻结"),
    INSUFFICIENT_BALANCE(-20, "短信余额不足"),
    EMPTY_MESSAGE(-30, "短信内容为空"),
    SENSITIVE_WORD(-31, "短信内容存在敏感词"),
    MISSING_SIGNATURE(-32, "短信内容缺少签名信息"),
    MESSAGE_TOO_LONG(-33, "短信过长，超过300字（含签名）"),
    SIGNATURE_UNAVAILABLE(-34, "签名不可用"),
    TEST_SIGNATURE_LIMITED(-35, "测试签名受限"),
    INVALID_MOBILE(-40, "错误的手机号"),
    BLACKLISTED_MOBILE(-41, "号码在黑名单中"),
    SEND_TOO_FAST(-42, "验证码类短信发送频率过快"),
    IP_NOT_IN_WHITELIST(-50, "请求发送IP不在白名单内"),
    UNKNOWN_ERROR(-99, "未知错误");

    private static final java.util.Map<Integer, LuosimaoErrorCode> CODE_MAP;
    
    static {
        java.util.Map<Integer, LuosimaoErrorCode> map = new java.util.HashMap<>();
        for (LuosimaoErrorCode error : values()) {
            map.put(error.getCode(), error);
        }
        CODE_MAP = java.util.Collections.unmodifiableMap(map);
    }

    private final int code;
    private final String description;

    LuosimaoErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static LuosimaoErrorCode fromCode(int code) {
        return CODE_MAP.getOrDefault(code, UNKNOWN_ERROR);
    }
}
