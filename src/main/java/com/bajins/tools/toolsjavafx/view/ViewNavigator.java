package com.bajins.tools.toolsjavafx.view;

import com.bajins.tools.toolsjavafx.utils.AppContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author bajins
 */
public class ViewNavigator {

    private static final String FXML_PATH_PREFIX = "/com/bajins/tools/toolsjavafx/fxml/";
    private static final Map<String, URL> VIEW_CACHE = new HashMap<>();
    private static final Map<String, Object> SHARED_DATA = new ConcurrentHashMap<>();
    private static Stage mainStage;
    private static Stage primaryStage;

    static {
        VIEW_CACHE.put("main", ViewNavigator.class.getResource(FXML_PATH_PREFIX + "main.fxml"));
        VIEW_CACHE.put("header", ViewNavigator.class.getResource(FXML_PATH_PREFIX + "header.fxml"));
        VIEW_CACHE.put("content", ViewNavigator.class.getResource(FXML_PATH_PREFIX + "content.fxml"));
    }

    /**
     * 初始化时设置主 Stage
     * @param stage
     */
    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static FXMLLoader createLoader(String fxmlPath) {
        return new FXMLLoader(ViewNavigator.class.getResource(FXML_PATH_PREFIX + fxmlPath));
    }

    /**
     * 加载并切换视图
     *
     * @param fxmlPath
     */
    public static void loadSceneMaxWindow(String fxmlPath, String title, Window parentWindow, Consumer<FXMLLoader> func) throws IOException {
        FXMLLoader loader = createLoader(fxmlPath);
        loader.setControllerFactory(AppContext::get);

        Parent root = loader.load();

        if (func != null) {
            func.accept(loader);
        }

        // 3. 创建 Stage
        Stage stage = new Stage();
        stage.setTitle(title);

        if (parentWindow != null) {
            stage.initOwner(parentWindow);
            // 设置大小和位置
            stage.setX(parentWindow.getX());
            stage.setY(parentWindow.getY());
            stage.setWidth(parentWindow.getWidth());
            stage.setHeight(parentWindow.getHeight());
        }
        // 4. 设置 Scene 并显示
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


    public Parent load(String fxmlPath) throws IOException {
        URL url = VIEW_CACHE.computeIfAbsent(fxmlPath, k -> getClass().getResource(k));

        FXMLLoader loader = new FXMLLoader(url);
        // 手动设置 Controller，实现依赖注入
        loader.setControllerFactory(this::createController);

        return loader.load();
    }

    private Object createController(Class<?> clazz) {
        // 这里可以接入 Spring/CDI/Guice
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 导航到指定视图
     *
     * @param viewName 视图名称
     */
    public static void navigateTo(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(VIEW_CACHE.get(viewName));
            loader.setControllerFactory(c -> {
                // 自定义Controller工厂，支持依赖注入
                try {
                    Object controller = c.getDeclaredConstructor().newInstance();
                    // 这里可集成DI框架（如Gluon Ignite/FxWeaver）
                    // injectDependencies(controller);
                    return controller;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.sizeToScene();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + viewName, e);
        }
    }

    /**
     * 导航到指定视图并传递参数
     *
     * @param viewName 视图名称
     * @param param    要传递的参数
     */
    public static <T> void navigateTo(String viewName, T param) {
        SHARED_DATA.put("param", param);
        navigateTo(viewName);
        // Controller中通过ViewNavigator.getParam()获取
    }
}
