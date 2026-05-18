package com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol;

import java.io.Serializable;

/**
 * Object server trả về client qua socket.
 *
 * 3 loại:
 *   OK           — request thành công, data chứa kết quả
 *   ERROR        — request thất bại, message chứa lý do
 *   NOTIFICATION — server chủ động push (bid mới, auction đóng...)
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status { OK, ERROR, NOTIFICATION }

    private final Status status;
    private final Object data;
    private final String message;

    // ── Static factory methods — dễ đọc hơn new Response(...) ──

    public static Response ok(Object data) {
        return new Response(Status.OK, data, null);
    }

    public static Response error(String message) {
        return new Response(Status.ERROR, null, message);
    }

    public static Response notification(Object data) {
        return new Response(Status.NOTIFICATION, data, null);
    }

    private Response(Status status, Object data, String message) {
        this.status  = status;
        this.data    = data;
        this.message = message;
    }

    public Status getStatus()  { return status; }
    public Object getData()    { return data; }
    public String getMessage() { return message; }

    public boolean isOk()           { return status == Status.OK; }
    public boolean isError()        { return status == Status.ERROR; }
    public boolean isNotification() { return status == Status.NOTIFICATION; }

    @Override
    public String toString() {
        return "Response{status=" + status + ", message=" + message + "}";
    }
}