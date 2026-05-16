package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.factory.ProductCardFactory;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper.AuctionCardMapper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Trang chủ: tải thẻ đấu giá, lọc/tìm kiếm, đồng hồ đếm ngược, kết quả đã kết thúc.
 */
public final class HomeCatalogPresenter {

    private static final Logger LOG = Logger.getLogger(HomeCatalogPresenter.class.getName());

    private final BidRepository bidRepo;
    private final AuctionCardMapper cardMapper;

    private HomeCatalogView view;
    private ProductCardFactory cardFactory;
    private Runnable onResultsReload;

    private final Map<String, Label> timerLabels = new HashMap<>();
    private final Set<String> expiredHandled = new HashSet<>();
    private final List<AuctionCardModel> displayedItems = new ArrayList<>();
    private Button activeChipButton;
    private String activeCategory;
    private ScheduledExecutorService timerScheduler;

    public HomeCatalogPresenter(
            BidRepository bidRepo,
            AuctionCardMapper cardMapper) {
        this.bidRepo = bidRepo;
        this.cardMapper = cardMapper;
    }

    public void bind(HomeCatalogView view, Consumer<String> onPlaceBid, Runnable onResultsReload) {
        this.view = view;
        this.onResultsReload = onResultsReload;
        this.cardFactory = new ProductCardFactory(onPlaceBid, timerLabels);
        activeChipButton = view.chipAll();
    }

    public void startTimers() {
        timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ubid-timer");
            t.setDaemon(true);
            return t;
        });

        timerScheduler.scheduleAtFixedRate(() -> {
            List<AuctionCardModel> snapshot;
            synchronized (displayedItems) {
                snapshot = List.copyOf(displayedItems);
            }

            boolean needsRefresh = false;
            for (AuctionCardModel item : snapshot) {
                if (item.endTime() == null) continue;

                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(), item.endTime()).getSeconds();

                if (secondsLeft <= 0) {
                    if (!expiredHandled.contains(item.id())) {
                        expiredHandled.add(item.id());
                        needsRefresh = true;
                    }
                    final String endedText = "Đã kết thúc";
                    Label hot = timerLabels.get(item.id());
                    if (hot != null) Platform.runLater(() -> hot.setText(endedText));
                    Label sm = timerLabels.get(item.id() + "_sm");
                    if (sm != null) Platform.runLater(() -> sm.setText("⏱  " + endedText));
                } else {
                    long h = secondsLeft / 3600;
                    long m = (secondsLeft % 3600) / 60;
                    long s = secondsLeft % 60;
                    final String fd = String.format("%02d:%02d:%02d", h, m, s);
                    Label hot = timerLabels.get(item.id());
                    if (hot != null) Platform.runLater(() -> hot.setText(fd));
                    Label sm = timerLabels.get(item.id() + "_sm");
                    if (sm != null) Platform.runLater(() -> sm.setText("⏱  " + fd));
                }
            }

            if (needsRefresh) {
                try {
                    ServiceLocator.getInstance().getAuctionHouse().closeExpiredAuctions();
                } catch (Exception e) {
                    LOG.warning("closeExpiredAuctions lỗi: " + e.getMessage());
                }
                Platform.runLater(this::refreshAll);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void refreshAll() {
        timerLabels.clear();
        loadHotAuctions();
        loadAllAuctions();
        if (onResultsReload != null) onResultsReload.run();
    }

    public void loadHotAuctions() {
        view.hotCardsContainer().getChildren().clear();
        synchronized (displayedItems) { displayedItems.clear(); }
        List<AuctionCardModel> items = cardMapper.loadByStatus(AuctionStatus.ACTIVE);
        items = items.stream()
                .sorted((a, b) -> Integer.compare(b.bidCount(), a.bidCount()))
                .limit(3)
                .toList();
        synchronized (displayedItems) { displayedItems.addAll(items); }
        for (AuctionCardModel item : items) {
            VBox card = cardFactory.buildHotCard(item);
            HBox.setHgrow(card, Priority.ALWAYS);
            view.hotCardsContainer().getChildren().add(card);
        }
    }

    public void loadAllAuctions() {
        view.allProductsGrid().getChildren().clear();
        view.allProductsGrid().getColumnConstraints().clear();
        int columns = 3;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            view.allProductsGrid().getColumnConstraints().add(cc);
        }
        List<AuctionCardModel> items = cardMapper.loadByStatus(AuctionStatus.ACTIVE);
        synchronized (displayedItems) {
            for (AuctionCardModel item : items) {
                boolean exists = displayedItems.stream().anyMatch(c -> c.id().equals(item.id()));
                if (!exists) displayedItems.add(item);
            }
        }
        for (int i = 0; i < items.size(); i++) {
            VBox card = cardFactory.buildSmallCard(items.get(i));
            view.allProductsGrid().add(card, i % columns, i / columns);
        }
        if (view.resultCountLabel() != null) {
            view.resultCountLabel().setText(items.size() + " kết quả");
        }
    }

    public void loadResultAuctions(VBox resultsList, Label resultsSubtitle) {
        resultsList.getChildren().clear();
        List<AuctionCardModel> closed = cardMapper.loadByStatus(AuctionStatus.CLOSED);
        List<AuctionCardModel> expired = cardMapper.loadByStatus(AuctionStatus.EXPIRED);
        List<AuctionCardModel> all = new ArrayList<>();
        all.addAll(closed);
        all.addAll(expired);
        all.sort((a, b) -> {
            if (a.endTime() == null) return 1;
            if (b.endTime() == null) return -1;
            return b.endTime().compareTo(a.endTime());
        });

        if (all.isEmpty()) {
            Label empty = new Label("Chưa có phiên đấu giá nào kết thúc.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 32;");
            resultsList.getChildren().add(empty);
        } else {
            for (AuctionCardModel item : all) {
                String winnerName = null;
                double finalAmount = item.currentBid();
                try {
                    var leadingBid = bidRepo.findLeadingBid(Integer.parseInt(item.id()));
                    if (leadingBid.isPresent()) {
                        winnerName = leadingBid.get().getBuyerUsername();
                        finalAmount = leadingBid.get().getAmount().doubleValue();
                    }
                } catch (Exception ignored) {
                }
                HBox card = cardFactory.buildResultCard(item, winnerName, finalAmount);
                resultsList.getChildren().add(card);
            }
        }
        resultsSubtitle.setText(all.size() + " phiên đấu giá đã kết thúc");
    }

    public void showAllCategories() {
        setActiveChip(view.chipAll());
        activeCategory = null;
        view.searchField().clear();
        applyFilters();
    }

    public void filterByCategory(Button chip) {
        setActiveChip(chip);
        String chipText = chip.getText().trim();
        activeCategory = chipText.replaceAll("^\\S+\\s*", "").trim();
        view.searchField().clear();
        applyFilters();
    }

    public void search() {
        String query = view.searchField().getText().trim().toLowerCase();
        if (query.isEmpty()) {
            applyFilters();
            return;
        }
        List<AuctionCardModel> base = baseForCategory();
        renderFilteredItems(
                base.stream()
                        .filter(item -> item.title().toLowerCase().contains(query)
                                || item.category().toLowerCase().contains(query))
                        .toList()
        );
    }

    public AuctionCardModel findById(String id) {
        synchronized (displayedItems) {
            return displayedItems.stream()
                    .filter(it -> it.id().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }

    public void shutdown() {
        if (timerScheduler != null && !timerScheduler.isShutdown()) {
            timerScheduler.shutdownNow();
        }
    }

    private List<AuctionCardModel> baseForCategory() {
        if (activeCategory == null || activeCategory.isEmpty()) {
            synchronized (displayedItems) {
                return List.copyOf(displayedItems);
            }
        }
        final String cat = activeCategory;
        synchronized (displayedItems) {
            return displayedItems.stream()
                    .filter(item -> item.category() != null
                            && item.category().toLowerCase().contains(cat.toLowerCase()))
                    .toList();
        }
    }

    private void applyFilters() {
        renderFilteredItems(baseForCategory());
    }

    private void renderFilteredItems(List<AuctionCardModel> items) {
        view.allProductsGrid().getChildren().clear();
        view.allProductsGrid().getColumnConstraints().clear();
        int columns = 3;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            view.allProductsGrid().getColumnConstraints().add(cc);
        }
        for (int i = 0; i < items.size(); i++) {
            VBox card = cardFactory.buildSmallCard(items.get(i));
            view.allProductsGrid().add(card, i % columns, i / columns);
        }
        if (items.isEmpty()) {
            Label empty = new Label("Không tìm thấy kết quả nào 🔍");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 14px; -fx-padding: 40 0;");
            view.allProductsGrid().add(empty, 0, 0);
            GridPane.setColumnSpan(empty, columns);
        }
        if (view.resultCountLabel() != null) {
            view.resultCountLabel().setText(items.size() + " kết quả");
        }
    }

    private void setActiveChip(Button newActive) {
        if (activeChipButton != null) {
            activeChipButton.getStyleClass().remove("chip-active");
            if (!activeChipButton.getStyleClass().contains("chip")) {
                activeChipButton.getStyleClass().add("chip");
            }
        }
        newActive.getStyleClass().remove("chip");
        if (!newActive.getStyleClass().contains("chip-active")) {
            newActive.getStyleClass().add("chip-active");
        }
        activeChipButton = newActive;
    }
}
