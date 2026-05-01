module com.nhom9.auction.baitaplon_ltnc_nhom9 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;

    opens com.nhom9.auction.baitaplon_ltnc_nhom9 to javafx.fxml;
    opens com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller to javafx.fxml;
    exports com.nhom9.auction.baitaplon_ltnc_nhom9;
}