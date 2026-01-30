package com.bajins.tools.toolsjavafx.controller;

import cn.hutool.db.Entity;
import com.bajins.tools.toolsjavafx.model.ProjectDevAmountData;
import com.bajins.tools.toolsjavafx.model.ProjectDevData;
import com.bajins.tools.toolsjavafx.service.MainService;
import com.bajins.tools.toolsjavafx.utils.JfxTableFilterUtils;
import com.bajins.tools.toolsjavafx.utils.JfxUtils;
import com.bajins.tools.toolsjavafx.utils.ToastUtils;
import com.dlsc.gemsfx.daterange.DateRange;
import com.dlsc.gemsfx.daterange.DateRangePicker;
import com.dlsc.gemsfx.daterange.DateRangePreset;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.apache.xmlbeans.impl.xb.xsdschema.Facet;
import org.controlsfx.control.CheckComboBox;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author bajins
 */
@Singleton
public class WorkHourCountController implements Initializable {

    @FXML
    private DateRangePicker datePicker;
    @FXML
    private CheckComboBox<String> workerCombo;
    @FXML
    private TextField txtMainSearch;
    @FXML
    private Button btnRun, btnCopy, btnExport;
    @FXML
    private Label statusLabel;
    @FXML
    private TableView<ProjectDevData> mainTableView;

    // 主界面表格绑定的数据列表 (仅在运行时填充)
    private final ObservableList<ProjectDevData> mainTableList = FXCollections.observableArrayList();

    private final MainService mainService;

    @Inject
    public WorkHourCountController(MainService mainService) {
        this.mainService = mainService;
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
        setDateRangePreset();

        ObservableList<String> workers = FXCollections.observableArrayList();
        try {
            List<Entity> entities = mainService.queryArrangeUser();
            for (Entity entity : entities) {
                workers.add(entity.getStr("user_code") + "-" + entity.getStr("user_name"));
            }
        } catch (SQLException e) {
            ToastUtils.alertError("查询排产人失败", e.getMessage());
        }
        workerCombo.getItems().addAll(workers);

        // 绑定搜索逻辑
        FilteredList<ProjectDevData> filteredData = JfxTableFilterUtils.setupSearch(txtMainSearch, mainTableList, mainTableView);
        filteredData.addListener((ListChangeListener<ProjectDevData>) c -> {
            while (c.next()) { // Handle adds/removes
                statusLabel.setText("过滤后 " + filteredData.size() + " 条");
            }
        });
        // 1. 配置列属性 (表头、宽度、样式 全部由实体类注解决定)
        JfxUtils.createColumnsFromAnnotations(mainTableView, ProjectDevData.class, filteredData);
    }

    /**
     * 设置日期范围选择器的预设值
     */
    private void setDateRangePreset() {
        // 1. 获取当前日期
        LocalDate today = LocalDate.now();

        // 2. 创建预设 (Presets)
        // "最近7天"
        /*DateRangePreset last7Days = new DateRangePreset("最近7天", () -> new DateRange(
                today.minusDays(6),
                today
        ));*/
        // "本月"
        DateRangePreset thisMonth = new DateRangePreset("本月", () -> new DateRange(today.with(TemporalAdjusters.firstDayOfMonth()),
                today.with(TemporalAdjusters.lastDayOfMonth())));
        // "上月"
        DateRangePreset lastMonth = new DateRangePreset("上月", () -> new DateRange(
                today.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(1),
                today.with(TemporalAdjusters.lastDayOfMonth()).minusMonths(1)
        ));
        // "今年"
        DateRangePreset thisYear = new DateRangePreset("今年", () -> new DateRange(
                today.with(TemporalAdjusters.firstDayOfYear()),
                today.with(TemporalAdjusters.lastDayOfYear())
        ));
        // 去年
        DateRangePreset lastYear = new DateRangePreset("去年", () -> new DateRange(
                today.with(TemporalAdjusters.firstDayOfYear()).minusYears(1),
                today.with(TemporalAdjusters.lastDayOfYear()).minusYears(1)
        ));

        // 3. 将预设添加到 Picker 中
        ObservableList<DateRangePreset> presets = datePicker.getDateRangeView().getPresets();
        presets.clear();
        presets.addAll(thisMonth, lastMonth, thisYear, lastYear);

        // 4. 设置默认选中“本月”
        datePicker.setValue(thisMonth.getDateRangeSupplier().get());

        // 5. 监听选择变化
        /*datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("选中范围: " + newVal.getStartDate() + " 到 " + newVal.getEndDate());
        });*/
    }

    /**
     * 运行查询
     */
    @FXML
    public void handleRun() {
        // 获取 DateRange 对象
        DateRange selectedRange = datePicker.getValue();
        // 判空检查 (非常重要，因为用户可能还没选)
        if (selectedRange == null) {
            ToastUtils.alertError("错误", "请选择日期范围。");
            return;
        }
        btnRun.setDisable(false);

        // 提取开始和结束日期
        LocalDate start = selectedRange.getStartDate();
        LocalDate end = selectedRange.getEndDate();

        ObservableList<String> selectedWorkers = workerCombo.getCheckModel().getCheckedItems();

        /*if (selectedWorkers.isEmpty()) {
            ToastUtils.alertError("错误", "请至少选择一个排产人。");
            return;
        }*/
        // btnRun.setDisable(true);
        statusLabel.setText("正在连接数据库查询...");
        try {
            // 从选择的排产人列表中提取 user_code 并用','连接
            String arrangeUserCodesStr = selectedWorkers.stream()
                    .map(worker -> worker.split("-")[0])
                    .collect(Collectors.joining("','", "('", "')"));
            List<Entity> entities = mainService.queryProjectDetail(start, end, null, arrangeUserCodesStr);
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

                ProjectDevData projectDevData = new ProjectDevData();
                projectDevData.userCodeProperty().set(userCode);
                projectDevData.userNameProperty().set(userName);
                projectDevData.devRegionProperty().set(devRegion);
                projectDevData.deptCodeProperty().set(deptCode);
                projectDevData.deptNameProperty().set(deptName);
                projectDevData.parentDeptCodeProperty().set(parentDeptCode);
                projectDevData.parentDeptNameProperty().set(parentDeptName);
                projectDevData.pmProjectCodeProperty().set(pmProjectCode);
                projectDevData.projectNameProperty().set(projectName);
                projectDevData.pmRegionProperty().set(pmRegion);
                projectDevData.leadUserCodeProperty().set(leadUserCode);
                projectDevData.leadUserNameProperty().set(leadUserName);
                projectDevData.leadDevRegionProperty().set(leadDevRegion);
                projectDevData.leadDeptCodeProperty().set(leadDeptCode);
                projectDevData.leadDeptNameProperty().set(leadDeptName);
                projectDevData.leadParentDeptCodeProperty().set(leadParentDeptCode);
                projectDevData.leadParentDeptNameProperty().set(leadParentDeptName);
                projectDevData.pmDevHoursProperty().set(pmDevHours.stripTrailingZeros().toPlainString());
                projectDevData.pmPrjHoursProperty().set(pmPrjHours.stripTrailingZeros().toPlainString());
                mainTableList.add(projectDevData);
                // Platform.runLater(() -> raw.setCompareResult(statusStr));
            }
            statusLabel.setText("查询完成，共 " + mainTableList.size() + " 条数据");
            btnCopy.setDisable(false);
            btnExport.setDisable(false);
        } catch (SQLException e) {
            btnCopy.setDisable(true);
            btnExport.setDisable(true);
            statusLabel.setText("查询失败");
            ToastUtils.alertError("查询项目详情失败", e.getMessage());
        } finally {
            // btnRun.setDisable(true);
        }
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
        JfxUtils.exportTableContent(mainTableView);
    }
}
