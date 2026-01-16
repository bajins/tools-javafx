package com.bajins.tools.toolsjavafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ListResourceBundle;
import java.util.Objects;

/**
 * @author bajin
 */
public class ToolsjavafxApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ToolsjavafxApplication.class.getResource("excel-diff-db.fxml"));
        // 1. 获取主屏幕的可视区域 (排除任务栏/Dock栏的区域)
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        // 2. 计算目标宽高 (宽60%, 高80%)
        double windowWidth = screenBounds.getWidth() * 0.6;
        double windowHeight = screenBounds.getHeight() * 0.8;
        // 3. 创建 Scene 时传入宽高
        Scene scene = new Scene(fxmlLoader.load(), windowWidth, windowHeight);

        // Scene scene = new Scene(new StackPane(fxmlLoader.load()), 400, 300);

        // 如果希望滚动条轨道一直显示
        // 可以添加这个 CSS。通常有了上面的 Policy，内容超宽时滚动条会自动出现。
        /*
        String css = """
            .table-view .virtual-flow {
                -fx-hbar-policy: always;
            }
        """;
        scene.getStylesheets().add("data:text/css," + css.replaceAll("\n", ""));
        */
        // 引入样式文件
        // scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());

        stage.setTitle("Excel比对数据库工具");
        stage.setScene(scene);
        // 设置窗口居中显示
        stage.setX((screenBounds.getWidth() - windowWidth) / 2);
        stage.setY((screenBounds.getHeight() - windowHeight) / 2);

        System.setProperty("controlsfx.locale", "zh");

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }


    public class ChineseTableFilterResources extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][]{
                    {"filter.apply", "应用"},
                    {"filter.reset", "重置"},
                    {"filter.placeholder", "输入筛选条件..."},
                    // 可根据需要添加更多键值对
            };
        }
    }
}
