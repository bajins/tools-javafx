package com.bajins.tools.toolsjavafx.model;

import com.bajins.tools.toolsjavafx.utils.TableCol;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
 *
 * @author bajin
 */
public class MainData {
    @TableCol(
            value = "用户编码",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
            // minWidth = 80,
            // maxWidth = 80,
            // resizable = false,
            // sortable = false
    )
    private final StringProperty userCode;

    @TableCol(
            value = "用户名称",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty userName;

    @TableCol(
            value = "用户区域",
            width = 50,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty devRegion;

    @TableCol(
            value = "部门编码",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty deptCode;

    @TableCol(
            value = "部门名称",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty deptName;

    @TableCol(
            value = "上级部门编码",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty parentDeptCode;

    @TableCol(
            value = "上级部门名称",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty parentDeptName;

    @TableCol(
            value = "项目编码",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty pmProjectCode;

    @TableCol(
            value = "项目名称",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty projectName;

    @TableCol(
            value = "项目区域",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty pmRegion;

    @TableCol(
            value = "主担用户编码",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
            // minWidth = 80,
            // maxWidth = 80,
            // resizable = false,
            // sortable = false
    )
    private final StringProperty leadUserCode;

    @TableCol(
            value = "主担用户名称",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty leadUserName;

    @TableCol(
            value = "主担区域",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty leadDevRegion;

    @TableCol(
            value = "开发工时",
            width = 120,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER",
            numeric = true
    )
    private final StringProperty pmDevHours;

    @TableCol(
            value = "项目总工时",
            width = 120,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER",
            numeric = true
    )
    private final StringProperty pmPrjHours;

    @TableCol(
            value = "项目金额",
            width = 120,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER",
            numeric = true
    )
    private final StringProperty prjAmount;

    @TableCol(
            value = "个人金额",
            width = 120,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER",
            numeric = true
    )
    private final StringProperty ujAmount;

    @TableCol(
            value = "主担部门编码",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty leadDeptCode;

    @TableCol(
            value = "主担部门名称",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty leadDeptName;

    @TableCol(
            value = "主担上级部门编码",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty leadParentDeptCode;

    @TableCol(
            value = "主担上级部门名称",
            width = 100,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty leadParentDeptName;

    public MainData() {
        this.userCode = new SimpleStringProperty();
        this.userName = new SimpleStringProperty();
        this.devRegion = new SimpleStringProperty();
        this.deptCode = new SimpleStringProperty();
        this.deptName = new SimpleStringProperty();
        this.parentDeptCode = new SimpleStringProperty();
        this.parentDeptName = new SimpleStringProperty();
        this.pmProjectCode = new SimpleStringProperty();
        this.projectName = new SimpleStringProperty();
        this.pmRegion = new SimpleStringProperty();
        this.leadUserCode = new SimpleStringProperty();
        this.leadUserName = new SimpleStringProperty();
        this.leadDevRegion = new SimpleStringProperty();
        this.pmDevHours = new SimpleStringProperty();
        this.pmPrjHours = new SimpleStringProperty();
        this.prjAmount = new SimpleStringProperty();
        this.ujAmount = new SimpleStringProperty();
        this.leadDeptCode = new SimpleStringProperty();
        this.leadDeptName = new SimpleStringProperty();
        this.leadParentDeptCode = new SimpleStringProperty();
        this.leadParentDeptName = new SimpleStringProperty();
    }

    public String getUserCode() {
        return userCode.get();
    }

    public StringProperty userCodeProperty() {
        return userCode;
    }

    public String getUserName() {
        return userName.get();
    }

    public StringProperty userNameProperty() {
        return userName;
    }

    public String getDevRegion() {
        return devRegion.get();
    }

    public StringProperty devRegionProperty() {
        return devRegion;
    }

    public String getDeptCode() {
        return deptCode.get();
    }

    public StringProperty deptCodeProperty() {
        return deptCode;
    }

    public String getDeptName() {
        return deptName.get();
    }

    public StringProperty deptNameProperty() {
        return deptName;
    }

    public String getParentDeptCode() {
        return parentDeptCode.get();
    }

    public StringProperty parentDeptCodeProperty() {
        return parentDeptCode;
    }

    public String getParentDeptName() {
        return parentDeptName.get();
    }

    public StringProperty parentDeptNameProperty() {
        return parentDeptName;
    }

    public String getPmProjectCode() {
        return pmProjectCode.get();
    }

    public StringProperty pmProjectCodeProperty() {
        return pmProjectCode;
    }

    public String getProjectName() {
        return projectName.get();
    }

    public StringProperty projectNameProperty() {
        return projectName;
    }

    public String getPmRegion() {
        return pmRegion.get();
    }

    public StringProperty pmRegionProperty() {
        return pmRegion;
    }

    public String getLeadUserCode() {
        return leadUserCode.get();
    }

    public StringProperty leadUserCodeProperty() {
        return leadUserCode;
    }

    public String getLeadUserName() {
        return leadUserName.get();
    }

    public StringProperty leadUserNameProperty() {
        return leadUserName;
    }

    public String getLeadDevRegion() {
        return leadDevRegion.get();
    }

    public StringProperty leadDevRegionProperty() {
        return leadDevRegion;
    }

    public String getPmDevHours() {
        return pmDevHours.get();
    }

    public StringProperty pmDevHoursProperty() {
        return pmDevHours;
    }

    public String getPmPrjHours() {
        return pmPrjHours.get();
    }

    public StringProperty pmPrjHoursProperty() {
        return pmPrjHours;
    }

    public String getPrjAmount() {
        return prjAmount.get();
    }

    public StringProperty prjAmountProperty() {
        return prjAmount;
    }

    public String getUjAmount() {
        return ujAmount.get();
    }

    public StringProperty ujAmountProperty() {
        return ujAmount;
    }

    public String getLeadDeptCode() {
        return leadDeptCode.get();
    }

    public StringProperty leadDeptCodeProperty() {
        return leadDeptCode;
    }

    public String getLeadDeptName() {
        return leadDeptName.get();
    }

    public StringProperty leadDeptNameProperty() {
        return leadDeptName;
    }

    public String getLeadParentDeptCode() {
        return leadParentDeptCode.get();
    }

    public StringProperty leadParentDeptCodeProperty() {
        return leadParentDeptCode;
    }

    public String getLeadParentDeptName() {
        return leadParentDeptName.get();
    }

    public StringProperty leadParentDeptNameProperty() {
        return leadParentDeptName;
    }
}