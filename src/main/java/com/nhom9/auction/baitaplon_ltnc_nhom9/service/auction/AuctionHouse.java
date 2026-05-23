package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuctionClosedException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.BidTooLowException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Lõi nghiệp vụ đấu giá – điều phối bid, buy-now, close, cancel.
 * Implements Auctionable + Subject trong Observer pattern.
 *
 * [AUTO-BID] Proxy Bidding Engine:
 *   - Hệ thống KHÔNG đặt ngay mức tối đa của người dùng
 *   - Chỉ tăng lên đủ để dẫn đầu (currentPrice + increment)
 *   - Khi 2 người đều có auto-bid → resolveAutoBidConflict() xử lý ngay lập tức
 *     (không loop từng bước, tránh tạo hàng trăm rows DB thừa)
 *   - Khi chỉ 1 người có auto-bid → triggerAutoBids() counter 1 lần rồi dừng
 */
public class AuctionHouse implements Auctionable {

    private static final Logger LOG = Logger.getLogger(AuctionHouse.class.getName());

    private final AuctionRepository     auctionRepo;
    private final BidRepository         bidRepo;
    private final UserRepository        userRepo;

    private final List<AuctionObserver> observers = new ArrayList<>();

    public AuctionHouse(AuctionRepository auctionRepo, BidRepository bidRepo,
                        UserRepository userRepo) {
        this.auctionRepo = auctionRepo;
        this.bidRepo  = bidRepo;
        this.userRepo = userRepo;
    }

    // ─── Observer Registration ────────────────────────────────────────────────

    public void addObserver(AuctionObserver o)    { observers.add(o); }
    public void removeObserver(AuctionObserver o) { observers.remove(o); }

    private void notifyNewBid(AuctionItem item, Bid bid) {
        observers.forEach(o -> o.onNewBid(item, bid));
    }
    private void notifyClosed(AuctionClosedEvent event) {
        observers.forEach(o -> o.onAuctionClosed(event));
    }
    private void notifyStarted(AuctionItem item) {
        observers.forEach(o -> o.onAuctionStarted(item));
    }
    private void notifyCancelled(AuctionItem item) {
        observers.forEach(o -> o.onAuctionCancelled(item));
    }
    private void notifyExtended(AuctionItem item, LocalDateTime newEndTime) {
        observers.forEach(o -> o.onAuctionExtended(item, newEndTime));
    }

    // ─── Place Bid ────────────────────────────────────────────────────────────

    @Override
    public synchronized Bid placeBid(int itemId, int bidderId, BigDecimal amount)
            throws AuctionClosedException, BidTooLowException, InsufficientBalanceException, Exception {

        AuctionItem item = loadActiveItem(itemId);
        validateBidder(item, bidderId);

        if (!item.isValidBid(amount))
            throw new BidTooLowException(amount, item.getNextMinimumBid());

        Buyer buyer = loadBuyer(bidderId);
        if (!buyer.hasSufficientBalance(amount))
            throw new InsufficientBalanceException(buyer.getWalletBalance(), amount);

        // Tạo và lưu bid
        Bid bid = new Bid(itemId, bidderId, amount);
        bid.setBuyerUsername(buyer.getUsername());
        bidRepo.save(bid);

        // Cập nhật giá hiện tại
        item.updateCurrentBid(amount, bidderId);
        auctionRepo.updateCurrentBid(itemId, amount, bidderId);

        LOG.info(String.format("Bid mới: item #%d, buyer=%s, amount=%,.0f đ",
                itemId, buyer.getUsername(), amount));

        // Kiểm tra gia hạn nếu bid trong phút cuối
        extendIfLastMinute(item);

        notifyNewBid(item, bid);

        // Người bid thủ công vừa vượt người dẫn đầu.
        // Nếu người dẫn đầu cũ có auto-bid → hệ thống tự counter lại 1 lần.
        // (Bid thủ công không bao giờ tạo ra xung đột 2 auto-bid với nhau,
        //  vì bản thân bid này không phải auto-bid.)
        triggerAutoBids(item, bidderId);

        return bid;
    }

    // ─── Auto Bid ─────────────────────────────────────────────────────────────

    /**
     * Đặt auto-bid (proxy bid) cho người dùng.
     *
     * Người dùng chỉ cần nhập số tiền TỐI ĐA họ sẵn sàng trả (maxLimit).
     * Hệ thống sẽ:
     *   1. Ghi nhớ previousLeaderId (người đang dẫn đầu TRƯỚC khi B đặt)
     *   2. Đặt bid ở mức TỐI THIỂU hiện tại (không tiết lộ limit của người dùng)
     *   3. Kiểm tra: previousLeader có auto-bid không?
     *      - Có  → resolveAutoBidConflict() so sánh limit ngay, tính kết quả cuối cùng
     *      - Không → triggerAutoBids() kiểm tra nhưng không counter được → dừng
     *
     * Tại sao cần capture previousLeaderId TRƯỚC khi gọi updateCurrentBid()?
     * → Sau updateCurrentBid(), item.getLeadingBidderId() = bidderId (người mới).
     *   Nếu capture sau, sẽ không tìm được đúng auto-bid của người dẫn đầu cũ.
     *
     * Ví dụ (increment = 1.000đ):
     *   Giá hiện tại: 50.000đ
     *   A đặt auto limit 100.000đ → hệ thống đặt A: 51.000đ, A dẫn đầu
     *   B đặt auto limit 200.000đ:
     *     - previousLeaderId = A
     *     - B bid: 52.000đ, B dẫn đầu
     *     - resolveAutoBidConflict(): B (200k) > A (100k) → B thắng
     *     - Ghi bid cuối của A: 100.000đ
     *     - Ghi bid thắng của B: 101.000đ ✅
     *   → Chỉ tạo 4 rows tổng cộng, không loop 50 lần
     */
    @Override
    public synchronized Bid placeAutoBid(int itemId, int bidderId, BigDecimal maxLimit)
            throws AuctionClosedException, BidTooLowException, InsufficientBalanceException, Exception {

        AuctionItem item = loadActiveItem(itemId);
        validateBidder(item, bidderId);

        Buyer buyer = loadBuyer(bidderId);
        if (!buyer.hasSufficientBalance(maxLimit))
            throw new InsufficientBalanceException(buyer.getWalletBalance(), maxLimit);

        BigDecimal firstBid = item.getNextMinimumBid();
        if (maxLimit.compareTo(firstBid) < 0)
            throw new BidTooLowException(maxLimit, firstBid);

        // ── Bước quan trọng: capture người dẫn đầu CŨ trước khi đặt bid ──
        // Sau updateCurrentBid() bên dưới, leadingBidderId sẽ thành bidderId.
        // previousLeaderId = 0 có nghĩa là chưa có ai bid (phiên mới mở).
        int previousLeaderId = item.getLeadingBidderId();

        // ── Phát hiện xung đột auto-bid TRƯỚC khi lưu bid của B ──────────────
        //
        // Lý do KHÔNG lưu firstBid của B trước rồi resolve sau:
        //   Nếu lưu B ở firstBid (thấp) trước, lịch sử sẽ có điểm giá thấp bất thường
        //   → biểu đồ dip xuống rồi vọt lên, trông vô lý với app đấu giá.
        //   Thay vào đó, resolveAutoBidConflict() tự quyết định ghi bid của B ở mức đúng.
        //
        // Ví dụ các trường hợp:
        //   previousLeaderId = 0  → Phiên mới, B là người đầu tiên → lưu firstBid bình thường
        //   previousLeaderId = A, A không có auto-bid → lưu firstBid, B dẫn đầu
        //   previousLeaderId = A, A có auto-bid → resolveAutoBidConflict() tự lưu bid đúng
        if (previousLeaderId > 0 && previousLeaderId != bidderId) {
            Optional<Bid> rivalAutoBid = bidRepo.findTopAutoBidByBuyer(itemId, previousLeaderId);

            if (rivalAutoBid.isPresent()) {
                // Cả 2 đều có auto-bid → giải quyết xung đột ngay lập tức.
                // Tạo bid object của B nhưng CHƯA lưu vào DB — resolveAutoBidConflict()
                // sẽ lưu B ở mức giá đúng (không phải firstBid thấp).
                Bid bid = new Bid(itemId, bidderId, firstBid);
                bid.setBuyerUsername(buyer.getUsername());
                bid.setAutoBid(true);
                bid.setAutoBidLimit(maxLimit);
                // Không gọi bidRepo.save(bid) ở đây — resolve sẽ tự lưu với amount đúng

                resolveAutoBidConflict(item, bid, rivalAutoBid.get());
                return bid;
            }
        }

        // Không có xung đột: lưu bid của B ở firstBid bình thường
        Bid bid = new Bid(itemId, bidderId, firstBid);
        bid.setBuyerUsername(buyer.getUsername());
        bid.setAutoBid(true);
        bid.setAutoBidLimit(maxLimit);
        bidRepo.save(bid);

        item.updateCurrentBid(firstBid, bidderId);
        auctionRepo.updateCurrentBid(itemId, firstBid, bidderId);

        LOG.info(String.format("Auto-bid đặt: item #%d, buyer=%s, đặt=%,.0fđ, limit=%,.0fđ",
                itemId, buyer.getUsername(), firstBid, maxLimit));

        notifyNewBid(item, bid);

        // Nếu có người đứng sau B không có auto-bid nhưng B vừa vượt qua,
        // triggerAutoBids sẽ không tìm thấy auto-bid → dừng ngay
        if (previousLeaderId > 0 && previousLeaderId != bidderId) {
            triggerAutoBids(item, bidderId);
        }

        return bid;
    }

    // ─── Auto Bid Conflict Resolution (Short-circuit) ─────────────────────────

    /**
     * Giải quyết xung đột khi 2 người CÙNG có auto-bid trên một phiên.
     *
     * ═══ Tại sao cần method này? ═══
     *
     * Nếu dùng triggerAutoBids() để xử lý 2 auto-bid counter nhau từng bước,
     * sẽ tạo ra rất nhiều rows DB thừa:
     *   Ví dụ: A limit=100.000đ, B limit=200.000đ, increment=1.000đ
     *   → triggerAutoBids() loop ~50 lần, tạo 50 rows trong bảng bids
     *   → Chậm, tốn DB, khó debug
     *
     * resolveAutoBidConflict() thay thế bằng cách so sánh limit trực tiếp:
     *   → Tính ngay kết quả cuối, chỉ tạo 2 rows (bid cuối của người thua + bid thắng)
     *   → Nhanh hơn, sạch hơn, dễ hiểu hơn
     *
     * ═══ Logic xử lý ═══
     *
     * Trường hợp 1: newBid.limit > existingBid.limit (B > A)
     *   → B thắng, giá cuối = A.limit + increment
     *   → Ghi bid cuối của A tại A.limit (A đã "dùng hết" limit)
     *   → Ghi bid thắng của B tại A.limit + increment
     *
     * Trường hợp 2: newBid.limit < existingBid.limit (B < A)
     *   → A vẫn thắng, giá tăng thêm vì B vừa thử counter
     *   → Giá mới = B.limit + increment (A counter qua B)
     *   → Ghi bid counter của A tại mức đó (nếu vẫn trong limit A)
     *
     * Trường hợp 3: newBid.limit == existingBid.limit
     *   → Người đặt TRƯỚC thắng (A đặt trước → A thắng)
     *   → Giá không thay đổi, không cần ghi thêm bid
     *
     * QUAN TRỌNG: newBid được truyền vào CHƯA được lưu vào DB.
     * Method này tự quyết định lưu newBid ở mức giá đúng trong từng trường hợp,
     * đảm bảo lịch sử đấu giá luôn tăng dần (không bao giờ có điểm dip).
     *
     * @param item          vật phẩm đang đấu giá
     * @param newBid        auto-bid của B (người mới) — CHƯA được lưu vào DB
     * @param existingBid   auto-bid tốt nhất của A (người đang dẫn đầu trước đó)
     */
    private void resolveAutoBidConflict(AuctionItem item, Bid newBid, Bid existingBid)
            throws Exception {

        BigDecimal increment  = item.getMinBidIncrement();
        BigDecimal newLimit   = newBid.getAutoBidLimit();       // limit của B (người mới)
        BigDecimal oldLimit   = existingBid.getAutoBidLimit();  // limit của A (người cũ)

        Buyer newBuyer = loadBuyer(newBid.getBuyerId());
        Buyer oldBuyer = loadBuyer(existingBid.getBuyerId());

        int cmp = newLimit.compareTo(oldLimit);

        if (cmp > 0) {
            // ── Trường hợp 1: B có limit CAO hơn A → B thắng ──────────────────
            //
            // Lịch sử mong muốn: ...giá_cũ(A) → A.limit(A) → A.limit+increment(B)
            // → Biểu đồ luôn đi lên ✅
            //
            // Ví dụ: A đang dẫn đầu ở 800.000đ, A.limit=2.000.000đ, B.limit=2.400.000đ
            //   → Ghi A tại 2.000.000đ (A dùng hết limit)
            //   → Ghi B tại 2.001.000đ (B thắng, vừa đủ hơn A 1 increment)
            //   → Biểu đồ: 800K → 2.0M → 2.001M  ✅ không bao giờ dip
            BigDecimal finalPrice = oldLimit.add(increment);

            // Ghi bid cuối của A ở mức limit (A "dùng hết" giới hạn)
            if (oldBuyer.hasSufficientBalance(oldLimit)) {
                Bid aFinalBid = new Bid(item.getId(), existingBid.getBuyerId(), oldLimit);
                aFinalBid.setBuyerUsername(oldBuyer.getUsername());
                aFinalBid.setAutoBid(true);
                aFinalBid.setAutoBidLimit(oldLimit);
                bidRepo.save(aFinalBid);
                item.updateCurrentBid(oldLimit, existingBid.getBuyerId());
                auctionRepo.updateCurrentBid(item.getId(), oldLimit, existingBid.getBuyerId());
                LOG.info(String.format(
                        "Auto-bid conflict: %s dùng hết limit, bid cuối=%,.0fđ",
                        oldBuyer.getUsername(), oldLimit));
                notifyNewBid(item, aFinalBid);
            }

            // Ghi bid thắng của B ở finalPrice (B dẫn đầu, vừa đủ hơn A)
            // newBid chưa được lưu vào DB → lưu tại đây với amount = finalPrice
            newBid.setAmount(finalPrice);
            bidRepo.save(newBid);

            item.updateCurrentBid(finalPrice, newBid.getBuyerId());
            auctionRepo.updateCurrentBid(item.getId(), finalPrice, newBid.getBuyerId());

            LOG.info(String.format(
                    "Auto-bid conflict resolved: %s (limit=%,.0fđ) thắng %s (limit=%,.0fđ) → giá=%,.0fđ",
                    newBuyer.getUsername(), newLimit,
                    oldBuyer.getUsername(), oldLimit,
                    finalPrice));

            notifyNewBid(item, newBid);

        } else if (cmp < 0) {
            // ── Trường hợp 2: A có limit CAO hơn B → A vẫn thắng ─────────────
            //
            // Lịch sử mong muốn: ...giá_cũ(A) → B.limit(B) → B.limit+increment(A)
            // → Biểu đồ luôn đi lên ✅
            //
            // Ví dụ: A đang dẫn đầu ở 800.000đ, A.limit=2.400.000đ, B.limit=2.000.000đ
            //   → Ghi B tại 2.000.000đ (B dùng hết limit)
            //   → Ghi A counter tại 2.001.000đ (A thắng)
            //   → Biểu đồ: 800K → 2.0M → 2.001M  ✅ không bao giờ dip

            // ── FIX BUG #3: counterPrice phải được cap tại oldLimit ────────────
            //
            // Vấn đề gốc: counterPrice = newLimit + increment
            // Khi increment lớn hơn khoảng cách (oldLimit - newLimit),
            // counterPrice vượt qua oldLimit → điều kiện `counterPrice <= oldLimit`
            // trả về FALSE → A không counter → B thắng DÙ B.limit < A.limit.
            //
            // Ví dụ bug:
            //   A.limit=120.000, B.limit=110.000, increment=15.000
            //   counterPrice = 110.000 + 15.000 = 125.000 > 120.000 → A thua oan!
            //   Nhưng A hoàn toàn có thể bid 111.000đ để thắng B.
            //
            // Fix: counterPrice = min(newLimit + increment, oldLimit)
            //   → A counter ở mức tối thiểu đủ để thắng B, không bao giờ vượt limit
            //   → Nếu oldLimit nằm giữa [newLimit, newLimit+increment):
            //      A counter đúng bằng oldLimit (A dùng hết limit nhưng vẫn thắng)
            //   → Biểu đồ: ...→ B.limit → A.limit  (hoặc B.limit+increment nếu đủ room)
            //              luôn tăng ✅
            BigDecimal counterPrice = newLimit.add(increment).min(oldLimit);

            // Ghi bid của B ở mức limit thực của B (không phải firstBid thấp)
            // newBid chưa được lưu vào DB → lưu tại đây với amount = newLimit
            newBid.setAmount(newLimit);
            bidRepo.save(newBid);
            item.updateCurrentBid(newLimit, newBid.getBuyerId());
            auctionRepo.updateCurrentBid(item.getId(), newLimit, newBid.getBuyerId());
            LOG.info(String.format(
                    "Auto-bid conflict: ghi bid %s tại limit=%,.0fđ",
                    newBuyer.getUsername(), newLimit));
            notifyNewBid(item, newBid);

            // A counter lên counterPrice để dẫn đầu lại.
            // counterPrice đã được cap tại oldLimit nên điều kiện <= luôn đúng
            // — chỉ còn cần kiểm tra số dư ví.
            if (oldBuyer.hasSufficientBalance(counterPrice)) {

                Bid aCounterBid = new Bid(item.getId(), existingBid.getBuyerId(), counterPrice);
                aCounterBid.setBuyerUsername(oldBuyer.getUsername());
                aCounterBid.setAutoBid(true);
                aCounterBid.setAutoBidLimit(oldLimit);
                bidRepo.save(aCounterBid);

                item.updateCurrentBid(counterPrice, existingBid.getBuyerId());
                auctionRepo.updateCurrentBid(item.getId(), counterPrice, existingBid.getBuyerId());

                LOG.info(String.format(
                        "Auto-bid conflict resolved: %s (limit=%,.0fđ) giữ vị trí, counter lên %,.0fđ",
                        oldBuyer.getUsername(), oldLimit, counterPrice));

                notifyNewBid(item, aCounterBid);
            } else {
                LOG.warning(String.format(
                        "Auto-bid conflict: %s không đủ số dư để counter %,.0fđ → B (%s) thắng",
                        oldBuyer.getUsername(), counterPrice, newBuyer.getUsername()));
            }

        } else {
            // ── Trường hợp 3: Cùng limit → người đặt TRƯỚC thắng (A) ──────────
            //
            // Lịch sử mong muốn: ...giá_cũ(A) → B.limit(B) → B.limit+increment(A)
            // → Biểu đồ luôn đi lên ✅
            //
            // Ví dụ: A.limit=B.limit=100.000đ, giá hiện tại 50.000đ, increment=1.000đ
            //   → Ghi B tại 100.000đ, A counter tại 101.000đ → A dẫn đầu
            BigDecimal tieBreakPrice = newLimit.add(increment); // = oldLimit + increment

            // ── FIX BUG #1: Guard chống DIP ──────────────────────────────────
            //
            // Vấn đề: nếu limit của 2 người ĐÃ thấp hơn giá hiện tại
            // (ví dụ giá đang là 210k, cả 2 cùng limit 200k),
            // ghi newBid.amount = newLimit = 200k sẽ kéo currentPrice XUỐNG
            // → biểu đồ đường giá bị DIP — điều vô lý với app đấu giá.
            //
            // Nguyên nhân root: B không được phép đặt auto-bid với limit
            // thấp hơn giá hiện tại. Validation ở placeAutoBid() đã chặn
            // trường hợp B.limit < nextMinimumBid khi B MỚI ĐẶT.
            // Nhưng nếu giá leo qua limit sau đó (do bid thủ công của người
            // khác), thì auto-bid đã lưu của A/B trở thành "stale" và không
            // bao giờ được trigger nữa — đây là trường hợp bình thường.
            //
            // Tuy nhiên nếu resolveAutoBidConflict() được gọi khi cả 2 limit
            // đều < currentPrice (edge case hiếm nhưng có thể xảy ra nếu giá
            // leo lên bởi bid thủ công sau khi B đã đặt auto), ta phải bỏ qua
            // và không ghi bất kỳ bid nào để tránh DIP.
            if (newLimit.compareTo(item.getCurrentPrice()) < 0) {
                LOG.info(String.format(
                        "Auto-bid tie: cả 2 limit (%,.0fđ) thấp hơn giá hiện tại (%,.0fđ)" +
                                " → bỏ qua, A (%s) giữ nguyên vị trí dẫn đầu.",
                        newLimit, item.getCurrentPrice(), oldBuyer.getUsername()));
                return;
            }

            // Ghi bid của B ở mức limit (dùng hết giới hạn, thua tie-break)
            newBid.setAmount(newLimit);
            bidRepo.save(newBid);
            item.updateCurrentBid(newLimit, newBid.getBuyerId());
            auctionRepo.updateCurrentBid(item.getId(), newLimit, newBid.getBuyerId());
            notifyNewBid(item, newBid);

            if (tieBreakPrice.compareTo(oldLimit) <= 0
                    && oldBuyer.hasSufficientBalance(tieBreakPrice)) {

                Bid aTieBreakBid = new Bid(item.getId(), existingBid.getBuyerId(), tieBreakPrice);
                aTieBreakBid.setBuyerUsername(oldBuyer.getUsername());
                aTieBreakBid.setAutoBid(true);
                aTieBreakBid.setAutoBidLimit(oldLimit);
                bidRepo.save(aTieBreakBid);

                item.updateCurrentBid(tieBreakPrice, existingBid.getBuyerId());
                auctionRepo.updateCurrentBid(item.getId(), tieBreakPrice, existingBid.getBuyerId());

                LOG.info(String.format(
                        "Auto-bid tie (cùng limit=%,.0fđ): %s thắng vì đặt trước, giá=%,.0fđ",
                        oldLimit, oldBuyer.getUsername(), tieBreakPrice));

                notifyNewBid(item, aTieBreakBid);
            } else {
                // tieBreakPrice > limit: A không thể counter qua B.
                // B thắng tie-break tình cờ — giá dừng ở newLimit (đã ghi ở trên).
                // Không cần log warning vì đây là kết quả hợp lệ:
                //   cả 2 cùng limit, giá đã đúng bằng limit, A hết room.
                LOG.info(String.format(
                        "Auto-bid tie: %s không counter được (tieBreakPrice=%,.0fđ > limit=%,.0fđ)" +
                                " → %s thắng tình cờ ở %,.0fđ",
                        oldBuyer.getUsername(), tieBreakPrice, oldLimit,
                        newBuyer.getUsername(), newLimit));
            }
        }
    }

    // ─── Auto Bid - Proxy Bidding Engine (dùng cho bid thủ công) ─────────────

    /**
     * Sau khi có bid THỦ CÔNG mới, tìm và kích hoạt auto-bid của người đang dẫn đầu cũ.
     *
     * ═══ Cơ chế Proxy Bidding ═══
     *
     * Nguyên tắc:
     *   - Chỉ tăng lên MỨC TỐI THIỂU cần thiết để dẫn đầu (currentPrice + increment)
     *   - Nếu người dẫn đầu cũ có auto-bid → counter 1 lần rồi dừng
     *   - Method này KHÔNG xử lý 2 auto-bid gặp nhau (dùng resolveAutoBidConflict() cho đó)
     *
     * Ví dụ (increment = 100.000đ):
     *   A đặt auto limit:  5.000.000đ  → hệ thống đặt cho A: 1.100.000đ
     *   B bid thủ công:    2.000.000đ  → triggerAutoBids → A counter: 2.100.000đ ✅
     *   B bid thủ công:    4.900.000đ  → triggerAutoBids → A counter: 5.000.000đ ✅
     *   B bid thủ công:    5.500.000đ  → triggerAutoBids → A muốn 5.600.000đ > limit → A thua ✅
     *
     * @param item          vật phẩm đang đấu giá (đã được cập nhật currentPrice)
     * @param lastBidderId  người vừa đặt bid thủ công (không trigger cho chính họ)
     */
    private void triggerAutoBids(AuctionItem item, int lastBidderId) throws Exception {
        BigDecimal increment = item.getMinBidIncrement();

        // Tìm auto-bid tốt nhất (limit cao nhất) của những người KHÁC người vừa bid
        List<Bid> rivals = bidRepo.findActiveAutoBidsExcluding(item.getId(), lastBidderId);

        if (rivals.isEmpty()) {
            // Không có đối thủ nào có auto-bid → không cần counter
            return;
        }

        // Lấy đối thủ có limit cao nhất (đứng đầu danh sách)
        Bid topRival = rivals.get(0);
        BigDecimal needed = item.getCurrentPrice().add(increment); // Giá cần để dẫn đầu lại

        // Kiểm tra rival có đủ limit để counter không
        if (topRival.getAutoBidLimit().compareTo(needed) < 0) {
            LOG.info(String.format(
                    "Auto-bid trigger: %s hết limit (limit=%,.0fđ < needed=%,.0fđ) → thua",
                    topRival.getBuyerUsername(), topRival.getAutoBidLimit(), needed));
            return;
        }

        // Kiểm tra số dư ví
        Buyer rivalBuyer = loadBuyer(topRival.getBuyerId());
        if (!rivalBuyer.hasSufficientBalance(needed)) {
            LOG.info(String.format(
                    "Auto-bid trigger: %s không đủ số dư để counter %,.0fđ → bỏ qua",
                    rivalBuyer.getUsername(), needed));
            return;
        }

        // Tính giá counter: đặt ở mức TỐI THIỂU để dẫn đầu, nhưng không vượt limit
        BigDecimal counterAmount = needed.min(topRival.getAutoBidLimit());

        // Lưu bid counter vào DB
        Bid counter = new Bid(item.getId(), topRival.getBuyerId(), counterAmount);
        counter.setBuyerUsername(rivalBuyer.getUsername());
        counter.setAutoBid(true);
        counter.setAutoBidLimit(topRival.getAutoBidLimit()); // Giữ nguyên limit gốc
        bidRepo.save(counter);

        // Cập nhật giá item
        item.updateCurrentBid(counterAmount, topRival.getBuyerId());
        auctionRepo.updateCurrentBid(item.getId(), counterAmount, topRival.getBuyerId());

        LOG.info(String.format(
                "Auto-bid trigger: %s counter lên %,.0fđ (limit=%,.0fđ)",
                rivalBuyer.getUsername(), counterAmount, topRival.getAutoBidLimit()));

        notifyNewBid(item, counter);

        // Sau khi rival counter thành công, bid thủ công của lastBidder đã bị qua.
        // Không cần loop thêm vì:
        //   - Nếu lastBidder là bid thủ công → họ không tự động counter lại
        //   - Nếu có auto-bid nào khác → sẽ được xử lý khi người dùng bid lần tiếp theo
        //
        // Lưu ý: Nếu bạn muốn hỗ trợ nhiều người cùng auto-bid tranh nhau,
        // có thể gọi đệ quy hoặc thêm loop ở đây. Nhưng với 2 người (trường hợp phổ biến),
        // resolveAutoBidConflict() đã xử lý hoàn toàn và triggerAutoBids() chỉ cần 1 lần.
    }

    // ─── Buy Now ──────────────────────────────────────────────────────────────

    // ─── Close Auction ────────────────────────────────────────────────────────

    @Override
    public synchronized void closeAuction(int itemId) throws Exception {
        AuctionItem item = auctionRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy item #" + itemId));

        if (item.getStatus() != AuctionStatus.ACTIVE) {
            LOG.warning("closeAuction gọi trên item không active: #" + itemId);
            return;
        }

        if (!item.hasBids()) {
            // Hết giờ, không có bid → EXPIRED
            item.setStatus(AuctionStatus.EXPIRED);
            auctionRepo.updateStatus(itemId, AuctionStatus.EXPIRED);
            LOG.info("Phiên hết hạn (không có bid): item #" + itemId);

            // Không có winner → balance không đổi → truyền null cho buyer balance
            notifyClosed(new AuctionClosedEvent(item, null, null,
                    item.getSellerId(), null));
            return;
        }

        // Có người thắng
        int winnerId = item.getLeadingBidderId();
        item.setStatus(AuctionStatus.CLOSED);
        auctionRepo.updateStatus(itemId, AuctionStatus.CLOSED);

        // Xử lý thanh toán (trừ tiền buyer, cộng tiền seller)
        // Sau processPayment(), đọc lại balance mới từ DB để gửi về client.
        // Tại sao đọc lại từ DB thay vì tự tính?
        //   → processPayment() đã cập nhật DB và object trong bộ nhớ,
        //     nhưng object đó là local variable trong processPayment().
        //   → Cách an toàn nhất là đọc lại từ DB để đảm bảo chính xác 100%.
        BigDecimal buyerNewBalance  = null;
        BigDecimal sellerNewBalance = null;
        try {
            processPayment(item, winnerId, item.getCurrentPrice());
            LOG.info(String.format("Phiên kết thúc: item #%d, winner #%d, price=%,.0f",
                    itemId, winnerId, item.getCurrentPrice()));

            // Đọc balance mới từ DB sau khi thanh toán thành công
            userRepo.findById(winnerId).ifPresent(u -> {
                // không thể assign vào local final var trong lambda → dùng array trick
            });
            // Dùng cách trực tiếp hơn: load lại từ DB
            var winnerOpt = userRepo.findById(winnerId);
            if (winnerOpt.isPresent()) {
                var winner = winnerOpt.get();
                if (winner instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer b)
                    buyerNewBalance = b.getWalletBalance();
                else if (winner instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller s)
                    buyerNewBalance = s.getEarningsBalance(); // seller thắng: dùng earnings
            }
            var sellerOpt = userRepo.findById(item.getSellerId());
            if (sellerOpt.isPresent() && sellerOpt.get() instanceof
                    com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller s) {
                sellerNewBalance = s.getEarningsBalance();
            }

        } catch (Exception paymentEx) {
            LOG.warning(String.format(
                    "Phiên #%d: đóng thành công nhưng processPayment thất bại"
                            + " (winner=#%d, price=%,.0f) — vẫn gửi notification. Lỗi: %s",
                    itemId, winnerId, item.getCurrentPrice(), paymentEx.getMessage()));
        }

        notifyClosed(new AuctionClosedEvent(
                item, winnerId, buyerNewBalance,
                item.getSellerId(), sellerNewBalance));
    }

    // ─── Close Expired Auctions ───────────────────────────────────────────────

    /**
     * Tìm tất cả phiên ACTIVE đã hết giờ và đóng từng phiên qua closeAuction().
     *
     * <p>Khác với auctionRepo.closeExpiredAuctions() (chỉ UPDATE database),
     * method này gọi closeAuction() cho từng phiên → observer được kích hoạt
     * → notification được gửi đến người dùng.</p>
     *
     * <p>Được gọi từ HomeController mỗi giây khi timer phát hiện phiên hết giờ.</p>
     */
    public void closeExpiredAuctions() throws Exception {
        List<AuctionItem> expired = auctionRepo.findExpiredActive();
        for (AuctionItem item : expired) {
            try {
                closeAuction(item.getId());
            } catch (Exception e) {
                LOG.warning("Không đóng được item #" + item.getId() + ": " + e.getMessage());
            }
        }
    }

    // ─── Cancel Auction ───────────────────────────────────────────────────────

    @Override
    public synchronized void cancelAuction(int itemId, int sellerId) throws Exception {
        AuctionItem item = auctionRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy item #" + itemId));

        if (item.getSellerId() != sellerId)
            throw new SecurityException("Không có quyền huỷ phiên đấu giá này.");

        if (item.hasBids())
            throw new IllegalStateException("Không thể huỷ phiên đã có bid.");

        if (item.getStatus() == AuctionStatus.CLOSED || item.getStatus() == AuctionStatus.EXPIRED)
            throw new IllegalStateException("Phiên đã kết thúc, không thể huỷ.");

        item.setStatus(AuctionStatus.CANCELLED);
        auctionRepo.updateStatus(itemId, AuctionStatus.CANCELLED);

        LOG.info("Phiên bị huỷ: item #" + itemId + " bởi seller #" + sellerId);
        notifyCancelled(item);
    }

    // ─── List Item ────────────────────────────────────────────────────────────

    @Override
    public AuctionItem listItem(AuctionItem item) throws Exception {
        if (!item.isValidItem())
            throw new IllegalArgumentException("Thông tin vật phẩm không hợp lệ.");

        if (item.getStartTime().isBefore(LocalDateTime.now().minusMinutes(1)))
            throw new IllegalArgumentException("Thời gian bắt đầu không hợp lệ.");

        long durationMinutes = java.time.temporal.ChronoUnit.MINUTES
                .between(item.getStartTime(), item.getEndTime());
        if (durationMinutes < AppConfig.MIN_AUCTION_DURATION_MINUTES)
            throw new IllegalArgumentException(
                    "Thời gian đấu giá tối thiểu là " + AppConfig.MIN_AUCTION_DURATION_MINUTES + " phút.");

        // Nếu start ngay bây giờ → ACTIVE, ngược lại → PENDING
        if (!item.getStartTime().isAfter(LocalDateTime.now()))
            item.setStatus(AuctionStatus.ACTIVE);
        else
            item.setStatus(AuctionStatus.PENDING);

        auctionRepo.save(item);
        LOG.info("Đăng vật phẩm: #" + item.getId() + " – " + item.getTitle());

        if (item.getStatus() == AuctionStatus.ACTIVE) notifyStarted(item);
        return item;
    }

    // ─── Internal Payment ─────────────────────────────────────────────────────

    /**
     * Trừ tiền buyer, cộng tiền seller.
     * Transaction history đã bị bỏ — chỉ cập nhật wallet balance.
     */
    private void processPayment(AuctionItem item, int buyerId, BigDecimal totalCost) throws Exception {
        // Load winner trực tiếp từ DB để biết role thực sự (Buyer hay Seller).
        User winner = userRepo.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy winner #" + buyerId));

        // ── Trừ tiền winner — phân biệt theo role ────────────────────────────
        if (winner instanceof Buyer buyer) {
            if (!buyer.hasSufficientBalance(totalCost))
                throw new InsufficientBalanceException(buyer.getWalletBalance(), totalCost);
            buyer.deduct(totalCost);
            userRepo.updateWalletBalance(buyerId, buyer.getWalletBalance());

        } else if (winner instanceof Seller winnerSeller) {
            BigDecimal earnings = winnerSeller.getEarningsBalance() != null
                    ? winnerSeller.getEarningsBalance() : BigDecimal.ZERO;
            if (earnings.compareTo(totalCost) < 0)
                throw new InsufficientBalanceException(earnings, totalCost);
            winnerSeller.setEarningsBalance(earnings.subtract(totalCost));
            userRepo.updateEarningsBalance(buyerId, winnerSeller.getEarningsBalance());

        } else {
            throw new IllegalStateException("Winner #" + buyerId + " có role không hợp lệ.");
        }

        // ── Cộng tiền seller ──────────────────────────────────────────────────
        Seller seller = loadSeller(item.getSellerId());
        seller.receivePayment(item.getCurrentPrice());
        userRepo.updateEarningsBalance(item.getSellerId(), seller.getEarningsBalance());

        LOG.info(String.format("Thanh toán: winner #%d trả %,.0f đ, seller #%d nhận %,.0f đ",
                buyerId, totalCost, item.getSellerId(), item.getCurrentPrice()));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AuctionItem loadActiveItem(int itemId) throws AuctionClosedException, Exception {
        try {
            AuctionItem item = auctionRepo.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy item #" + itemId));

            // Kiểm tra status trong DB trước
            if (item.getStatus() != AuctionStatus.ACTIVE)
                throw new AuctionClosedException(itemId, item.getStatus());

            // ── FIX Bug: Kiểm tra thời gian thực tế, không chỉ status trong DB ──
            //
            // Vấn đề: AuctionScheduler cập nhật status định kỳ, nhưng có độ trễ.
            // Nếu scheduler chưa kịp chạy, phiên đã hết giờ nhưng DB vẫn còn ACTIVE
            // → user vẫn bid được và bid bị lưu vào lịch sử → dữ liệu sai.
            //
            // Giải pháp: Tự kiểm tra endTime ngay tại đây.
            //   - Nếu phiên đã hết giờ → gọi closeAuction() để đồng bộ DB luôn
            //   - Sau đó throw AuctionClosedException để ngăn bid mới.
            if (item.getRemainingSeconds() <= 0) {
                LOG.warning(String.format(
                        "Phiên #%d đã hết giờ nhưng status vẫn là ACTIVE trong DB — tự đóng.", itemId));
                try {
                    closeAuction(itemId);
                } catch (Exception ex) {
                    LOG.warning("closeAuction thất bại khi auto-close: " + ex.getMessage());
                }
                throw new AuctionClosedException(itemId, AuctionStatus.CLOSED);
            }

            return item;
        } catch (AuctionClosedException e) {
            throw e;
        } catch (SQLException e) {
            throw new Exception("Lỗi DB: " + e.getMessage(), e);
        }
    }

    /**
     * Tải thông tin người dùng dưới dạng Buyer để phục vụ việc đặt bid.
     *
     * <p>Hỗ trợ 2 trường hợp:</p>
     * <ul>
     *   <li>{@code Buyer}: trả về trực tiếp.</li>
     *   <li>{@code Seller}: cho phép đặt bid ở sản phẩm của người khác
     *       (việc tự bid cho sản phẩm của mình đã bị chặn tại validateBidder).
     *       Trả về Buyer proxy qua Seller.asBuyer(), dùng earningsBalance
     *       của Seller làm số dư để kiểm tra.</li>
     * </ul>
     *
     * <p>Admin và các role khác không được phép đặt bid.</p>
     */
    private Buyer loadBuyer(int buyerId) throws Exception {
        User user = userRepo.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user #" + buyerId));

        if (user instanceof Buyer buyer) {
            return buyer;
        }

        if (user instanceof Seller seller) {
            // Seller được phép đặt bid cho sản phẩm của người khác.
            // validateBidder() đã chặn trường hợp tự bid cho sản phẩm của mình.
            return seller.asBuyer();
        }

        throw new IllegalStateException("User #" + buyerId + " không có quyền đặt bid.");
    }

    private Seller loadSeller(int sellerId) throws Exception {
        User user = userRepo.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy seller #" + sellerId));
        if (!(user instanceof Seller))
            throw new IllegalStateException("User #" + sellerId + " không phải Seller.");
        return (Seller) user;
    }

    private void validateBidder(AuctionItem item, int bidderId) throws Exception {
        if (item.getSellerId() == bidderId)
            throw new IllegalStateException("Người bán không thể đặt bid cho chính mình.");
    }

    /**
     * Anti-sniping: nếu có bid mới trong {@code ANTI_SNIPE_WINDOW_SECONDS} giây cuối
     * → gia hạn thêm {@code ANTI_SNIPE_EXTENSION_SECONDS} giây và notify UI.
     *
     * <p>Ví dụ với giá trị mặc định (window=30s, extension=60s):</p>
     * <pre>
     *   Kết thúc dự kiến : 20:00:00
     *   19:59:50 có bid  → còn 10s &lt; window 30s → kéo dài đến 20:01:00
     * </pre>
     *
     * Dùng {@link AuctionRepository#updateEndTime} thay vì {@code update(item)}
     * để chỉ ghi đúng 1 cột, tránh ghi đè dữ liệu không liên quan.
     */
    private void extendIfLastMinute(AuctionItem item) throws SQLException {
        if (item.getRemainingSeconds() < AppConfig.ANTI_SNIPE_WINDOW_SECONDS) {
            LocalDateTime newEnd = item.getEndTime()
                    .plusSeconds(AppConfig.ANTI_SNIPE_EXTENSION_SECONDS);
            item.setEndTime(newEnd);
            auctionRepo.updateEndTime(item.getId(), newEnd);
            LOG.info(String.format(
                    "Anti-snipe kích hoạt: item #%d còn %ds → gia hạn +%ds → kết thúc lúc %s",
                    item.getId(), item.getRemainingSeconds(),
                    AppConfig.ANTI_SNIPE_EXTENSION_SECONDS, newEnd));
            notifyExtended(item, newEnd);
        }
    }
}