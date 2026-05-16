package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;

import javafx.stage.Stage;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Callback điều hướng / coordinator từ {@link com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.HomeController}. */
public record SellerProductsHost(
        Runnable showMyProductsOverlay,
        Runnable showSellerTermsOverlay,
        Runnable showListProductOverlay,
        Runnable showHomeOverlay,
        Runnable selectHomeTab,
        Runnable refreshCatalog,
        Runnable requireLogin,
        Runnable onSessionChanged,
        BiConsumer<AuctionCardModel, Runnable> openSellerItemDetail,
        Supplier<Stage> ownerStage,
        Supplier<javafx.stage.Window> dialogOwner,
        Runnable ensureCoordinators
) {}
