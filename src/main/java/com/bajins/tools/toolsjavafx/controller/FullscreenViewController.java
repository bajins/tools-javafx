package com.bajins.tools.toolsjavafx.controller;

import com.bajins.tools.toolsjavafx.model.ProjectDevAmountData;
import com.bajins.tools.toolsjavafx.utils.JfxTableFilterUtils;
import com.bajins.tools.toolsjavafx.utils.JfxUtils;
import jakarta.inject.Singleton;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author bajins
 */
@Singleton
public class FullscreenViewController implements Initializable {

    @FXML
    private TextField txtSearch;

    @FXML
    private Button btnCopy;

    @FXML
    private Button btnExport;

    @FXML
    private Label statusLabel;

    @FXML
    private TableView<ProjectDevAmountData> tableView;

    /**
     * 初始化控制器，实现Initializable接口后，initialize()则不再自动调用
     *
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 此时@FXML字段已注入，可安全使用
        initialize();
    }

    @FXML
    public void initialize() {

    }

    public void initData(ObservableList<ProjectDevAmountData> projectDevAmountDataList) {
        // --- 数据绑定和过滤 ---
        FilteredList<ProjectDevAmountData> filteredData = JfxTableFilterUtils.setupSearch(txtSearch, projectDevAmountDataList, tableView);
        filteredData.addListener((javafx.collections.ListChangeListener<ProjectDevAmountData>) c -> {
            while (c.next()) { // Handle adds/removes
                statusLabel.setText("过滤后 " + filteredData.size() + " 条");
            }
        });
        statusLabel.setText("共 " + filteredData.size() + " 条");

        JfxUtils.createColumnsFromAnnotations(tableView, ProjectDevAmountData.class, filteredData);

        // --- 事件绑定 ---
        btnCopy.setOnAction(e -> JfxUtils.copyTableContent(tableView));
        btnExport.setOnAction(e -> JfxUtils.exportTableContent(tableView));
    }
}
