// client/src/main/java/module-info.java
module com.itis._5a.frasson.busanello.client {
    requires com.itis._5a.frasson.busanello.common;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;

    opens com.itis._5a.frasson.busanello.client to javafx.fxml;

    exports com.itis._5a.frasson.busanello.client;

}