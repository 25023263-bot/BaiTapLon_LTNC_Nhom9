package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Transaction;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuctionClosedException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.BidTooLowException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.TransactionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho AuctionHouse — lõi nghiệp vụ đấu giá.
 *
 * <h3>Tại sao dùng Mockito?</h3>
 * <p>AuctionHouse phụ thuộc vào 4 Repository (auctionRepo, bidRepo, userRepo, txRepo).
 * Nếu test thật sự gọi DB, mỗi test sẽ cần setup/teardown dữ liệu, chạy chậm,
 * và có thể fail vì lý do không liên quan (DB offline, data dirty...).</p>
 *
 * <p>Mockito tạo "giả lập" cho mỗi Repository:
 * <ul>
 *   <li>{@code when(repo.findById(1)).thenReturn(Optional.of(item))} — giả lập DB trả về item</li>
 *   <li>{@code verify(repo).save(any())} — kiểm tra xem code có gọi save() không</li>
 * </ul>
 * </p>
 *
 * <h3>Cấu trúc test:</h3>
 * <p>Dùng {@code @Nested} class để nhóm các test liên quan, dễ đọc hơn danh sách phẳng.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionHouse — Business Logic Tests")
class AuctionHouseTest {

    // ── Mock dependencies ─────────────────────────────────────────────────────
    // @Mock tạo giả lập tự động, không cần new XxxRepository() thật

    @Mock private AuctionRepository     auctionRepo;
    @Mock private BidRepository         bidRepo;
    @Mock private UserRepository        userRepo;
    @Mock private TransactionRepository txRepo;

    // Class đang được test (System Under Test)
    private AuctionHouse auctionHouse;

    // ── Dữ liệu test dùng chung ───────────────────────────────────────────────

    private static final int    SELLER_ID  = 1;
    private static final int    BUYER_ID   = 2;
    private static final int    ITEM_ID    = 100;

    private static final BigDecimal STARTING_PRICE  = new BigDecimal("1000000");  // 1 triệu
    private static final BigDecimal INCREMENT        = new BigDecimal("50000");    // 50 nghìn
    private static final BigDecimal BUYER_BALANCE    = new BigDecimal("5000000"); // 5 triệu

    @BeforeEach
    void setUp() {
        // Khởi tạo AuctionHouse với các mock — KHÔNG có DB thật
        auctionHouse = new AuctionHouse(auctionRepo, bidRepo, userRepo, txRepo);
    }

    // =========================================================================
    // Helper methods — tạo đối tượng test
    // =========================================================================

    /**
     * Tạo một PhysicalItem đang ACTIVE, thời gian kết thúc sau 1 giờ.
     * Đây là trạng thái "bình thường" cho hầu hết các test.
     */
    private PhysicalItem makeActiveItem() {
        PhysicalItem item = new PhysicalItem(
                ITEM_ID, SELLER_ID, "Laptop Test", "Mô tả test",
                "Electronics", STARTING_PRICE, INCREMENT,
                new BigDecimal("10000000"), // buyNowPrice = 10 triệu
                LocalDateTime.now().minusHours(1),  // startTime = 1 giờ trước
                LocalDateTime.now().plusHours(1),   // endTime = 1 giờ sau (còn hạn)
                "LIKE_NEW", 1500, "30x20x5 cm", "HCM",
                new BigDecimal("50000"), false
        );
        item.setStatus(AuctionStatus.ACTIVE);
        return item;
    }

    /**
     * Tạo Buyer với balance đủ để đặt giá.
     * Password hash dùng giá trị cứng (không cần BCrypt thật trong unit test).
     */
    private Buyer makeBuyer(int id, BigDecimal balance) {
        Buyer buyer = new Buyer(id, "buyer" + id, "buyer" + id + "@test.com",
                "$2a$12$fakehash", "Nguyen Test", "0900000000");
        buyer.setWalletBalance(balance);
        return buyer;
    }

    private Seller makeSeller(int id) {
        Seller seller = new Seller(id, "seller" + id, "seller" + id + "@test.com",
                "$2a$12$fakehash", "Tran Seller", "0911111111");
        seller.setEarningsBalance(new BigDecimal("10000000"));
        return seller;
    }

    // =========================================================================
    // placeBid() tests
    // =========================================================================

    @Nested
    @DisplayName("placeBid()")
    class PlaceBidTests {

        @Test
        @DisplayName("Happy path: bid hợp lệ được lưu và observer được notify")
        void placeBid_validBid_savesBidAndNotifiesObservers() throws Exception {
            // ARRANGE — Chuẩn bị dữ liệu và mock
            PhysicalItem item  = makeActiveItem();
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);
            BigDecimal   bidAmount = STARTING_PRICE.add(INCREMENT); // 1.050.000đ — hợp lệ

            // Giả lập DB: findById trả về item, findById user trả về buyer
            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));

            // Thêm observer để kiểm tra notify
            AuctionObserver mockObserver = mock(AuctionObserver.class);
            auctionHouse.addObserver(mockObserver);

            // ACT — Gọi method đang test
            Bid result = auctionHouse.placeBid(ITEM_ID, BUYER_ID, bidAmount);

            // ASSERT — Kiểm tra kết quả
            assertNotNull(result, "Bid trả về không được null");
            assertEquals(bidAmount, result.getAmount(), "Amount phải khớp");
            assertEquals(ITEM_ID,  result.getAuctionId(),  "AuctionId phải khớp");
            assertEquals(BUYER_ID, result.getBuyerId(), "BuyerId phải khớp");

            // Kiểm tra side effects: bidRepo.save() phải được gọi đúng 1 lần
            verify(bidRepo, times(1)).save(any(Bid.class));

            // Kiểm tra currentPrice và leadingBidderId được cập nhật trong DB
            verify(auctionRepo, times(1)).updateCurrentBid(eq(ITEM_ID), eq(bidAmount), eq(BUYER_ID));

            // Observer phải được notify
            verify(mockObserver, times(1)).onNewBid(eq(item), any(Bid.class));
        }

        @Test
        @DisplayName("Bid thấp hơn minimum → BidTooLowException")
        void placeBid_amountBelowMinimum_throwsBidTooLowException() throws Exception {
            // ARRANGE
            PhysicalItem item  = makeActiveItem();
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);
            // Bid ở đúng startingPrice — chưa đủ (cần startingPrice + increment)
            BigDecimal tooLow = STARTING_PRICE;

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));

            // ACT & ASSERT — Phải ném đúng loại exception
            BidTooLowException ex = assertThrows(BidTooLowException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, tooLow));

            assertEquals(tooLow, ex.getBidAmount(),
                    "Exception phải giữ số tiền bid thực tế");
            assertEquals(item.getNextMinimumBid(), ex.getMinimumRequired(),
                    "Exception phải giữ số tiền tối thiểu cần thiết");

            // DB KHÔNG được cập nhật khi bid thất bại
            verify(bidRepo, never()).save(any());
            verify(auctionRepo, never()).updateCurrentBid(anyInt(), any(), anyInt());
        }

        @Test
        @DisplayName("Số dư không đủ → InsufficientBalanceException")
        void placeBid_insufficientBalance_throwsInsufficientBalanceException() throws Exception {
            // ARRANGE — buyer chỉ có 10.000đ
            PhysicalItem item       = makeActiveItem();
            Buyer        poorBuyer  = makeBuyer(BUYER_ID, new BigDecimal("10000"));
            BigDecimal   bidAmount  = STARTING_PRICE.add(INCREMENT); // 1.050.000đ

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(poorBuyer));

            // ACT & ASSERT
            assertThrows(InsufficientBalanceException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, bidAmount));

            verify(bidRepo, never()).save(any());
        }

        @Test
        @DisplayName("Phiên đã đóng → AuctionClosedException")
        void placeBid_closedAuction_throwsAuctionClosedException() throws Exception {
            // ARRANGE — item có status CLOSED
            PhysicalItem item = makeActiveItem();
            item.setStatus(AuctionStatus.CLOSED);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            // ACT & ASSERT
            assertThrows(AuctionClosedException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT)));

            verify(bidRepo, never()).save(any());
        }

        @Test
        @DisplayName("Seller tự bid vào item của mình → IllegalStateException")
        void placeBid_sellerBidsOwnItem_throwsIllegalStateException() throws Exception {
            // ARRANGE — bidderId == sellerId
            PhysicalItem item = makeActiveItem();

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            // ACT & ASSERT
            // SELLER_ID == item.getSellerId() → bị chặn
            assertThrows(IllegalStateException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, SELLER_ID, STARTING_PRICE.add(INCREMENT)));
        }

        @Test
        @DisplayName("Phiên hết giờ nhưng DB vẫn ACTIVE → tự closeAuction() rồi throw")
        void placeBid_expiredButStillActive_autoClosesAndThrows() throws Exception {
            // ARRANGE — item ACTIVE nhưng endTime đã qua
            PhysicalItem item = makeActiveItem();
            item.setEndTime(LocalDateTime.now().minusMinutes(5)); // đã hết 5 phút trước
            item.setStatus(AuctionStatus.ACTIVE); // DB chưa kịp update

            // findById được gọi 2 lần: lần 1 trong placeBid, lần 2 trong closeAuction
            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            // ACT & ASSERT
            assertThrows(AuctionClosedException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT)));

            // closeAuction() phải được gọi để đồng bộ DB
            verify(auctionRepo, atLeastOnce()).updateStatus(eq(ITEM_ID), any());
        }
    }

    // =========================================================================
    // closeAuction() tests
    // =========================================================================

    @Nested
    @DisplayName("closeAuction()")
    class CloseAuctionTests {

        @Test
        @DisplayName("Có bid → CLOSED, thanh toán, notify observer")
        void closeAuction_hasBids_closesAndProcessesPayment() throws Exception {
            // ARRANGE
            PhysicalItem item = makeActiveItem();
            // Giả lập có 1 bid — leadingBidderId = BUYER_ID, currentPrice tăng
            item.updateCurrentBid(STARTING_PRICE.add(INCREMENT), BUYER_ID);

            Buyer  buyer  = makeBuyer(BUYER_ID, BUYER_BALANCE);
            Seller seller = makeSeller(SELLER_ID);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            // processPayment gọi userRepo.findById để phân biệt Buyer/Seller
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(userRepo.findById(SELLER_ID)).thenReturn(Optional.of(seller));

            AuctionObserver observer = mock(AuctionObserver.class);
            auctionHouse.addObserver(observer);

            // ACT
            auctionHouse.closeAuction(ITEM_ID);

            // ASSERT — status phải là CLOSED
            verify(auctionRepo).updateStatus(ITEM_ID, AuctionStatus.CLOSED);

            // Transaction phải được tạo
            verify(txRepo).save(any(Transaction.class));

            // Observer phải được notify với winner = BUYER_ID
            verify(observer).onAuctionClosed(eq(item), eq(BUYER_ID));
        }

        @Test
        @DisplayName("Không có bid → EXPIRED, notify observer với winner = null")
        void closeAuction_noBids_setsExpiredAndNotifies() throws Exception {
            // ARRANGE — item chưa có bid nào (leadingBidderId = 0)
            PhysicalItem item = makeActiveItem();
            // leadingBidderId = 0 mặc định → hasBids() = false

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            AuctionObserver observer = mock(AuctionObserver.class);
            auctionHouse.addObserver(observer);

            // ACT
            auctionHouse.closeAuction(ITEM_ID);

            // ASSERT
            verify(auctionRepo).updateStatus(ITEM_ID, AuctionStatus.EXPIRED);

            // Không có winner → không có transaction
            verify(txRepo, never()).save(any());

            // Observer được notify với winnerId = null
            verify(observer).onAuctionClosed(eq(item), isNull());
        }

        @Test
        @DisplayName("Gọi closeAuction() trên item không ACTIVE → bỏ qua, không làm gì")
        void closeAuction_alreadyClosed_doesNothing() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.setStatus(AuctionStatus.CLOSED);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            auctionHouse.closeAuction(ITEM_ID);

            // Không có gì thay đổi trong DB
            verify(auctionRepo, never()).updateStatus(anyInt(), any());
            verify(txRepo, never()).save(any());
        }
    }

    // =========================================================================
    // cancelAuction() tests
    // =========================================================================

    @Nested
    @DisplayName("cancelAuction()")
    class CancelAuctionTests {

        @Test
        @DisplayName("Cancel thành công: seller đúng, chưa có bid")
        void cancelAuction_validRequest_cancelsAndNotifies() throws Exception {
            PhysicalItem item = makeActiveItem();
            // chưa có bid — mặc định sau makeActiveItem()

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            AuctionObserver observer = mock(AuctionObserver.class);
            auctionHouse.addObserver(observer);

            auctionHouse.cancelAuction(ITEM_ID, SELLER_ID);

            verify(auctionRepo).updateStatus(ITEM_ID, AuctionStatus.CANCELLED);
            verify(observer).onAuctionCancelled(item);
        }

        @Test
        @DisplayName("Cancel khi đã có bid → IllegalStateException")
        void cancelAuction_hasBids_throwsIllegalStateException() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.updateCurrentBid(STARTING_PRICE.add(INCREMENT), BUYER_ID); // có bid

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThrows(IllegalStateException.class,
                    () -> auctionHouse.cancelAuction(ITEM_ID, SELLER_ID));

            verify(auctionRepo, never()).updateStatus(anyInt(), any());
        }

        @Test
        @DisplayName("Seller khác cố cancel → SecurityException")
        void cancelAuction_wrongSeller_throwsSecurityException() throws Exception {
            PhysicalItem item = makeActiveItem();
            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            int wrongSellerId = 999;
            assertThrows(SecurityException.class,
                    () -> auctionHouse.cancelAuction(ITEM_ID, wrongSellerId));
        }
    }

    // =========================================================================
    // listItem() tests
    // =========================================================================

    @Nested
    @DisplayName("listItem()")
    class ListItemTests {

        @Test
        @DisplayName("Item hợp lệ, startTime là ngay bây giờ → status ACTIVE")
        void listItem_startTimeNow_setsActiveStatus() throws Exception {
            PhysicalItem item = new PhysicalItem(
                    0, SELLER_ID, "Điện thoại Samsung", "Mô tả",
                    "Electronics", STARTING_PRICE, INCREMENT, null,
                    LocalDateTime.now().minusSeconds(30),  // startTime = vừa rồi
                    LocalDateTime.now().plusHours(2),       // endTime = 2 giờ sau
                    "NEW", 200, "15x8x1 cm", "Hanoi",
                    BigDecimal.ZERO, false
            );

            auctionHouse.listItem(item);

            // Status phải là ACTIVE vì startTime <= now
            assertEquals(AuctionStatus.ACTIVE, item.getStatus());
            verify(auctionRepo).save(item);
        }

        @Test
        @DisplayName("Item hợp lệ, startTime trong tương lai → status PENDING")
        void listItem_futureStartTime_setsPendingStatus() throws Exception {
            PhysicalItem item = new PhysicalItem(
                    0, SELLER_ID, "Đồng hồ Rolex", "Mô tả",
                    "Fashion", STARTING_PRICE, INCREMENT, null,
                    LocalDateTime.now().plusHours(2),  // startTime = 2 giờ sau
                    LocalDateTime.now().plusHours(4),  // endTime = 4 giờ sau
                    "LIKE_NEW", 150, "10x10x5 cm", "HCM",
                    BigDecimal.ZERO, false
            );

            auctionHouse.listItem(item);

            assertEquals(AuctionStatus.PENDING, item.getStatus());
            verify(auctionRepo).save(item);
        }

        @Test
        @DisplayName("Item không hợp lệ (thiếu title) → IllegalArgumentException")
        void listItem_invalidItem_throwsIllegalArgumentException() throws SQLException {
            PhysicalItem item = new PhysicalItem();
            item.setSellerId(SELLER_ID);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusHours(2));
            // title = null → isValidItem() = false

            assertThrows(IllegalArgumentException.class,
                    () -> auctionHouse.listItem(item));

            verify(auctionRepo, never()).save(any());
        }
    }

    // =========================================================================
    // Anti-sniping (extendIfLastMinute) tests
    // =========================================================================

    @Nested
    @DisplayName("Anti-sniping")
    class AntiSnipingTests {

        @Test
        @DisplayName("Bid trong 30 giây cuối → endTime được gia hạn")
        void placeBid_bidInLastSeconds_extendsEndTime() throws Exception {
            // ARRANGE — item sắp hết giờ (còn 10 giây)
            PhysicalItem item  = makeActiveItem();
            item.setEndTime(LocalDateTime.now().plusSeconds(10)); // còn 10s < window 30s

            Buyer buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // ACT
            auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT));

            // ASSERT — endTime phải được cập nhật trong DB (anti-snipe gia hạn)
            verify(auctionRepo).updateEndTime(eq(ITEM_ID), any(java.time.LocalDateTime.class));
        }

        @Test
        @DisplayName("Bid khi còn nhiều giờ → endTime KHÔNG bị gia hạn")
        void placeBid_bidWithTimeRemaining_doesNotExtendEndTime() throws Exception {
            // ARRANGE — item còn 1 giờ (không trong window 30s)
            PhysicalItem item  = makeActiveItem(); // endTime = now + 1 giờ
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // ACT
            auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT));

            // ASSERT — updateEndTime KHÔNG được gọi
            verify(auctionRepo, never()).updateEndTime(anyInt(), any());
        }
    }

    // =========================================================================
    // Observer pattern tests
    // =========================================================================

    @Nested
    @DisplayName("Observer Pattern")
    class ObserverTests {

        @Test
        @DisplayName("addObserver/removeObserver hoạt động đúng")
        void observer_addAndRemove_worksCorrectly() throws Exception {
            PhysicalItem item  = makeActiveItem();
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            AuctionObserver observer = mock(AuctionObserver.class);
            auctionHouse.addObserver(observer);
            auctionHouse.removeObserver(observer);

            // Sau khi remove → observer không được notify
            auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT));

            verify(observer, never()).onNewBid(any(), any());
        }

        @Test
        @DisplayName("Nhiều observer đều được notify")
        void observer_multipleObservers_allGetNotified() throws Exception {
            PhysicalItem item  = makeActiveItem();
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            AuctionObserver obs1 = mock(AuctionObserver.class);
            AuctionObserver obs2 = mock(AuctionObserver.class);
            auctionHouse.addObserver(obs1);
            auctionHouse.addObserver(obs2);

            auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT));

            // Cả 2 observer đều phải nhận thông báo
            verify(obs1).onNewBid(eq(item), any(Bid.class));
            verify(obs2).onNewBid(eq(item), any(Bid.class));
        }
    }

    // =========================================================================
    // Bid amount capture test — dùng ArgumentCaptor
    // =========================================================================

    @Test
    @DisplayName("ArgumentCaptor: kiểm tra chính xác dữ liệu Bid được lưu vào DB")
    void placeBid_capturesSavedBidWithCorrectData() throws Exception {
        // ARRANGE
        PhysicalItem item  = makeActiveItem();
        Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);
        BigDecimal   bidAmount = new BigDecimal("1200000");

        when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
        when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        // ArgumentCaptor "chụp" object được truyền vào save() để kiểm tra
        ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);

        // ACT
        auctionHouse.placeBid(ITEM_ID, BUYER_ID, bidAmount);

        // ASSERT — kiểm tra chính xác dữ liệu Bid được lưu
        verify(bidRepo).save(bidCaptor.capture());
        Bid savedBid = bidCaptor.getValue();

        assertEquals(ITEM_ID,   savedBid.getAuctionId(),  "AuctionId phải đúng");
        assertEquals(BUYER_ID,  savedBid.getBuyerId(), "BuyerId phải đúng");
        assertEquals(bidAmount, savedBid.getAmount(),  "Amount phải đúng");
        assertFalse(savedBid.isAutoBid(), "Bid thủ công không được đánh dấu auto-bid");
    }
}
