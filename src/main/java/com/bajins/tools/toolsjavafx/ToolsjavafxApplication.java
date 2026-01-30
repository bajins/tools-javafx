package com.bajins.tools.toolsjavafx;

import com.bajins.tools.toolsjavafx.utils.AppContext;
import com.bajins.tools.toolsjavafx.view.ViewNavigator;
import io.avaje.inject.BeanScope;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
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
    public void init() {
        // 1. 启动 Avaje 容器，扫描并注入所有 @Singleton Bean
        AppContext.init();

        // 1. 初始化容器，扫描指定包
        // DimensionDI.builder().scanPackages("com.example.project").buildAndInit();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // ViewNavigator.setMainStage(primaryStage);
        // ViewNavigator.loadScene("excel-diff-db.fxml");

        AppContext.getScope().all().forEach(System.out::println);

        FXMLLoader fxmlLoader = ViewNavigator.createLoader("excel-diff-db.fxml");
        // 将 Controller 的创建委托给 Avaje
        fxmlLoader.setControllerFactory(AppContext::get);
        // 委托给 Dimension ServiceLocator
        // fxmlLoader.setControllerFactory(type -> ServiceLocator.get(type));

        // 1. 获取主屏幕的可视区域 (排除任务栏/Dock栏的区域)
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        // 2. 计算目标宽高 (宽60%, 高80%)
        double windowWidth = screenBounds.getWidth() * 0.6;
        double windowHeight = screenBounds.getHeight() * 0.8;

        Parent root = fxmlLoader.load();
        // 3. 创建 Scene 时传入宽高
        Scene scene = new Scene(root, windowWidth, windowHeight);

        // Scene scene = new Scene(new StackPane(root), 400, 300);

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

        primaryStage.setTitle("Excel比对数据库工具");
        primaryStage.setScene(scene);
        // 设置窗口居中显示
        primaryStage.setX((screenBounds.getWidth() - windowWidth) / 2);
        primaryStage.setY((screenBounds.getHeight() - windowHeight) / 2);

        System.setProperty("controlsfx.locale", "zh");

        primaryStage.show();
    }

    @Override
    public void stop() {
        // 关闭 Avaje 容器，释放所有 Bean 实例
        AppContext.close();
    }

    public static void main(String[] args) {
        launch();
    }


    public static class ChineseTableFilterResources extends ListResourceBundle {
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
