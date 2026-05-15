package com.nhom9.auction.baitaplon_ltnc_nhom9.exception;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho các custom exception – kiểm tra message, field, và dữ liệu đính kèm.
 *
 * Tại sao test exception?
 * - Exception mang thông tin quan trọng để UI hiển thị lỗi đúng cho người dùng
 * - Nếu exception không lưu đúng dữ liệu, UI không thể gợi ý hành động tiếp theo
 *   (ví dụ: "Bid tối thiểu 150K" – nếu minimumRequired sai thì hướng dẫn người dùng sai)
 */
@DisplayName("Custom Exceptions – Thông tin lỗi")
class ExceptionTest {

    // ─── InsufficientBalanceException ────────────────────────────────────────

    @Test
    @DisplayName("InsufficientBalanceException: lưu đúng available và required")
    void insufficientBalance_storesAvailableAndRequired() {
        BigDecimal available = new BigDecimal("100000");
        BigDecimal required  = new BigDecimal("500000");

        InsufficientBalanceException ex =
                new InsufficientBalanceException(available, required);

        assertEquals(available, ex.getAvailable());
        assertEquals(required,  ex.getRequired());
    }

    @Test
    @DisplayName("InsufficientBalanceException: message chứa số tiền")
    void insufficientBalance_messageContainsAmounts() {
        InsufficientBalanceException ex =
                new InsufficientBalanceException(
                        new BigDecimal("100000"),
                        new BigDecimal("500000"));

        // Message phải có thông tin về số dư, không được rỗng
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }

    @Test
    @DisplayName("InsufficientBalanceException: constructor chuỗi → available = null")
    void insufficientBalance_stringConstructor_nullFields() {
        InsufficientBalanceException ex =
                new InsufficientBalanceException("Số dư không đủ.");

        assertEquals("Số dư không đủ.", ex.getMessage());
        assertNull(ex.getAvailable());
        assertNull(ex.getRequired());
    }

    // ─── BidTooLowException ───────────────────────────────────────────────────

    @Test
    @DisplayName("BidTooLowException: lưu đúng bidAmount và minimumRequired")
    void bidTooLow_storesBidAndMinimum() {
        BigDecimal bid     = new BigDecimal("50000");
        BigDecimal minimum = new BigDecimal("110000");

        BidTooLowException ex = new BidTooLowException(bid, minimum);

        assertEquals(bid,     ex.getBidAmount());
        assertEquals(minimum, ex.getMinimumRequired());
    }

    @Test
    @DisplayName("BidTooLowException: message không rỗng")
    void bidTooLow_messageNotEmpty() {
        BidTooLowException ex = new BidTooLowException(
                new BigDecimal("50000"),
                new BigDecimal("110000"));

        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }

    // ─── AuctionClosedException ───────────────────────────────────────────────

    @Test
    @DisplayName("AuctionClosedException: lưu đúng auctionId và status")
    void auctionClosed_storesIdAndStatus() {
        AuctionClosedException ex =
                new AuctionClosedException(99, AuctionStatus.CANCELLED);

        assertEquals(99, ex.getAuctionId());
        assertEquals(AuctionStatus.CANCELLED, ex.getCurrentStatus());
    }

    @Test
    @DisplayName("AuctionClosedException: EXPIRED status → message không rỗng")
    void auctionClosed_expiredStatus_messageNotEmpty() {
        AuctionClosedException ex =
                new AuctionClosedException(5, AuctionStatus.EXPIRED);

        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }

    @Test
    @DisplayName("AuctionClosedException: CLOSED status → thông tin đúng")
    void auctionClosed_closedStatus_correctData() {
        AuctionClosedException ex =
                new AuctionClosedException(7, AuctionStatus.CLOSED);

        assertEquals(7, ex.getAuctionId());
        assertEquals(AuctionStatus.CLOSED, ex.getCurrentStatus());
    }

    // ─── DuplicateUserException ───────────────────────────────────────────────

    @Test
    @DisplayName("DuplicateUserException: field USERNAME → lưu đúng field và value")
    void duplicateUser_usernameField_storesCorrectly() {
        DuplicateUserException ex =
                new DuplicateUserException(DuplicateUserException.Field.USERNAME, "alice");

        assertEquals(DuplicateUserException.Field.USERNAME, ex.getDuplicateField());
        assertEquals("alice", ex.getValue());
    }

    @Test
    @DisplayName("DuplicateUserException: field EMAIL → lưu đúng field và value")
    void duplicateUser_emailField_storesCorrectly() {
        DuplicateUserException ex =
                new DuplicateUserException(DuplicateUserException.Field.EMAIL, "alice@test.com");

        assertEquals(DuplicateUserException.Field.EMAIL, ex.getDuplicateField());
        assertEquals("alice@test.com", ex.getValue());
    }

    @Test
    @DisplayName("DuplicateUserException USERNAME: message chứa tên đăng nhập")
    void duplicateUser_usernameMessage_containsUsername() {
        DuplicateUserException ex =
                new DuplicateUserException(DuplicateUserException.Field.USERNAME, "alice");

        assertTrue(ex.getMessage().contains("alice"),
                "Message phải nhắc đến tên đăng nhập bị trùng");
    }

    @Test
    @DisplayName("DuplicateUserException EMAIL: message chứa email")
    void duplicateUser_emailMessage_containsEmail() {
        DuplicateUserException ex =
                new DuplicateUserException(DuplicateUserException.Field.EMAIL, "alice@test.com");

        assertTrue(ex.getMessage().contains("alice@test.com"),
                "Message phải nhắc đến email bị trùng");
    }
}
