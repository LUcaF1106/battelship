module com.itis._5a.frasson.busanello {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    exports com.itis._5a.frasson.busanello;

    opens com.itis._5a.frasson.busanello to javafx.fxml;
}