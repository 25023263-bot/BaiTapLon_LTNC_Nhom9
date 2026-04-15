package com.nhom9.auction.baitaplon_ltnc_nhom9.exception;

/**
 * Ném ra khi đăng nhập thất bại hoặc token/session không hợp lệ.
 */
public class AuthenticationException extends Exception {

    public enum Reason {
        INVALID_CREDENTIALS("Tên đăng nhập hoặc mật khẩu không đúng."),
        ACCOUNT_DISABLED("Tài khoản đã bị vô hiệu hóa."),
        SESSION_EXPIRED("Phiên đăng nhập đã hết hạn."),
        UNAUTHORIZED("Bạn không có quyền thực hiện thao tác này.");

        private final String message;
        Reason(String message) { this.message = message; }
        public String getMessage() { return message; }
    }

    private final Reason reason;

    public AuthenticationException(Reason reason) {
        super(reason.getMessage());
        this.reason = reason;
    }

    public AuthenticationException(String message) {
        super(message);
        this.reason = null;
    }

    public Reason getReason() { return reason; }
}