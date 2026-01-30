module com.bajins.tools.toolsjavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.gemsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires cn.hutool;
    requires java.sql;
    requires org.apache.commons.lang3;
    requires io.avaje.inject;
    requires javax.inject;

    opens com.bajins.tools.toolsjavafx to javafx.fxml, org.controlsfx.controls;
    exports com.bajins.tools.toolsjavafx;
    exports com.bajins.tools.toolsjavafx.utils;
    opens com.bajins.tools.toolsjavafx.utils to javafx.fxml;
    exports com.bajins.tools.toolsjavafx.model;
    opens com.bajins.tools.toolsjavafx.model to javafx.fxml;
    exports com.bajins.tools.toolsjavafx.controller;
    opens com.bajins.tools.toolsjavafx.controller to javafx.fxml;
    exports com.bajins.tools.toolsjavafx.service;
    opens com.bajins.tools.toolsjavafx.service to javafx.fxml;
    // 必须提供生成的模块类
    provides io.avaje.inject.spi.InjectExtension with com.bajins.tools.toolsjavafx.ToolsjavafxModule;  // 编译后生成
}