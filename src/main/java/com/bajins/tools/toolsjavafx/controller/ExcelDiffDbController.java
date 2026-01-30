package com.bajins.tools.toolsjavafx.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import com.bajins.tools.toolsjavafx.ToolsjavafxApplication;
import com.bajins.tools.toolsjavafx.model.ProjectDevAmountData;
import com.bajins.tools.toolsjavafx.model.RawData;
import com.bajins.tools.toolsjavafx.model.UserAmountData;
import com.bajins.tools.toolsjavafx.service.MainService;
import com.bajins.tools.toolsjavafx.utils.*;
import com.bajins.tools.toolsjavafx.view.ViewNavigator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

/**
 * @author bajin
 */
@Singleton
public class ExcelDiffDbController implements Initializable {
    // UI 组件
    @FXML
    private TextField filePathField;
    // 按钮
    @FXML
    private Button btnPreview;
    @FXML
    private Label lblSourceStatus;

    @FXML
    private ComboBox<String> dbTypeCombo;
    @FXML
    private TextField dbIpField, dbPortField, dbNameField, dbUserField;
    @FXML
    private PasswordField dbPasswordField;

    @FXML
    private TextField txtMainSearch;
    @FXML
    private Button btnTestConn, btnRun, btnCopy, btnExport, btnUtAmount, btnFullscreen, btnWorkHourCount;
    @FXML
    private Label statusLabel;
    // 主界面表格
    @FXML
    private TableView<ProjectDevAmountData> mainTableView;

    // 数据源
    // 1. 用于暂存导入的原始数据，不直接显示在主界面
    private List<RawData> cachedData = new ArrayList<>();

    // 2. 主界面表格绑定的数据列表 (仅在运行时填充)
    private final ObservableList<ProjectDevAmountData> mainTableList = FXCollections.observableArrayList();

    private final MainService mainService;

    private final FullscreenViewController fullscreenViewController;

    @Inject
    public ExcelDiffDbController(MainService mainService, FullscreenViewController fullscreenViewController) {
        this.mainService = mainService;
        this.fullscreenViewController = fullscreenViewController;
    }

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
        // 初始化下拉框
        dbTypeCombo.getItems().addAll("PostgreSQL", "MySQL", "Oracle", "SQLServer");
        dbTypeCombo.getSelectionModel().select("PostgreSQL");

        // 限制端口输入框
        JfxUtils.setupPortInputRestriction(dbPortField);
        // 设置数据库输入框点击自动全选
        JfxUtils.setupAutoSelectOnFocus(
                dbIpField,
                dbPortField,
                dbNameField,
                dbUserField,
                dbPasswordField
        );

        // 绑定搜索逻辑
        FilteredList<ProjectDevAmountData> filteredData = JfxTableFilterUtils.setupSearch(txtMainSearch, mainTableList, mainTableView);
        filteredData.addListener((ListChangeListener<ProjectDevAmountData>) c -> {
            while (c.next()) { // Handle adds/removes
                statusLabel.setText("过滤后 " + filteredData.size() + " 条");
            }
        });
        // 1. 配置列属性 (表头、宽度、样式 全部由实体类注解决定)
        JfxUtils.createColumnsFromAnnotations(mainTableView, ProjectDevAmountData.class, filteredData);
    }

    /**
     * 导入 Excel
     */
    @FXML
    public void handleSelectFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            filePathField.setText(file.getName());
            loadDataToMemory(file, null);
        }
    }

    /**
     * 粘贴数据 (弹窗输入)
     */
    @FXML
    public void handlePasteData() {
        // 获取父窗口 (Main Stage)
        // 注意：必须确保此时 filePathField 已经加载到场景中（在按钮点击事件中是肯定的）
        javafx.stage.Window parentWindow = filePathField.getScene().getWindow();

        // 创建一个包含 TextArea 的对话框
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("粘贴 Excel 数据");
        dialog.setHeaderText("请将 Excel 中的数据复制并粘贴到下方 (自动清空旧数据)");

        // 设置 Owner (关键：确立父子关系，使模态和居中生效)
        if (parentWindow != null) {
            dialog.initOwner(parentWindow);
        }
        dialog.setResizable(true); // 建议允许手动调整大小

        // 构建内容区域
        TextArea textArea = new TextArea();
        textArea.setPromptText("在此处 Ctrl+V 粘贴...");
        textArea.setWrapText(false); // Excel 数据通常不换行
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane pane = new GridPane();
        pane.add(textArea, 0, 0);

        // 设置动态大小
        if (parentWindow != null) {
            double pWidth = parentWindow.getWidth();
            double pHeight = parentWindow.getHeight();

            // 设置内容面板的大小为父窗口的 80% * 80%
            // DialogPane 会自动适配内容的大小
            pane.setPrefSize(pWidth * 0.8, pHeight * 0.8);
        } else {
            // 兜底默认大小
            pane.setPrefSize(600, 400);
        }

        dialog.getDialogPane().setContent(pane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 打开时直接聚焦到文本框，方便直接粘贴
        Platform.runLater(textArea::requestFocus);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? textArea.getText() : null);

        dialog.showAndWait().ifPresent(text -> {
            if (StrUtil.isBlank(text)) {
                return;
            }
            filePathField.setText("粘贴的数据");
            loadDataToMemory(null, text);
        });
    }

    /**
     * 加载数据到内存 (不渲染主界面)
     *
     * @param file
     * @param pasteText
     */
    private void loadDataToMemory(File file, String pasteText) {
        // 清空状态
        cachedData.clear();
        mainTableList.clear(); // 清空主界面表格

        // 禁用按钮
        btnPreview.setDisable(true);
        btnRun.setDisable(true);
        btnCopy.setDisable(true);
        btnExport.setDisable(true);
        btnUtAmount.setDisable(true);
        btnFullscreen.setDisable(true);
        btnWorkHourCount.setDisable(true);
        lblSourceStatus.setText("正在解析...");
        lblSourceStatus.setTextFill(Color.ORANGE);
        try {
            List<RawData> tempList = new ArrayList<>();

            if (file != null) {
                // 读取 Excel
                try (FileInputStream fis = new FileInputStream(file);
                     Workbook wb = WorkbookFactory.create(fis)) {
                    Sheet sheet = wb.getSheetAt(0);
                    for (Row row : sheet) {
                        if (row.getRowNum() == 0) {
                            continue;
                        }
                        String c1 = getCellVal(row.getCell(0));
                        String c2 = getCellVal(row.getCell(1));
                        tempList.add(new RawData(c1, c2));
                    }
                }
            } else if (pasteText != null) {
                // 解析文本
                List<String> lines = StrUtil.split(pasteText, '\n');
                for (String line : lines) {
                    if (StrUtil.isBlank(line)) {
                        continue;
                    }
                    List<String> cols = StrUtil.split(line, '\t');
                    String c1 = CollUtil.get(cols, 0);
                    String c2 = CollUtil.get(cols, 1);
                    tempList.add(new RawData(c1, c2));
                }
            }

            // 数据存入缓存
            this.cachedData = tempList;

            // 更新 UI 状态
            lblSourceStatus.setText("已加载 " + tempList.size() + " 条数据");
            lblSourceStatus.setTextFill(Color.GREEN);
            btnPreview.setDisable(false);
            btnRun.setDisable(false);

            // 清空主搜索框
            txtMainSearch.clear();

        } catch (Exception e) {
            lblSourceStatus.setText("加载失败");
            lblSourceStatus.setTextFill(Color.RED);
            ToastUtils.alertError("加载失败", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 预览数据 (独立窗口)
     */
    @FXML
    public void handlePreviewData() {
        if (cachedData.isEmpty()) {
            return;
        }
        // 获取父窗口 (Main Stage)
        // 注意：必须确保此时 filePathField 已经加载到场景中（在按钮点击事件中是肯定的）
        javafx.stage.Window parentWindow = filePathField.getScene().getWindow();

        // 创建新窗口
        Stage previewStage = new Stage();
        previewStage.setTitle("数据预览 (共 " + cachedData.size() + " 条)");
        // 允许同时操作主界面，如果是 APPLICATION_MODAL 则必须关闭预览才能操作主界面
        previewStage.initModality(Modality.NONE);

        // 设置 Owner (关键：确立父子关系，使模态和居中生效)
        if (parentWindow != null) {
            previewStage.initOwner(parentWindow);
        }
        previewStage.setResizable(true); // 建议允许手动调整大小

        // 2.1 搜索区域
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(5));
        Label lblSearch = new Label("🔍 过滤:");
        TextField txtPreviewSearch = new TextField();
        txtPreviewSearch.setPromptText("输入关键词...");
        txtPreviewSearch.setPrefWidth(200);
        searchBox.getChildren().addAll(lblSearch, txtPreviewSearch);

        // 2.2 创建临时 TableView
        TableView<RawData> previewTable = new TableView<>();
        // 2.3 配置预览搜索逻辑
        // 注意：这里使用cachedData创建新的ObservableList
        ObservableList<RawData> previewList = FXCollections.observableArrayList(cachedData);
        FilteredList<RawData> filteredData = JfxTableFilterUtils.setupSearch(txtPreviewSearch, previewList, previewTable);

        JfxUtils.createColumnsFromAnnotations(previewTable, RawData.class, filteredData);
        // 列宽自适应
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // 2.4 布局
        VBox root = new VBox(5, searchBox, previewTable);
        root.setPadding(new Insets(10));
        VBox.setVgrow(previewTable, Priority.ALWAYS); // 自适应高度

        double targetWidth = 800; // 默认兜底宽度
        double targetHeight = 600; // 默认兜底高度

        if (parentWindow != null) {
            targetWidth = parentWindow.getWidth() * 0.8;
            targetHeight = parentWindow.getHeight() * 0.8;
        }
        Scene scene = new Scene(root, targetWidth, targetHeight);
        previewStage.setScene(scene);

        // 将新窗口居中显示在父窗口之上
        if (parentWindow != null) {
            // 需要在 show 之前设置位置，或者利用 CenterOnScreen
            // 这里手动计算相对居中坐标
            previewStage.setX(parentWindow.getX() + (parentWindow.getWidth() - targetWidth) / 2);
            previewStage.setY(parentWindow.getY() + (parentWindow.getHeight() - targetHeight) / 2);
        }
        previewStage.show();
    }

    /**
     * 测试数据库连接
     */
    @FXML
    public void handleTestConn(ActionEvent actionEvent) {

        // 获取 DB 配置
        String dbType = dbTypeCombo.getValue();
        String ip = dbIpField.getText();
        String port = dbPortField.getText();
        String dbName = dbNameField.getText();
        String user = dbUserField.getText();
        String pass = dbPasswordField.getText();

        // 简单校验
        if (StrUtil.hasBlank(dbType, ip, port, dbName)) {
            ToastUtils.alertError("配置错误", "请完善数据库连接信息");
            return;
        }
        try {
            JdbcUtil.createDataSource(dbType, ip, port, dbName, user, pass);
            JdbcUtil.testConn();

            btnRun.setDisable(false);
            btnWorkHourCount.setDisable(false);

            ToastUtils.alertInfo("数据库连接测试成功");
        } catch (SQLException e) {
            ToastUtils.alertError("数据库连接测试失败", e.getMessage());
        }
    }

    /**
     * 运行
     */
    @FXML
    public void handleRun() {
        if (cachedData.isEmpty()) {
            ToastUtils.alertError("提示", "无数据，请先导入/粘贴");
            return;
        }

        btnRun.setDisable(true);
        statusLabel.setText("正在连接数据库查询...");

        // 后台任务
        Task<Void> task = new Task<>() {
            // 用于收集结果的临时列表 (MainData)
            final List<ProjectDevAmountData> resultList = new ArrayList<>();

            @Override
            protected Void call() throws Exception {
                // 数据转换方便后续快速获取
                Map<String, String> paMap = new HashMap<>();
                StringJoiner stringJoiner = new StringJoiner("','", "('", "')");
                stringJoiner.setEmptyValue("");
                for (RawData rawData : cachedData) {
                    paMap.put(rawData.getPmProjectCode(), rawData.getPrjAmount());
                    stringJoiner.add(rawData.getPmProjectCode());
                }
                List<Entity> entities = mainService.queryProjectDetail("", null, stringJoiner.toString(), null);
                if (entities.isEmpty()) {
                    updateMessage("查询到 0 条数据");
                    return null;
                }
                updateMessage("正在处理 " + entities.size() + " 条数据...");

                for (Entity entity : entities) {

                    String userCode = entity.getStr("user_code");
                    String userName = entity.getStr("user_name");
                    String devRegion = entity.getStr("dev_region");
                    String deptCode = entity.getStr("dept_code");
                    String deptName = entity.getStr("dept_name");
                    String parentDeptCode = entity.getStr("parent_dept_code");
                    String parentDeptName = entity.getStr("parent_dept_name");
                    String pmProjectCode = entity.getStr("pm_project_code");
                    String projectName = entity.getStr("project_name");
                    String pmRegion = entity.getStr("pm_region");
                    String leadUserCode = entity.getStr("lead_user_code");
                    String leadUserName = entity.getStr("lead_user_name");
                    String leadDevRegion = entity.getStr("lead_dev_region");
                    String leadDeptCode = entity.getStr("lead_dept_code");
                    String leadDeptName = entity.getStr("lead_dept_name");
                    String leadParentDeptCode = entity.getStr("lead_parent_dept_code");
                    String leadParentDeptName = entity.getStr("lead_parent_dept_name");
                    BigDecimal pmDevHours = entity.getBigDecimal("pm_dev_hours");
                    BigDecimal pmPrjHours = entity.getBigDecimal("pm_prj_hours");

                    String prjAmount = paMap.get(pmProjectCode);
                    String ujAmount = "";

                    if (prjAmount != null) {
                        // 工时占比 = 该人在该项目的工时 / 该项目的总工时
                        BigDecimal whp = pmDevHours.divide(pmPrjHours, 6, RoundingMode.HALF_UP);
                        // 个人金额 = 该项目总金额 × 工时占比
                        ujAmount = new BigDecimal(prjAmount).multiply(whp).setScale(2, RoundingMode.HALF_UP).toPlainString();
                    }
                    ProjectDevAmountData projectDevAmountData = new ProjectDevAmountData();
                    projectDevAmountData.userCodeProperty().set(userCode);
                    projectDevAmountData.userNameProperty().set(userName);
                    projectDevAmountData.devRegionProperty().set(devRegion);
                    projectDevAmountData.deptCodeProperty().set(deptCode);
                    projectDevAmountData.deptNameProperty().set(deptName);
                    projectDevAmountData.parentDeptCodeProperty().set(parentDeptCode);
                    projectDevAmountData.parentDeptNameProperty().set(parentDeptName);
                    projectDevAmountData.pmProjectCodeProperty().set(pmProjectCode);
                    projectDevAmountData.projectNameProperty().set(projectName);
                    projectDevAmountData.pmRegionProperty().set(pmRegion);
                    projectDevAmountData.leadUserCodeProperty().set(leadUserCode);
                    projectDevAmountData.leadUserNameProperty().set(leadUserName);
                    projectDevAmountData.leadDevRegionProperty().set(leadDevRegion);
                    projectDevAmountData.leadDeptCodeProperty().set(leadDeptCode);
                    projectDevAmountData.leadDeptNameProperty().set(leadDeptName);
                    projectDevAmountData.leadParentDeptCodeProperty().set(leadParentDeptCode);
                    projectDevAmountData.leadParentDeptNameProperty().set(leadParentDeptName);
                    projectDevAmountData.pmDevHoursProperty().set(pmDevHours.stripTrailingZeros().toPlainString());
                    projectDevAmountData.pmPrjHoursProperty().set(pmPrjHours.stripTrailingZeros().toPlainString());
                    projectDevAmountData.prjAmountProperty().set(prjAmount);
                    projectDevAmountData.ujAmountProperty().set(ujAmount);
                    resultList.add(projectDevAmountData);

                    // Platform.runLater(() -> raw.setCompareResult(statusStr));
                }
                updateMessage("执行完成: " + resultList.size() + " 条");
                return null;
            }

            @Override
            protected void succeeded() {
                // 3. 核心：运行成功后，才将数据刷入主界面表格ObservableList
                // FilteredList 会自动感知这个 setAll 操作并应用当前的过滤规则
                mainTableList.setAll(resultList);

                btnRun.setDisable(false);
                btnCopy.setDisable(false);
                btnExport.setDisable(false);
                btnUtAmount.setDisable(false);
                btnFullscreen.setDisable(false);
                btnWorkHourCount.setDisable(false);
                statusLabel.setText(getMessage());
                // ToastUtils.alertInfo("运行完成！");
            }

            @Override
            protected void failed() {
                btnRun.setDisable(false);
                Throwable e = getException();
                ToastUtils.alertError("运行失败", "数据库连接或查询错误:\n" + e.getMessage());
                statusLabel.setText("运行出错");
                e.printStackTrace();
            }
        };
        // 将 Task 的 message 属性直接绑定到 Label
        // statusLabel.textProperty().bind(task.messageProperty());
        // 监听状态成功
        /*task.setOnSucceeded(e -> {
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            // 这里也可以调用上面方案一或方案二的弹窗
            ToastUtil.show(btnRun.getScene().getWindow(), "任务全部完成！");
        });*/
        new Thread(task).start();
    }

    /**
     * 复制结果
     */
    @FXML
    public void handleCopy() {
        JfxUtils.copyTableContent(mainTableView);
    }

    /**
     * 导出结果
     */
    @FXML
    public void handleExport() {
        if (mainTableList.isEmpty()) {
            return;
        }
        JfxUtils.exportTableContent(mainTableView);
    }

    /**
     * 个人结案金额
     */
    @FXML
    public void handleUtAmount() {
        if (mainTableList.isEmpty()) {
            ToastUtils.alertWarning("当前没有数据，请先运行比对");
            return;
        }

        // 根据用户分组统计值
        Map<String, UserAmountData> sumOnlyMap = new HashMap<>();
        HashSet<String> prjQtySet = new HashSet<>();
        for (ProjectDevAmountData projectDevAmountData : mainTableList) {
            String userCode = projectDevAmountData.getUserCode();

            if (StrUtil.isBlank(projectDevAmountData.getPrjAmount())) {
                continue;
            }
            UserAmountData sumOnly = sumOnlyMap.get(userCode);
            if (sumOnly == null) {
                sumOnly = new UserAmountData();
                sumOnly.userCodeProperty().set(userCode);
                sumOnly.userNameProperty().set(projectDevAmountData.getUserName());
                sumOnly.devRegionProperty().set(projectDevAmountData.getDevRegion());
                sumOnly.deptCodeProperty().set(projectDevAmountData.getDeptCode());
                sumOnly.deptNameProperty().set(projectDevAmountData.getDeptName());
                sumOnly.parentDeptCodeProperty().set(projectDevAmountData.getParentDeptCode());
                sumOnly.parentDeptNameProperty().set(projectDevAmountData.getParentDeptName());
                sumOnly.leadPrjQtyProperty().set("0");
                sumOnly.asstPrjQtyProperty().set("0");
                sumOnly.totalPrjQtyProperty().set("0");
                sumOnly.amountProperty().set("0");

                sumOnlyMap.put(userCode, sumOnly);
            }

            String key = userCode + projectDevAmountData.getPmProjectCode();
            if (!prjQtySet.contains(key)) {
                if (userCode.equals(projectDevAmountData.getLeadUserCode())) {
                    // 主担项目
                    sumOnly.leadPrjQtyProperty().set(Integer.toString(Integer.parseInt(sumOnly.getLeadPrjQty()) + 1));
                } else {
                    // 协从项目
                    sumOnly.asstPrjQtyProperty().set(Integer.toString(Integer.parseInt(sumOnly.getAsstPrjQty()) + 1));
                }
                prjQtySet.add(key);

                sumOnly.totalPrjQtyProperty().set(Integer.toString(Integer.parseInt(sumOnly.getLeadPrjQty()) + Integer.parseInt(sumOnly.getAsstPrjQty())));
                BigDecimal ujAmount = new BigDecimal(projectDevAmountData.getUjAmount());
                sumOnly.amountProperty().set(new BigDecimal(sumOnly.getAmount()).add(ujAmount).toPlainString());
            }
        }
        if (sumOnlyMap.isEmpty()) {
            ToastUtils.alertInfo("没有找到符合条件的数据");
            return;
        }
        // 对过滤的数据进行包装排序
        ObservableList<UserAmountData> finishedData = FXCollections.observableArrayList(sumOnlyMap.values());
        // 降序排序（从大到小）
        finishedData.sort((o1, o2) -> {
            String v1 = o1.getAmount();
            String v2 = o2.getAmount();

            // 1. 处理 null (排在最后)
            if (StrUtil.isAllBlank(v1, v2)) {
                return 0;
            }
            if (StrUtil.isBlank(v1)) {
                return 1;
            }
            if (StrUtil.isBlank(v2)) {
                return -1;
            }
            try {
                // 2. 转 BigDecimal
                BigDecimal b1 = new BigDecimal(v1.trim());
                BigDecimal b2 = new BigDecimal(v2.trim());

                // 3. 降序：用 b2 比较 b1
                return b2.compareTo(b1);
            } catch (NumberFormatException e) {
                // 4. 如果不是数字，回退到字符串降序
                return v2.compareTo(v1);
            }
        });

        // 2. 创建窗口
        Stage stage = new Stage();
        stage.setTitle("结案项目列表 (共 " + finishedData.size() + " 条)");
        stage.initModality(Modality.NONE);

        // 获取父窗口并设置 Owner
        Window parentWindow = txtMainSearch.getScene().getWindow();
        if (parentWindow != null) {
            stage.initOwner(parentWindow);
        }

        // 3. 构建顶部操作栏
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(5));

        Label lblSearch = new Label("🔍 二次过滤:");
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("在此列表中搜索...");
        txtSearch.setPrefWidth(200);

        Button btnCopyParams = new Button("复制列表");
        Button btnExportParams = new Button("导出列表");

        topBar.getChildren().addAll(lblSearch, txtSearch, new Separator(javafx.geometry.Orientation.VERTICAL), btnCopyParams, btnExportParams);

        // 4. 构建表格
        TableView<UserAmountData> table = new TableView<>();
        // 搜索
        FilteredList<UserAmountData> filteredData = JfxTableFilterUtils.setupSearch(txtSearch, finishedData, table);

        JfxUtils.createColumnsFromAnnotations(table, UserAmountData.class, filteredData);
        // 列宽自适应
        // table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // 5. 绑定功能
        // 复制
        btnCopyParams.setOnAction(e -> JfxUtils.copyTableContent(table));
        // 导出
        btnExportParams.setOnAction(e -> JfxUtils.exportTableContent(table));

        // 6. 布局与尺寸
        VBox root = new VBox(5, topBar, table);
        root.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        // 计算 80% 宽高
        double targetWidth = 800;
        double targetHeight = 600;
        if (parentWindow != null) {
            targetWidth = parentWindow.getWidth() * 0.8;
            targetHeight = parentWindow.getHeight() * 0.8;
        }

        Scene scene = new Scene(root, targetWidth, targetHeight);
        stage.setScene(scene);

        // 居中
        if (parentWindow != null) {
            stage.setX(parentWindow.getX() + (parentWindow.getWidth() - targetWidth) / 2);
            stage.setY(parentWindow.getY() + (parentWindow.getHeight() - targetHeight) / 2);
        }

        stage.show();
    }

    @FXML
    public void handleFullscreen() {
        if (mainTableList.isEmpty()) {
            ToastUtils.alertWarning("当前没有数据，请先点击【个人项目金额】按钮");
            return;
        }

        try {
            Window parentWindow = mainTableView.getScene().getWindow();
            ViewNavigator.loadSceneMaxWindow("fullscreen-view.fxml", "全屏查看 (共 " + mainTableList.size() + " 条)", parentWindow, _ -> {
                fullscreenViewController.initData(mainTableList);
            });
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtils.alertError("错误", e.getMessage());
        }
    }

    @FXML
    public void handleWorkHourCount() {
        try {
            Window parentWindow = btnWorkHourCount.getScene().getWindow();
            ViewNavigator.loadSceneMaxWindow("work-hour-count.fxml", btnWorkHourCount.getText(), parentWindow, null);
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtils.alertError("错误", e.getMessage());
        }
    }

    private String getCellVal(Cell cell) {
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING); // 强转 String 防止数字格式问题
        return cell.getStringCellValue();
    }

}
