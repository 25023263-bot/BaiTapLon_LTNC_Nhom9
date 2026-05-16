package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.factory;

import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.CurrencyFormatHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.ProductImageHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Tạo card sản phẩm cho trang chủ và màn kết quả.
 */
public final class ProductCardFactory {

    private final Consumer<String> onPlaceBid;
    private final Map<String, Label> timerLabels;

    public ProductCardFactory(Consumer<String> onPlaceBid, Map<String, Label> timerLabels) {
        this.onPlaceBid = onPlaceBid;
        this.timerLabels = timerLabels;
    }

    public VBox buildHotCard(AuctionCardModel item) {
        VBox card = new VBox();
        card.getStyleClass().add("auction-card");
        card.setSpacing(0);

        StackPane imageStack = new StackPane();
        imageStack.getStyleClass().add("card-image-placeholder");
        imageStack.getChildren().add(
                ProductImageHelper.buildNode(item.imageUrl(), item.imagePlaceholderEmoji(), 300, 200));

        HBox topOverlay = new HBox();
        topOverlay.setAlignment(Pos.TOP_CENTER);
        topOverlay.setSpacing(8);
        StackPane.setAlignment(topOverlay, Pos.TOP_CENTER);

        Label categoryBadge = new Label(item.categoryEmoji() + "  " + item.category());
        categoryBadge.getStyleClass().add("badge-category");
        Region badgeSpacer = new Region();
        HBox.setHgrow(badgeSpacer, Priority.ALWAYS);
        Label liveBadge = new Label(item.isLive() ? "● LIVE AUCTION" : "● AUCTION ENDED");
        liveBadge.getStyleClass().add(item.isLive() ? "badge-live" : "badge-ended");
        topOverlay.getChildren().addAll(categoryBadge, badgeSpacer, liveBadge);
        imageStack.getChildren().add(topOverlay);

        HBox timerRow = new HBox();
        timerRow.getStyleClass().add("timer-row");
        timerRow.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(timerRow, Pos.BOTTOM_CENTER);
        Label timerIcon = new Label("⏱");
        Label timerLabel = new Label();
        if (item.isLive() && item.endTime() != null) {
            timerLabel.getStyleClass().add("timer-label-live");
            timerLabel.setText("--:--:--");
            timerLabels.put(item.id(), timerLabel);
        } else {
            timerLabel.getStyleClass().add("timer-label-ended");
            timerLabel.setText("Đã kết thúc");
        }
        timerRow.getChildren().addAll(timerIcon, timerLabel);
        imageStack.getChildren().add(timerRow);

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");
        HBox priceRow = new HBox();
        priceRow.getStyleClass().add("card-price-row");
        VBox priceLeft = new VBox(2);
        Label bidLbl = new Label("Giá hiện tại");
        bidLbl.getStyleClass().add("price-label-small");
        Label priceValue = new Label(CurrencyFormatHelper.formatPrice(item.currentBid()));
        priceValue.getStyleClass().add("price-value");
        priceLeft.getChildren().addAll(bidLbl, priceValue);
        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        VBox bidsRight = new VBox(2);
        bidsRight.setAlignment(Pos.TOP_RIGHT);
        Label bidsLbl = new Label("📈  Lượt đấu");
        bidsLbl.getStyleClass().add("bid-count-label");
        Label bidsVal = new Label(String.valueOf(item.bidCount()));
        bidsVal.getStyleClass().add("bid-count-value");
        bidsRight.getChildren().addAll(bidsLbl, bidsVal);
        priceRow.getChildren().addAll(priceLeft, priceSpacer, bidsRight);

        Label title = new Label(item.title());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        cardBody.getChildren().addAll(title, priceRow);

        VBox cardFooter = new VBox();
        cardFooter.getStyleClass().add("card-footer");
        cardFooter.getChildren().add(buildBidFooter(item));

        card.getChildren().addAll(imageStack, cardBody, cardFooter);
        return card;
    }

    public VBox buildSmallCard(AuctionCardModel item) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card-sm");
        StackPane imgPane = new StackPane();
        imgPane.getStyleClass().add("card-sm-image");
        imgPane.getChildren().add(
                ProductImageHelper.buildNode(item.imageUrl(), item.imagePlaceholderEmoji(), 160, 120));

        VBox body = new VBox(4);
        body.getStyleClass().add("card-sm-body");
        Label categoryLabel = new Label(item.categoryEmoji() + "  " + item.category());
        categoryLabel.getStyleClass().add("card-sm-category");
        Label titleLabel = new Label(item.title());
        titleLabel.getStyleClass().add("card-sm-title");
        titleLabel.setWrapText(true);
        Label priceLabel = new Label(CurrencyFormatHelper.formatPrice(item.currentBid()));
        priceLabel.getStyleClass().add("card-sm-price");
        Label statusLabel = new Label();
        if (item.isLive()) {
            statusLabel.setText("⏱  LIVE");
            statusLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 11px; -fx-font-weight: bold;");
            timerLabels.put(item.id() + "_sm", statusLabel);
        } else {
            statusLabel.setText("Đã kết thúc");
            statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        }
        body.getChildren().addAll(categoryLabel, titleLabel, priceLabel, statusLabel);

        VBox footer = new VBox();
        footer.getStyleClass().add("card-sm-footer");
        footer.getChildren().add(buildBidFooterSmall(item));

        card.getChildren().addAll(imgPane, body, footer);
        return card;
    }

    public HBox buildResultCard(AuctionCardModel item, String winner, double finalAmount) {
        HBox card = new HBox();
        card.getStyleClass().add("result-card");

        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add("result-card-image");
        imagePane.getChildren().add(
                ProductImageHelper.buildNode(item.imageUrl(), item.imagePlaceholderEmoji(), 140, 120));

        VBox body = new VBox(6);
        body.getStyleClass().add("result-card-body");
        HBox.setHgrow(body, Priority.ALWAYS);

        Label categoryLabel = new Label(item.categoryEmoji() + "  " + item.category());
        categoryLabel.getStyleClass().add("result-card-category");
        Label titleLabel = new Label(item.title());
        titleLabel.getStyleClass().add("result-card-title");
        titleLabel.setWrapText(true);

        VBox priceBox = new VBox(2);
        Label priceLbl = new Label("Giá cuối cùng");
        priceLbl.getStyleClass().add("result-final-price-label");
        Label priceVal = new Label(CurrencyFormatHelper.formatPrice(finalAmount));
        priceVal.getStyleClass().add("result-final-price-value");
        priceBox.getChildren().addAll(priceLbl, priceVal);

        Label winnerBadge;
        if (winner != null) {
            winnerBadge = new Label("🏆  Người thắng: " + winner);
            winnerBadge.getStyleClass().add("result-winner-badge");
        } else {
            winnerBadge = new Label("⏰  Hết hạn – không có lượt đấu");
            winnerBadge.getStyleClass().add("result-expired-badge");
        }

        body.getChildren().addAll(categoryLabel, titleLabel, priceBox, winnerBadge);
        card.getChildren().addAll(imagePane, body);
        return card;
    }

    private javafx.scene.Node buildBidFooter(AuctionCardModel item) {
        if (isOwner(item)) {
            // FIX: Trước đây là Label không click được → seller không mở được
            // màn chi tiết từ trang chủ. Đổi thành Button "Xem chi tiết" để
            // gọi onPlaceBid → handlePlaceBid → sellerItemDetailCoordinator.
            Button viewBtn = new Button("🏷  Xem chi tiết");
            viewBtn.getStyleClass().add("btn-bid");
            viewBtn.setMaxWidth(Double.MAX_VALUE);
            viewBtn.setStyle("-fx-opacity: 0.75;"); // phân biệt với nút Đặt giá của buyer
            viewBtn.setOnAction(e -> onPlaceBid.accept(item.id()));
            return viewBtn;
        }
        Button bidBtn = new Button("Đặt giá");
        bidBtn.getStyleClass().add("btn-bid");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> onPlaceBid.accept(item.id()));
        return bidBtn;
    }

    private javafx.scene.Node buildBidFooterSmall(AuctionCardModel item) {
        if (isOwner(item)) {
            // FIX: Tương tự buildBidFooter — đổi Label thành Button.
            Button viewBtn = new Button("🏷  Xem chi tiết");
            viewBtn.getStyleClass().add("btn-bid-sm");
            viewBtn.setMaxWidth(Double.MAX_VALUE);
            viewBtn.setStyle("-fx-opacity: 0.75;");
            viewBtn.setOnAction(e -> onPlaceBid.accept(item.id()));
            return viewBtn;
        }
        Button bidBtn = new Button("Đặt giá");
        bidBtn.getStyleClass().add("btn-bid-sm");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> onPlaceBid.accept(item.id()));
        return bidBtn;
    }

    private static boolean isOwner(AuctionCardModel item) {
        return UserSession.getInstance().isLoggedIn()
                && UserSession.getInstance().getCurrentUserId() == item.sellerId();
    }
}