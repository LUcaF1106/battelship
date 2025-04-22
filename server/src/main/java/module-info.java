module com.itis._5a.frasson.busanello.server {
    requires com.itis._5a.frasson.busanello.common;
    requires static lombok;
    requires com.google.gson;
    requires password4j;

    opens com.itis._5a.frasson.busanello.server to com.google.gson;

    exports com.itis._5a.frasson.busanello.server;
}