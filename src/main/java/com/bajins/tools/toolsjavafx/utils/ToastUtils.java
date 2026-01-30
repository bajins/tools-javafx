package com.bajins.tools.toolsjavafx.utils;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;

public class ToastUtils {

    /**
     * ToastUtil.show(btnRun.getScene().getWindow(), "导出成功！已保存至桌面。");
     *
     * @param owner
     * @param message
     */
    public static void show(Window owner, String message) {
        if (owner == null) {
            return;
        }

        final Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true); // 点击其他地方自动消失

        // 1. 构建 UI
        Label label = new Label(message);
        label.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); " +
                "-fx-text-fill: white; " +
                "-fx-padding: 10px 20px; " +
                "-fx-background-radius: 5px; " +
                "-fx-font-size: 14px;");
        label.setWrapText(true);
        label.setMaxWidth(300);
        label.setTextAlignment(TextAlignment.CENTER);

        // 2. 显示位置 (父窗口的中下方)
        popup.getContent().add(label);
        popup.setOnShown(e -> {
            popup.setX(owner.getX() + (owner.getWidth() - popup.getWidth()) / 2);
            popup.setY(owner.getY() + owner.getHeight() * 0.85);
        });

        popup.show(owner);

        // 3. 动画 (显示 2 秒后淡出)
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(popup.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(2.0), new KeyValue(popup.opacityProperty(), 1.0)), // 停留2秒
                new KeyFrame(Duration.seconds(3.0), new KeyValue(popup.opacityProperty(), 0.0))  // 第3秒淡出
        );
        timeline.setOnFinished(e -> popup.hide());
        timeline.play();
    }

    /**
     * ToastUtil.showSuccess(btnRun.getScene().getWindow(), "导出成功！已保存至桌面。");
     *
     * @param owner
     * @param message
     */
    public static void showSuccess(Window owner, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                    .title("操作成功")
                    .text(message)
                    .graphic(null) // 可以放一个图标
                    .hideAfter(Duration.seconds(3)) // 3秒后消失
                    .position(Pos.BOTTOM_RIGHT) // 在此位置弹出
                    .owner(owner) // 绑定父窗口（可选）
                    .showInformation(); // 或者 showConfirm(), showWarning(), show()
        });
    }

    /**
     * 显示信息弹窗
     *
     * @param message 弹窗内容
     */
    public static void alertInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    /**
     * 显示警告弹窗
     *
     * @param message 弹窗内容
     */
    public static void alertWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message).show();
    }

    /**
     * 显示错误弹窗
     *
     * @param header 弹窗标题
     * @param content 弹窗内容
     */
    public static void alertError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 显示通知弹窗（信息）
     *
     * @param title 弹窗标题
     * @param content 弹窗内容
     */
    public static void showNotifyInfo(String title, String content) {
        Notifications.create().title(title).text(content)
                // 位置
                .position(Pos.BOTTOM_RIGHT)
                .showInformation();
    }

    /**
     * 显示通知弹窗（错误）
     *
     * @param title 弹窗标题
     * @param content 弹窗内容
     */
    public static void showNotifyError(String title, String content) {
        Notifications.create().title(title).text(content)
                // 位置
                .position(Pos.BOTTOM_RIGHT)
                .showError();
    }

    /**
     * 显示通知弹窗（警告）
     *
     * @param title 弹窗标题
     * @param content 弹窗内容
     */
    public static void showNotifyWarning(String title, String content) {
        Notifications.create().title(title).text(content)
                // 位置
                .position(Pos.BOTTOM_RIGHT)
                .showWarning();
    }

    /**
     * 显示通知弹窗（确认）
     *
     * @param title 弹窗标题
     * @param content 弹窗内容
     */
    public static void showNotifyConfirm(String title, String content) {
        Notifications.create().title(title).text(content)
                // 位置
                .position(Pos.BOTTOM_RIGHT)
                .showConfirm();
    }

    /**
     * 显示气泡弹窗
     *
     * @param owner 弹窗绑定的父窗口
     * @param message 弹窗内容
     */
    public static void showPopOver(Window owner, String message) {
        // 创建气泡内容
        Label content = new Label(message);
        content.setPadding(new Insets(10));
        showPopOver(owner, content);
    }

    /**
     * 显示气泡弹窗
     *
     * @param owner 弹窗绑定的父窗口
     * @param content 弹窗内容
     */
    public static void showPopOver(Window owner, Node content) {
        PopOver popOver = new PopOver();
        popOver.setContentNode(content);
        // 允许拖拽分离
        popOver.setDetachable(true);
        popOver.show(owner);
    }

    /**
     * 显示异常弹窗
     *
     * @param title 弹窗标题
     * @param header 弹窗头内容
     * @param e 异常对象
     */
    public static void showExceptionDialog(String title, String header, String content, Exception e) {
        ExceptionDialog dialog = new ExceptionDialog(e);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        dialog.showAndWait();
    }

    /**
     * 显示异常弹窗
     *
     * @param title 弹窗标题
     * @param header 弹窗头内容
     * @param e 异常对象
     */
    public static void showExceptionDialog(String title, String header, Exception e) {
        showExceptionDialog(title, header, e.getMessage(), e);
    }

    /**
     * 显示异常弹窗
     *
     * @param title 弹窗标题
     * @param e 异常对象
     */
    public static void showExceptionDialog(String title, Exception e) {
        showExceptionDialog(title, e.getMessage(), e);
    }

    /**
     * 显示进度弹窗
     *
     * @param title 弹窗标题
     * @param content 弹窗内容
     */
    public static void showProgressDialog(String title, String content, Task<Void> task) {
        ProgressDialog dialog = new ProgressDialog(task);
        dialog.setTitle(title);
        dialog.setContentText(content);
        new Thread(task).start();
        dialog.show(); // 任务结束时弹窗会自动关闭（视配置而定）
    }
}
