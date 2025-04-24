module com.itis._5a.frasson.busanello.server {
    requires com.itis._5a.frasson.busanello.common;
    requires static lombok;
    requires com.google.gson;
    requires password4j;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;

    opens com.itis._5a.frasson.busanello.server to com.google.gson;

    exports com.itis._5a.frasson.busanello.server;
}