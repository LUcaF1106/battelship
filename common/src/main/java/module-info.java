module com.itis._5a.frasson.busanello.common {
    requires static lombok;
    requires com.google.gson;
    exports com.itis._5a.frasson.busanello.common;
    exports com.itis._5a.frasson.busanello.common.Message;
    opens  com.itis._5a.frasson.busanello.common.Message;
}