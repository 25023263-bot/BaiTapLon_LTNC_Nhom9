package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;


import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HomeController — MVC Controller for HomeView.fxml
 *
 * Responsibilities:
 *  - Initialize UI state (search, filter dropdown, category chips)
 *  - Dynamically build "hot" auction cards and inject into hotCardsContainer
 *  - Dynamically build all-auction cards and inject into allProductsGrid
 *  - Handle countdown timers for live auctions
 *  - Handle navigation and user actions
 *
 * Architecture note:
 *  In a full application, data would come from an AuctionService / repository.
 *  For now, we use in-memory mock data so the UI is immediately runnable.
 *  Replace the mock data section with real service calls once the backend is ready.
 */
public class HomeController implements Initializable {

    // ── FXML Injections ─────────────────────────────────────────────────────

    /** Container for the 3 trending/hot auction cards */
    @FXML private HBox hotCardsContainer;

    /** GridPane that holds all auction products (scrollable, below fold) */
    @FXML private GridPane allProductsGrid;

    /** The horizontal scroll container for category chips */
    @FXML private HBox categoryChipsContainer;

    /** Search text field */
    @FXML private TextField searchField;

    /** Shows "N results" next to the search bar */
    @FXML private Label resultCountLabel;

    /** Dropdown for sort/filter options */
    @FXML private ComboBox<String> filterDropdown;

    /** Auth buttons — shown when user is NOT logged in */
    @FXML private Button btnLogin;
    @FXML private Button btnRegister;

    /** "All" chip — kept as reference so we can reset its style on category switch */
    @FXML private Button chipAll;

    // ── State ────────────────────────────────────────────────────────────────

    /** Scheduler for live countdown timers (one thread, polls every second) */
    private ScheduledExecutorService timerScheduler;

    /**
     * Maps auctionId → Label that displays the countdown timer.
     * Needed so the scheduler can update the correct label.
     */
    private final Map<String, Label> timerLabels = new HashMap<>();

    /** The currently selected category chip (for styling toggling) */
    private Button activeChipButton;

    // ════════════════════════════════════════════════════════════════════════
    //  MOCK DATA — Replace this section with real service/repository calls
    // ════════════════════════════════════════════════════════════════════════

    /**
     * AuctionItem is a simple data container (DTO / value object).
     *
     * FUTURE: Replace with a real AuctionDTO coming from AuctionService.
     *
     * Why a record? Java records are concise, immutable data carriers —
     * perfect for display-only data passed from service → controller → view.
     */
    public record AuctionItem(
            String id,
            String title,
            String category,
            String categoryEmoji,
            double currentBid,
            int bidCount,
            boolean isLive,
            LocalDateTime endTime,       // null if already ended
            String imagePlaceholderEmoji // temporary until real images are loaded
    ) {}

    /** Returns mock data for the 3 trending auctions */
    private List<AuctionItem> getMockHotAuctions() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new AuctionItem(
                        "HOT-001",
                        "Patek Philippe Nautilus 5711 – Rare Edition",
                        "Watches", "⌚",
                        195_000.00, 212,
                        false, null,          // ended
                        "🕐"
                ),
                new AuctionItem(
                        "HOT-002",
                        "Pink Diamond 3.5 Carat – GIA Certified",
                        "Jewelry", "💎",
                        124_000.00, 143,
                        false, null,          // ended
                        "💎"
                ),
                new AuctionItem(
                        "HOT-003",
                        "Rolex Submariner 1965 – Limited Edition",
                        "Watches", "⌚",
                        32_500.00, 87,
                        true, now.plusSeconds(13 * 3600 + 26 * 60 + 28), // live countdown
                        "⌚"
                )
        );
    }

    /** Returns mock data for the general product grid */
    private List<AuctionItem> getMockAllAuctions() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new AuctionItem("ALL-001", "Vintage Cartier Tank – 1972",
                        "Watches", "⌚", 18_500.0, 54, true, now.plusHours(2), "🕑"),
                new AuctionItem("ALL-002", "18K Gold Emerald Necklace",
                        "Jewelry", "💚", 8_900.0, 31, true, now.plusHours(5), "💍"),
                new AuctionItem("ALL-003", "Monet Impression – Signed Print",
                        "Art", "🎨", 45_000.0, 98, true, now.plusHours(1), "🖼"),
                new AuctionItem("ALL-004", "Ming Dynasty Vase – Authenticated",
                        "Antiques", "🏺", 62_000.0, 77, false, null, "🏺"),
                new AuctionItem("ALL-005", "Ferrari 250 GTO – 1962 Replica",
                        "Cars", "🚗", 280_000.0, 211, true, now.plusHours(9), "🏎"),
                new AuctionItem("ALL-006", "Rare Penny Black Stamp – 1840",
                        "Collectibles", "🏆", 14_200.0, 42, true, now.plusHours(3), "📮")
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Called automatically by JavaFX after the FXML is loaded.
     * This is the entry point — set everything up here.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Populate the sort/filter dropdown options
        setupFilterDropdown();

        // 2. Track the "All" chip as the initially active chip
        activeChipButton = chipAll;

        // 3. Load and render the 3 trending cards
        loadHotAuctions();

        // 4. Load and render the full product grid
        loadAllAuctions();

        // 5. Start the countdown timer for all live auctions
        startCountdownTimers();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SETUP
    // ════════════════════════════════════════════════════════════════════════

    /** Populate the filter/sort ComboBox with options */
    private void setupFilterDropdown() {
        filterDropdown.getItems().addAll(
                "Ending Soon",
                "Most Bids",
                "Lowest Price",
                "Highest Price",
                "Newest First"
        );
        filterDropdown.setValue("Ending Soon");
        filterDropdown.setOnAction(e -> handleFilterChange());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CARD BUILDERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Loads mock hot auctions and builds one large card per item.
     * Cards are injected into hotCardsContainer (HBox) defined in FXML.
     *
     * FUTURE: Replace getMockHotAuctions() with auctionService.getHotAuctions()
     */
    private void loadHotAuctions() {
        hotCardsContainer.getChildren().clear();

        List<AuctionItem> items = getMockHotAuctions();
        for (AuctionItem item : items) {
            VBox card = buildHotCard(item);

            // Each card shares horizontal space equally
            HBox.setHgrow(card, Priority.ALWAYS);
            hotCardsContainer.getChildren().add(card);
        }
    }

    /**
     * Loads mock products and arranges them in a responsive 3-column grid.
     *
     * FUTURE: Replace getMockAllAuctions() with auctionService.getAllAuctions()
     */
    private void loadAllAuctions() {
        allProductsGrid.getChildren().clear();
        allProductsGrid.getColumnConstraints().clear();

        // 3 equal columns
        int columns = 3;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            allProductsGrid.getColumnConstraints().add(cc);
        }

        List<AuctionItem> items = getMockAllAuctions();
        for (int i = 0; i < items.size(); i++) {
            VBox card = buildSmallCard(items.get(i));
            allProductsGrid.add(card, i % columns, i / columns);
        }

        resultCountLabel.setText(items.size() + " results");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  buildHotCard()  — Builds one large "trending" auction card
    //
    //  Layout:
    //    VBox (card root)
    //    ├── StackPane (image area with overlay badges + timer)
    //    │   ├── image placeholder / ImageView
    //    │   ├── HBox (top badges — category left, LIVE right)
    //    │   └── HBox (bottom timer row)
    //    ├── VBox (card body — title, price row)
    //    └── VBox (card footer — Place Bid button)
    // ─────────────────────────────────────────────────────────────────────────
    private VBox buildHotCard(AuctionItem item) {

        // ── Root card container ──────────────────────────────────────────────
        VBox card = new VBox();
        card.getStyleClass().add("auction-card");
        card.setSpacing(0);

        // ── Image area (StackPane lets badges overlay the image) ─────────────
        StackPane imageStack = new StackPane();
        imageStack.getStyleClass().add("card-image-placeholder");

        //  Image placeholder (emoji centered in a dark box)
        //  FUTURE: replace with ImageView + image URL from auction data
        Label imgIcon = new Label(item.imagePlaceholderEmoji());
        imgIcon.getStyleClass().add("card-image-icon");
        imageStack.getChildren().add(imgIcon);

        //  Top overlay: [Category badge]   [LIVE / AUCTION ENDED badge]
        HBox topOverlay = new HBox();
        topOverlay.setAlignment(Pos.TOP_CENTER);
        topOverlay.setPadding(new Insets(12));
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

        //  Bottom overlay: timer row
        HBox timerRow = new HBox();
        timerRow.getStyleClass().add("timer-row");
        timerRow.setAlignment(Pos.CENTER_LEFT);
        timerRow.setSpacing(6);
        StackPane.setAlignment(timerRow, Pos.BOTTOM_CENTER);

        Label timerIcon = new Label("⏱");
        timerIcon.getStyleClass().add("timer-icon");

        Label timerLabel = new Label();
        if (item.isLive() && item.endTime() != null) {
            timerLabel.getStyleClass().add("timer-label-live");
            timerLabel.setText("--:--:--");
            // Register this label so the scheduler can update it every second
            timerLabels.put(item.id(), timerLabel);
        } else {
            timerLabel.getStyleClass().add("timer-label-ended");
            timerLabel.setText("Auction Ended");
        }

        timerRow.getChildren().addAll(timerIcon, timerLabel);
        imageStack.getChildren().add(timerRow);

        // ── Card body (title + price) ────────────────────────────────────────
        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");
        cardBody.setSpacing(4);

        Label title = new Label(item.title());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        // Price row: [Current Bid label + value]  spacer  [Bids label + count]
        HBox priceRow = new HBox();
        priceRow.getStyleClass().add("card-price-row");
        priceRow.setAlignment(Pos.BOTTOM_LEFT);

        VBox priceLeft = new VBox(2);
        Label bidLabel = new Label("Current Bid");
        bidLabel.getStyleClass().add("price-label-small");
        Label priceValue = new Label(formatPrice(item.currentBid()));
        priceValue.getStyleClass().add("price-value");
        priceLeft.getChildren().addAll(bidLabel, priceValue);

        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);

        VBox bidsRight = new VBox(2);
        bidsRight.setAlignment(Pos.TOP_RIGHT);
        Label bidsLabel = new Label("📈  Bids");
        bidsLabel.getStyleClass().add("bid-count-label");
        Label bidsValue = new Label(String.valueOf(item.bidCount()));
        bidsValue.getStyleClass().add("bid-count-value");
        bidsRight.getChildren().addAll(bidsLabel, bidsValue);

        priceRow.getChildren().addAll(priceLeft, priceSpacer, bidsRight);
        cardBody.getChildren().addAll(title, priceRow);

        // ── Card footer (Place Bid button) ───────────────────────────────────
        VBox cardFooter = new VBox();
        cardFooter.getStyleClass().add("card-footer");

        Button bidBtn = new Button("Place Bid");
        bidBtn.getStyleClass().add("btn-bid");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> handlePlaceBid(item.id()));

        cardFooter.getChildren().add(bidBtn);

        // ── Assemble card ────────────────────────────────────────────────────
        card.getChildren().addAll(imageStack, cardBody, cardFooter);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  buildSmallCard()  — Builds a compact card for the all-auctions grid
    // ─────────────────────────────────────────────────────────────────────────
    private VBox buildSmallCard(AuctionItem item) {

        VBox card = new VBox();
        card.getStyleClass().add("product-card-sm");
        card.setSpacing(0);

        // Image placeholder
        StackPane imgPane = new StackPane();
        imgPane.getStyleClass().add("card-sm-image");
        Label imgLabel = new Label(item.imagePlaceholderEmoji());
        imgLabel.setStyle("-fx-font-size: 36px; -fx-text-fill: #333333;");
        imgPane.getChildren().add(imgLabel);

        // Body
        VBox body = new VBox(4);
        body.getStyleClass().add("card-sm-body");

        Label categoryLabel = new Label(item.categoryEmoji() + "  " + item.category());
        categoryLabel.getStyleClass().add("card-sm-category");

        Label titleLabel = new Label(item.title());
        titleLabel.getStyleClass().add("card-sm-title");
        titleLabel.setWrapText(true);

        Label priceLabel = new Label(formatPrice(item.currentBid()));
        priceLabel.getStyleClass().add("card-sm-price");

        // Status label (live countdown or ended)
        Label statusLabel = new Label();
        if (item.isLive()) {
            statusLabel.setText("⏱  LIVE");
            statusLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 11px; -fx-font-weight: bold;");
            timerLabels.put(item.id() + "_sm", statusLabel);
        } else {
            statusLabel.setText("Ended");
            statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        }

        body.getChildren().addAll(categoryLabel, titleLabel, priceLabel, statusLabel);

        // Footer button
        VBox footer = new VBox();
        footer.getStyleClass().add("card-sm-footer");

        Button bidBtn = new Button("Place Bid");
        bidBtn.getStyleClass().add("btn-bid-sm");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> handlePlaceBid(item.id()));

        footer.getChildren().add(bidBtn);

        card.getChildren().addAll(imgPane, body, footer);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COUNTDOWN TIMERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Starts a background thread that ticks every second and updates all
     * live countdown timer labels via Platform.runLater() (JavaFX thread-safe).
     *
     * Why Platform.runLater()?
     *   JavaFX UI can only be updated from the JavaFX Application Thread.
     *   Our scheduler runs on a background thread, so we must "post" updates
     *   back to the UI thread using Platform.runLater().
     */
    private void startCountdownTimers() {
        timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ubid-timer");
            t.setDaemon(true); // dies when app closes — no need to shut it down manually
            return t;
        });

        timerScheduler.scheduleAtFixedRate(() -> {
            // This block runs on a background thread every second.
            // We collect all auction items to update timers for.
            List<AuctionItem> allItems = new ArrayList<>(getMockHotAuctions());
            allItems.addAll(getMockAllAuctions());

            for (AuctionItem item : allItems) {
                if (!item.isLive() || item.endTime() == null) continue;

                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(), item.endTime()).getSeconds();

                String display;
                if (secondsLeft <= 0) {
                    display = "Auction Ended";
                } else {
                    long h = secondsLeft / 3600;
                    long m = (secondsLeft % 3600) / 60;
                    long s = secondsLeft % 60;
                    display = String.format("%02d:%02d:%02d", h, m, s);
                }

                final String finalDisplay = display;

                // Update hot card timer
                Label hotLabel = timerLabels.get(item.id());
                if (hotLabel != null) {
                    Platform.runLater(() -> hotLabel.setText(finalDisplay));
                }

                // Update small card status
                Label smLabel = timerLabels.get(item.id() + "_sm");
                if (smLabel != null) {
                    Platform.runLater(() -> smLabel.setText("⏱  " + finalDisplay));
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FXML EVENT HANDLERS
    // ════════════════════════════════════════════════════════════════════════

    /** Called when user clicks "Home" in the navbar */
    @FXML
    private void handleNavHome() {
        // Already on home — could scroll to top
        System.out.println("[Nav] Home clicked");
    }

    /** Called when user clicks "Categories" in the navbar */
    @FXML
    private void handleNavCategories() {
        // TODO: navigate to CategoriesView.fxml
        System.out.println("[Nav] Categories clicked");
    }

    /** Called when user clicks "Results" in the navbar */
    @FXML
    private void handleNavResults() {
        // TODO: navigate to ResultsView.fxml
        System.out.println("[Nav] Results clicked");
    }

    /** Called when user clicks the search icon button in the navbar */
    @FXML
    private void handleSearch() {
        searchField.requestFocus();
    }

    /** Called when user presses Enter in the search field */
    @FXML
    private void handleSearchQuery() {
        String query = searchField.getText().trim();
        if (!query.isEmpty()) {
            System.out.println("[Search] Query: " + query);
            // TODO: call auctionService.search(query) and refresh grid
        }
    }

    /** Called when user clicks the notification bell */
    @FXML
    private void handleNotifications() {
        // TODO: open notifications panel/popup
        System.out.println("[Notifications] Bell clicked");
    }

    /** Called when user clicks "Sign In" */
    @FXML
    private void handleLogin() {
        // TODO: load LoginView.fxml into scene / open modal
        System.out.println("[Auth] Sign In clicked");
    }

    /** Called when user clicks "Sell on UBid" */
    @FXML
    private void handleRegister() {
        // TODO: load RegisterView.fxml into scene / open modal
        System.out.println("[Auth] Register as Seller clicked");
    }

    /** Called when user clicks the "All" category chip */
    @FXML
    private void handleCategoryAll() {
        setActiveChip(chipAll);
        loadAllAuctions(); // reset to full list
    }

    /**
     * Generic handler for non-"All" category chip clicks.
     * Each chip button's onAction is bound to this.
     * We read the button's text to determine which category was selected.
     *
     * Note: In a more advanced setup, each chip would store the category
     * as user data: chip.setUserData(Category.WATCHES) and we'd read it here.
     */
    @FXML
    private void handleCategoryFilter() {
        // The event source is the clicked chip button
        // For now just log — TODO: filter allProductsGrid by category
        System.out.println("[Category] Filter chip clicked");
    }

    /** Called when user changes the sort dropdown */
    private void handleFilterChange() {
        String selected = filterDropdown.getValue();
        System.out.println("[Filter] Sort by: " + selected);
        // TODO: re-sort the auction list and refresh the grid
    }

    /**
     * Called when "Place Bid" is clicked on any card.
     *
     * @param auctionId The unique ID of the auction item
     */
    private void handlePlaceBid(String auctionId) {
        System.out.println("[Bid] Place Bid clicked for auction: " + auctionId);
        // TODO: check if user is logged in
        //       if yes → open BidDialog / navigate to AuctionDetailView
        //       if no  → prompt to sign in first
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Updates which category chip appears "active" (gold highlight).
     * Toggles CSS styleClass between "chip-active" and "chip".
     */
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

    /**
     * Formats a price value as a USD string.
     * Example: 195000.0 → "$195,000"
     *
     * FUTURE: Use a proper NumberFormat / currency locale from user settings.
     */
    private String formatPrice(double price) {
        if (price >= 1_000_000) {
            return String.format("$%.2fM", price / 1_000_000);
        }
        return String.format("$%,.0f", price);
    }

    /**
     * Called by the JavaFX lifecycle when the scene is being torn down.
     * IMPORTANT: Always shut down background schedulers to prevent memory leaks.
     */
    public void shutdown() {
        if (timerScheduler != null && !timerScheduler.isShutdown()) {
            timerScheduler.shutdownNow();
        }
    }
}