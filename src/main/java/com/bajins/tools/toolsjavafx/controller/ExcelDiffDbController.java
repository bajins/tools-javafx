package com.bajins.tools.toolsjavafx.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.simple.SimpleDataSource;
import com.bajins.tools.toolsjavafx.model.MainData;
import com.bajins.tools.toolsjavafx.model.RawData;
import com.bajins.tools.toolsjavafx.model.UserAmountData;
import com.bajins.tools.toolsjavafx.utils.JfxTableFilterUtils;
import com.bajins.tools.toolsjavafx.utils.JfxUtils;
import com.bajins.tools.toolsjavafx.utils.ToastUtils;
import com.solubris.typedtuples.mutable.MutableQuintuple;
import com.solubris.typedtuples.mutable.MutableTuple;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author bajin
 */
public class ExcelDiffDbController {

    // UI 组件
    @FXML
    private TextField filePathField;
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

    // 按钮
    @FXML
    private Button btnPreview;
    @FXML
    private Button btnRun, btnCopy, btnExport, btnUtAmount;
    @FXML
    private Label statusLabel;

    // 主界面表格
    @FXML
    private TableView<MainData> tableView;

    // 数据源
    // 1. 用于暂存导入的原始数据，不直接显示在主界面
    private List<RawData> cachedData = new ArrayList<>();

    // 2. 主界面表格绑定的数据列表 (仅在运行时填充)
    private final ObservableList<MainData> mainTableList = FXCollections.observableArrayList();

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
        FilteredList<MainData> filteredData = JfxTableFilterUtils.setupSearch(txtMainSearch, mainTableList, tableView);
        // 1. 配置列属性 (表头、宽度、样式 全部由实体类注解决定)
        JfxUtils.createColumnsFromAnnotations(tableView, MainData.class, filteredData);

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
     * 运行
     */
    @FXML
    public void handleRun() {
        if (cachedData.isEmpty()) {
            ToastUtils.alertError("提示", "无数据，请先导入/粘贴");
            return;
        }

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

        btnRun.setDisable(true);
        statusLabel.setText("正在连接数据库查询...");

        // 后台任务
        Task<Void> task = new Task<>() {
            // 用于收集结果的临时列表 (MainData)
            List<MainData> resultList = new ArrayList<>();

            @Override
            protected Void call() throws Exception {
                // 3.1 动态构建 URL
                String url = buildUrl(dbType, ip, port, dbName);

                /*// 自定义数据库Setting，更多实用请参阅Hutool-Setting章节
                Setting setting = new Setting();
                // 获取指定配置，第二个参数为分组，用于多数据源，无分组情况下传null
                // 注意此处DSFactory需要复用或者关闭
                DSFactory dsFactory = DSFactory.create(setting);
                DataSource ds = dsFactory.getDataSource();*/

                // SimpleDataSource只是DriverManager.getConnection的简单包装，本身并不支持池化功能，此类特别适合少量数据库连接的操作。
                DataSource ds = new SimpleDataSource(url, user, pass);
                Db db = Db.use(ds);

                String sql = """
                        -- 查询所有人员的投入项目工时，考虑实际情况，可能开发一部分然后会把需求转给其他人，使用pm_hours_log查实际投入
                        with top as (
                            select phl.pm_task_code, phl.user_code, pnp.pm_project_code, phl.pm_calculate_hours
                            from pm_hours_log phl
                            join pm_emp emp on phl.user_code=emp.user_code and emp.pm_ps_type=1 --and emp.pm_arrange_user='PG2006471'
                            --		and phl.created_date >= DATE_TRUNC('year', CURRENT_DATE) AND phl.created_date < DATE_TRUNC('year', CURRENT_DATE) + INTERVAL '1 year'
                            --	and phl.created_date >= '2025-01-01 00:00:00' AND phl.created_date <= '2025-12-31 23:59:59'
                                and emp.pm_arrange_user in ('PG1605125','PG1508090','PG1706192','PG1505071','PG2006471')
                            join pm_dev pd on pd.pm_develop_code=phl.pm_task_code
                            join pm_needs_propose pnp on pnp.pm_needs_code=pd.pm_needs_code and pnp.pm_project_code not in ('PGKF2017','D00902')
                            where 1=1
                        --	and pnp.pm_project_code in ('')
                        ),
                        ph as (
                            select top.pm_project_code, sum(top.pm_calculate_hours) pm_prj_hours
                            from top
                            group by top.pm_project_code
                        ),
                        uh as (
                            select top.user_code, top.pm_project_code, sum(top.pm_calculate_hours) pm_dev_hours
                            from top
                            group by top.user_code, top.pm_project_code
                        ),
                        uph as (
                            select uh.user_code, iu.user_name, uh.pm_project_code, uh.pm_dev_hours, ph.pm_prj_hours, pp.pm_region, pr.pm_region_name, pp.pm_project_name
                            from uh
                            join ims_user iu on iu.user_code=uh.user_code
                            join ph on ph.pm_project_code=uh.pm_project_code
                            join pm_project pp on pp.pm_project_code=uh.pm_project_code
                            left join pm_emp pe on pe.user_code=uh.user_code
                            left join pm_region pr on pr.pm_region_code=pe.pm_region_code
                        ),
                        ld as (
                            select
                                tpp.pm_project_code,
                                tpp.user_code,
                                tpp.pm_region_code,
                                peu.user_name,
                                pr.pm_region_name
                            from (
                                select
                                    ppe.pm_project_code,
                                    ppe.pm_transfer_in_date,
                                    ppe.created_date,
                                    pe.user_code,
                                    pe.pm_region_code,
                                    ROW_NUMBER() OVER (PARTITION BY ppe.pm_project_code ORDER BY ppe.pm_transfer_in_date DESC, ppe.created_date DESC) as rn
                                from pm_prj_emp ppe
                                join top on ppe.pm_project_code=top.pm_project_code and ppe.pm_is_lead_developer='y'
                                -- 考虑不同的部门
                                join pm_emp pe on pe.user_code=ppe.user_code --and pe.pm_region_code<>'05'
                                    and pe.pm_arrange_user in ('PG1605125','PG1508090','PG1706192','PG1505071','PG2006471')
                            ) tpp
                            join ims_user peu on tpp.rn=1 and peu.user_code=tpp.user_code
                            join pm_region pr on pr.pm_region_code=tpp.pm_region_code
                        ),
                        res as (
                            select uph.user_code, uph.user_name, uph.pm_region_name as dev_region, uph.pm_project_code,
                                CASE\s
                                    WHEN uph.pm_project_name ~ '^[a-zA-Z]+$' THEN
                                        -- 全是英文
                                        uph.pm_project_name
                                    WHEN uph.pm_project_name ~ '^[a-zA-Z]' THEN
                                        -- 以英文开头
                                        LEFT(uph.pm_project_name, 8)
                                    ELSE
                                        -- 包含中文或其他字符
                                        LEFT(NULLIF(TRIM(uph.pm_project_name),''), 6)
                                END project_name,
                                case uph.pm_region
                                    when 1 then '华南'
                                    when 2 then '华东'
                                    when 3 then '西南'
                                    when 4 then '华北'
                                    when 5 then '华中'
                                    else uph.pm_region::numeric::TEXT
                                end pm_region,
                                ld.user_code as lead_user_code,
                                ld.user_name as lead_user_name,
                                ld.pm_region_name as lead_dev_region,
                                uph.pm_dev_hours,
                                uph.pm_prj_hours
                            from uph
                            left join ld on ld.pm_project_code=uph.pm_project_code
                        )
                        select res.user_code, res.user_name, res.dev_region, res.pm_project_code,
                            res.project_name, res.pm_region, res.lead_user_code, res.lead_user_name,
                            case when res.lead_dev_region is null then res.pm_region else res.lead_dev_region end lead_dev_region,
                            res.pm_dev_hours, res.pm_prj_hours
                        from res
                        """;

                // 数据转换方便后续快速获取
                Map<String, String> paMap = new HashMap<>();
                StringJoiner stringJoiner = new StringJoiner("','", " and pnp.pm_project_code in ('", "')");
                for (RawData rawData : cachedData) {
                    paMap.put(rawData.getPmProjectCode(), rawData.getPrjAmount());
                    stringJoiner.add(rawData.getPmProjectCode());
                }
                List<Entity> entities = db.query(sql.replace("--\tand pnp.pm_project_code in ('')", stringJoiner.toString()));
                if (entities.isEmpty()) {
                    updateMessage("查询到 0 条数据");
                    return null;
                }

                // 3.3 比对逻辑
                updateMessage("正在比对 " + entities.size() + " 条数据...");

                for (Entity entity : entities) {

                    String userCode = entity.getStr("user_code");
                    String userName = entity.getStr("user_name");
                    String devRegion = entity.getStr("dev_region");
                    String pmProjectCode = entity.getStr("pm_project_code");
                    String projectName = entity.getStr("project_name");
                    String pmRegion = entity.getStr("pm_region");
                    String leadUserCode = entity.getStr("lead_user_code");
                    String leadUserName = entity.getStr("lead_user_name");
                    String leadDevRegion = entity.getStr("lead_dev_region");
                    String pmDevHours = entity.getStr("pm_dev_hours");
                    String pmPrjHours = entity.getStr("pm_prj_hours");

                    String prjAmount = paMap.get(pmProjectCode);
                    String ujAmount = "";

                    if (prjAmount != null) {
                        // 工时占比 = 该人在该项目的工时 / 该项目的总工时
                        BigDecimal whp = new BigDecimal(pmDevHours).divide(new BigDecimal(pmPrjHours), 6, RoundingMode.HALF_UP);
                        // 个人金额 = 该项目总金额 × 工时占比
                        ujAmount = new BigDecimal(prjAmount).multiply(whp).setScale(2, RoundingMode.HALF_UP).toPlainString();
                    }
                    resultList.add(new MainData(userCode, userName, devRegion, pmProjectCode, projectName, pmRegion, leadUserCode, leadUserName, leadDevRegion, pmDevHours, pmPrjHours, prjAmount, ujAmount));

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
    public void handleCopyResults() {
        JfxUtils.copyTableContent(tableView);
    }

    /**
     * 导出结果
     */
    @FXML
    public void handleExportResults() {
        if (mainTableList.isEmpty()) {
            return;
        }
        JfxUtils.exportTableContent(tableView);
    }

    @FXML
    public void handleShowFinished() {
        if (mainTableList.isEmpty()) {
            ToastUtils.alertWarning("当前没有数据，请先运行比对");
            return;
        }

        // 根据用户分组统计值
        Map<String, MutableQuintuple<String, Integer, Integer, Integer, BigDecimal>> sumOnlyMap = new HashMap<>();
        HashSet<String> leadPrjQtySet = new HashSet<>();
        HashSet<String> asstPrjQtySet = new HashSet<>();
        for (MainData mainData : mainTableList) {
            String userCode = mainData.getUserCode();
            String userName = mainData.getUserName();

            if (StrUtil.isBlank(mainData.getPrjAmount())) {
                continue;
            }
            MutableQuintuple<String, Integer, Integer, Integer, BigDecimal> sumOnly = sumOnlyMap.computeIfAbsent(userCode, k -> MutableTuple.of(userName, 0, 0, 0, BigDecimal.ZERO));

            String key = userCode + mainData.getPmProjectCode();
            if (userCode.equals(mainData.getLeadUserCode())) {
                // 主担项目
                if (!leadPrjQtySet.contains(key)) {
                    sumOnly.setThird(sumOnly.getThird() + 1);
                    leadPrjQtySet.add(key);
                }
            } else if (!asstPrjQtySet.contains(key)) {
                // 协从项目
                sumOnly.setFourth(sumOnly.getFourth() + 1);
                asstPrjQtySet.add(key);
            }
            sumOnly.setSecond(sumOnly.getThird() + sumOnly.getFourth());
            BigDecimal ujAmount = new BigDecimal(mainData.getUjAmount());
            sumOnly.setFifth(sumOnly.getFifth().add(ujAmount));
        }
        if (sumOnlyMap.isEmpty()) {
            ToastUtils.alertInfo("没有找到符合条件的数据");
            return;
        }
        // 对过滤的数据进行包装排序
        ObservableList<UserAmountData> finishedData = FXCollections.observableArrayList();
        for (Map.Entry<String, MutableQuintuple<String, Integer, Integer, Integer, BigDecimal>> entry : sumOnlyMap.entrySet()) {
            String userCode = entry.getKey();

            MutableQuintuple<String, Integer, Integer, Integer, BigDecimal> value = entry.getValue();
            String userName = value.getFirst();
            String totalPrjQty = Integer.toString(value.getSecond());
            String leadPrjQty = Integer.toString(value.getThird());
            String asstPrjQty = Integer.toString(value.getFourth());
            String amount = value.getFifth().toPlainString();
            finishedData.add(new UserAmountData(userCode, userName, totalPrjQty, leadPrjQty, asstPrjQty, amount));
        }
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
        javafx.stage.Window parentWindow = txtMainSearch.getScene().getWindow();
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

    // 辅助方法
    private String buildUrl(String type, String ip, String port, String db) {
        if ("MySQL".equals(type)) {
            return String.format("jdbc:mysql://%s:%s/%s?useSSL=false", ip, port, db);
        }
        if ("PostgreSQL".equals(type)) {
            return String.format("jdbc:postgresql://%s:%s/%s", ip, port, db);
        }
        if ("Oracle".equals(type)) {
            return String.format("jdbc:oracle:thin:@%s:%s:%s", ip, port, db);
        }
        if ("SQLServer".equals(type)) {
            return String.format("jdbc:sqlserver://%s:%s;databaseName=%s", ip, port, db);
        }
        return "";
    }

    private String getCellVal(Cell cell) {
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING); // 强转 String 防止数字格式问题
        return cell.getStringCellValue();
    }
}
