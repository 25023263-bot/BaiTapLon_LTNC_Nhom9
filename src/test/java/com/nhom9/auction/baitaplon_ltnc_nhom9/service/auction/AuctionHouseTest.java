package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuctionClosedException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.BidTooLowException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho AuctionHouse — lõi nghiệp vụ đấu giá.
 *
 * <h3>Tại sao dùng Mockito?</h3>
 * <p>AuctionHouse phụ thuộc vào 3 Repository (auctionRepo, bidRepo, userRepo).
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

    @Mock private AuctionRepository auctionRepo;
    @Mock private BidRepository     bidRepo;
    @Mock private UserRepository    userRepo;

    private AuctionHouse auctionHouse;

    // ── Hằng số test dùng chung ───────────────────────────────────────────────

    private static final int SELLER_ID = 1;
    private static final int BUYER_ID  = 2;
    private static final int ITEM_ID   = 100;

    private static final BigDecimal STARTING_PRICE = new BigDecimal("1000000"); // 1 triệu
    private static final BigDecimal INCREMENT       = new BigDecimal("50000");   // 50k
    private static final BigDecimal BUYER_BALANCE   = new BigDecimal("5000000"); // 5 triệu

    @BeforeEach
    void setUp() {
        auctionHouse = new AuctionHouse(auctionRepo, bidRepo, userRepo);
    }

    // =========================================================================
    // Helper methods — tạo đối tượng test
    // =========================================================================

    /**
     * Tạo PhysicalItem đang ACTIVE, endTime còn 1 giờ.
     * Dùng constructor 9-tham-số khớp với PhysicalItem thực tế.
     */
    private PhysicalItem makeActiveItem() {
        PhysicalItem item = new PhysicalItem(
                ITEM_ID, SELLER_ID,
                "Laptop Test", "Mô tả test", "Electronics",
                STARTING_PRICE, INCREMENT,
                LocalDateTime.now().minusHours(1),  // startTime đã qua
                LocalDateTime.now().plusHours(1)    // endTime còn 1h
        );
        item.setStatus(AuctionStatus.ACTIVE);
        return item;
    }

    /**
     * Tạo Buyer với balance tùy chỉnh.
     * Password hash dùng giá trị cứng — không cần BCrypt thật trong unit test.
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
        @DisplayName("Happy path: bid hợp lệ → lưu Bid, cập nhật giá, notify observer")
        void placeBid_validBid_savesBidAndNotifiesObservers() throws Exception {
            // ARRANGE
            PhysicalItem item  = makeActiveItem();
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);
            BigDecimal   bidAmount = STARTING_PRICE.add(INCREMENT); // 1.050.000đ

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            // triggerAutoBids gọi bidRepo.findActiveAutoBidsExcluding → trả về rỗng
            when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            AuctionObserver mockObserver = mock(AuctionObserver.class);
            auctionHouse.addObserver(mockObserver);

            // ACT
            Bid result = auctionHouse.placeBid(ITEM_ID, BUYER_ID, bidAmount);

            // ASSERT — kết quả trả về
            assertNotNull(result);
            assertEquals(bidAmount, result.getAmount());
            assertEquals(ITEM_ID,   result.getAuctionId()); // Bid dùng getAuctionId()
            assertEquals(BUYER_ID,  result.getBuyerId());

            // Side effects: phải save bid và update giá trong DB
            verify(bidRepo, times(1)).save(any(Bid.class));
            verify(auctionRepo, times(1)).updateCurrentBid(eq(ITEM_ID), eq(bidAmount), eq(BUYER_ID));

            // Observer phải được notify
            verify(mockObserver, times(1)).onNewBid(eq(item), any(Bid.class));
        }

        @Test
        @DisplayName("Bid thấp hơn minimum → BidTooLowException, DB không thay đổi")
        void placeBid_amountBelowMinimum_throwsBidTooLowException() throws Exception {
            PhysicalItem item  = makeActiveItem();
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);
            // bid đúng bằng currentPrice — chưa đủ, phải cộng thêm INCREMENT
            BigDecimal tooLow = STARTING_PRICE;

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            BidTooLowException ex = assertThrows(BidTooLowException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, tooLow));

            assertEquals(tooLow, ex.getBidAmount());
            assertEquals(item.getNextMinimumBid(), ex.getMinimumRequired());

            // DB không được thay đổi khi bid thất bại
            verify(bidRepo, never()).save(any());
            verify(auctionRepo, never()).updateCurrentBid(anyInt(), any(), anyInt());
        }

        @Test
        @DisplayName("Số dư ví không đủ → InsufficientBalanceException")
        void placeBid_insufficientBalance_throws() throws Exception {
            PhysicalItem item      = makeActiveItem();
            Buyer        poorBuyer = makeBuyer(BUYER_ID, new BigDecimal("10000")); // chỉ có 10k
            BigDecimal   bidAmount = STARTING_PRICE.add(INCREMENT);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(poorBuyer));

            assertThrows(InsufficientBalanceException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, bidAmount));

            verify(bidRepo, never()).save(any());
        }

        @Test
        @DisplayName("Phiên đã CLOSED → AuctionClosedException")
        void placeBid_closedAuction_throwsAuctionClosedException() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.setStatus(AuctionStatus.CLOSED);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThrows(AuctionClosedException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT)));

            verify(bidRepo, never()).save(any());
        }

        @Test
        @DisplayName("Phiên PENDING (chưa bắt đầu) → AuctionClosedException")
        void placeBid_pendingAuction_throwsAuctionClosedException() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.setStatus(AuctionStatus.PENDING);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThrows(AuctionClosedException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT)));
        }

        @Test
        @DisplayName("Seller tự bid vào item của mình → IllegalStateException")
        void placeBid_sellerBidsOwnItem_throwsIllegalStateException() throws Exception {
            // SELLER_ID == item.getSellerId() → bị chặn bởi validateBidder()
            PhysicalItem item = makeActiveItem();
            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThrows(IllegalStateException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, SELLER_ID, STARTING_PRICE.add(INCREMENT)));
        }

        @Test
        @DisplayName("Phiên đã hết giờ nhưng DB vẫn ACTIVE → tự closeAuction() rồi throw")
        void placeBid_expiredButActiveInDb_autoClosesAndThrows() throws Exception {
            // Mô phỏng race condition: AuctionScheduler chưa kịp update
            PhysicalItem item = makeActiveItem();
            item.setEndTime(LocalDateTime.now().minusMinutes(5)); // đã hết 5 phút trước
            item.setStatus(AuctionStatus.ACTIVE); // DB vẫn ACTIVE

            // findById được gọi ≥ 2 lần: 1 lần trong loadActiveItem, 1 lần trong closeAuction
            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThrows(AuctionClosedException.class,
                    () -> auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT)));

            // closeAuction phải được gọi để đồng bộ DB
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
        @DisplayName("Có bid → CLOSED, thanh toán, observer nhận BUYER_ID")
        void closeAuction_hasBids_closesWithPayment() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.updateCurrentBid(STARTING_PRICE.add(INCREMENT), BUYER_ID); // simulate 1 bid

            Buyer  buyer  = makeBuyer(BUYER_ID, BUYER_BALANCE);
            Seller seller = makeSeller(SELLER_ID);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(userRepo.findById(SELLER_ID)).thenReturn(Optional.of(seller));

            AuctionObserver observer = mock(AuctionObserver.class);
            auctionHouse.addObserver(observer);

            auctionHouse.closeAuction(ITEM_ID);

            // Status phải CLOSED
            verify(auctionRepo).updateStatus(ITEM_ID, AuctionStatus.CLOSED);

            // Buyer bị trừ tiền → updateWalletBalance được gọi
            verify(userRepo).updateWalletBalance(eq(BUYER_ID), any());

            // Observer notify với đúng winnerId
            verify(observer).onAuctionClosed(eq(item), eq(BUYER_ID));
        }

        @Test
        @DisplayName("Không có bid → EXPIRED, observer nhận null winner")
        void closeAuction_noBids_setsExpired() throws Exception {
            PhysicalItem item = makeActiveItem();
            // leadingBidderId = 0 mặc định → hasBids() = false

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            AuctionObserver observer = mock(AuctionObserver.class);
            auctionHouse.addObserver(observer);

            auctionHouse.closeAuction(ITEM_ID);

            verify(auctionRepo).updateStatus(ITEM_ID, AuctionStatus.EXPIRED);
            verify(observer).onAuctionClosed(eq(item), isNull());

            // Không có payment nếu không có bid
            verifyNoInteractions(userRepo);
        }

        @Test
        @DisplayName("Item đã CLOSED → bỏ qua, không làm gì thêm")
        void closeAuction_alreadyClosed_doesNothing() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.setStatus(AuctionStatus.CLOSED);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            auctionHouse.closeAuction(ITEM_ID);

            // Không có update nào được gọi
            verify(auctionRepo, never()).updateStatus(anyInt(), any());
        }

        @Test
        @DisplayName("Item EXPIRED → bỏ qua")
        void closeAuction_expired_doesNothing() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.setStatus(AuctionStatus.EXPIRED);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            auctionHouse.closeAuction(ITEM_ID);

            verify(auctionRepo, never()).updateStatus(anyInt(), any());
        }
    }

    // =========================================================================
    // cancelAuction() tests
    // =========================================================================

    @Nested
    @DisplayName("cancelAuction()")
    class CancelAuctionTests {

        @Test
        @DisplayName("Cancel hợp lệ: đúng seller, chưa có bid → CANCELLED + notify")
        void cancelAuction_validRequest_cancels() throws Exception {
            PhysicalItem item = makeActiveItem();
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
            item.updateCurrentBid(STARTING_PRICE.add(INCREMENT), BUYER_ID);

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

            assertThrows(SecurityException.class,
                    () -> auctionHouse.cancelAuction(ITEM_ID, 999));
        }

        @Test
        @DisplayName("Cancel phiên đã CLOSED → IllegalStateException")
        void cancelAuction_alreadyClosed_throwsIllegalStateException() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.setStatus(AuctionStatus.CLOSED);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));

            assertThrows(IllegalStateException.class,
                    () -> auctionHouse.cancelAuction(ITEM_ID, SELLER_ID));
        }
    }

    // =========================================================================
    // listItem() tests
    // =========================================================================

    @Nested
    @DisplayName("listItem()")
    class ListItemTests {

        @Test
        @DisplayName("startTime đã qua → ACTIVE, auctionRepo.save() được gọi")
        void listItem_startTimeInPast_setsActive() throws Exception {
            PhysicalItem item = new PhysicalItem(
                    0, SELLER_ID, "Điện thoại Samsung", "Mô tả",
                    "Electronics", STARTING_PRICE, INCREMENT,
                    LocalDateTime.now().minusSeconds(30), // bắt đầu vừa rồi
                    LocalDateTime.now().plusHours(2)
            );

            auctionHouse.listItem(item);

            assertEquals(AuctionStatus.ACTIVE, item.getStatus());
            verify(auctionRepo).save(item);
        }

        @Test
        @DisplayName("startTime trong tương lai → PENDING")
        void listItem_futureStartTime_setsPending() throws Exception {
            PhysicalItem item = new PhysicalItem(
                    0, SELLER_ID, "Đồng hồ Rolex", "Mô tả",
                    "Fashion", STARTING_PRICE, INCREMENT,
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusHours(4)
            );

            auctionHouse.listItem(item);

            assertEquals(AuctionStatus.PENDING, item.getStatus());
            verify(auctionRepo).save(item);
        }

        @Test
        @DisplayName("Item không hợp lệ (title rỗng) → IllegalArgumentException, không save")
        void listItem_invalidItem_throws() throws Exception {
            PhysicalItem item = new PhysicalItem();
            item.setSellerId(SELLER_ID);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusHours(2));
            // title = null → isValidItem() = false

            assertThrows(IllegalArgumentException.class,
                    () -> auctionHouse.listItem(item));

            verify(auctionRepo, never()).save(any());
        }

        @Test
        @DisplayName("Thời gian đấu giá quá ngắn → IllegalArgumentException")
        void listItem_durationTooShort_throws() throws Exception {
            PhysicalItem item = new PhysicalItem(
                    0, SELLER_ID, "Item ngắn hạn", "Mô tả",
                    "Other", STARTING_PRICE, INCREMENT,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusMinutes(1) // chỉ 1 phút — quá ngắn
            );

            assertThrows(IllegalArgumentException.class,
                    () -> auctionHouse.listItem(item));
        }
    }

    // =========================================================================
    // Anti-sniping tests
    // =========================================================================

    @Nested
    @DisplayName("Anti-sniping (gia hạn cuối giờ)")
    class AntiSnipingTests {

        @Test
        @DisplayName("Bid trong 30 giây cuối → updateEndTime được gọi")
        void placeBid_lastSeconds_extendsEndTime() throws Exception {
            PhysicalItem item = makeActiveItem();
            item.setEndTime(LocalDateTime.now().plusSeconds(10)); // còn 10s < window 30s

            Buyer buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT));

            // Anti-snipe phải gọi updateEndTime để gia hạn
            verify(auctionRepo).updateEndTime(eq(ITEM_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Bid khi còn hơn 30 giây → endTime KHÔNG thay đổi")
        void placeBid_withTimeRemaining_noExtension() throws Exception {
            PhysicalItem item  = makeActiveItem(); // endTime = now + 1h
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT));

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
        @DisplayName("Observer bị remove → không nhận notify sau đó")
        void observer_afterRemove_notNotified() throws Exception {
            PhysicalItem item  = makeActiveItem();
            Buyer        buyer = makeBuyer(BUYER_ID, BUYER_BALANCE);

            when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
            when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
            when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            AuctionObserver observer = mock(AuctionObserver.class);
            auctionHouse.addObserver(observer);
            auctionHouse.removeObserver(observer);

            auctionHouse.placeBid(ITEM_ID, BUYER_ID, STARTING_PRICE.add(INCREMENT));

            verify(observer, never()).onNewBid(any(), any());
        }

        @Test
        @DisplayName("Nhiều observer → tất cả đều nhận notify")
        void observer_multipleObservers_allNotified() throws Exception {
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

            verify(obs1).onNewBid(eq(item), any(Bid.class));
            verify(obs2).onNewBid(eq(item), any(Bid.class));
        }
    }

    // =========================================================================
    // ArgumentCaptor test — kiểm tra chính xác dữ liệu lưu vào DB
    // =========================================================================

    @Test
    @DisplayName("ArgumentCaptor: Bid được lưu có đúng auctionId, buyerId, amount")
    void placeBid_capturesSavedBidWithCorrectData() throws Exception {
        PhysicalItem item      = makeActiveItem();
        Buyer        buyer     = makeBuyer(BUYER_ID, BUYER_BALANCE);
        BigDecimal   bidAmount = new BigDecimal("1200000");

        when(auctionRepo.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(userRepo.findById(BUYER_ID)).thenReturn(Optional.of(buyer));
        when(bidRepo.findActiveAutoBidsExcluding(anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        // ArgumentCaptor "chụp" object truyền vào save() để kiểm tra từng field
        ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);

        auctionHouse.placeBid(ITEM_ID, BUYER_ID, bidAmount);

        verify(bidRepo).save(bidCaptor.capture());
        Bid savedBid = bidCaptor.getValue();

        assertEquals(ITEM_ID,   savedBid.getAuctionId(), "AuctionId phải đúng");
        assertEquals(BUYER_ID,  savedBid.getBuyerId(),   "BuyerId phải đúng");
        assertEquals(bidAmount, savedBid.getAmount(),    "Amount phải đúng");
        assertFalse(savedBid.isAutoBid(), "Bid thủ công không đánh dấu auto-bid");
    }
}
