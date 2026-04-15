module org.example.baitaplon_ltnc_nhom9 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires java.sql;

    opens org.example.baitaplon_ltnc_nhom9 to javafx.fxml;
    exports org.example.baitaplon_ltnc_nhom9;
}