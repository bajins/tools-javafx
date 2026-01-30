package com.bajins.tools.toolsjavafx.model;

import com.bajins.tools.toolsjavafx.utils.TableCol;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author bajin
 */
public class UserAmountData {
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
            width = 80,
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
            value = "总项目数",
            width = 80,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty totalPrjQty;

    @TableCol(
            value = "主担项目数",
            width = 80,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty leadPrjQty;

    @TableCol(
            value = "协从项目数",
            width = 80,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty asstPrjQty;

    @TableCol(
            value = "金额",
            width = 200,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER",
            // minWidth = 80,
            // maxWidth = 80,
            // resizable = false,
            // sortable = false
            numeric = true
    )
    private final StringProperty amount;

    public UserAmountData() {
        this.userCode = new SimpleStringProperty();
        this.userName = new SimpleStringProperty();
        this.devRegion = new SimpleStringProperty();
        this.deptCode = new SimpleStringProperty();
        this.deptName = new SimpleStringProperty();
        this.parentDeptCode = new SimpleStringProperty();
        this.parentDeptName = new SimpleStringProperty();
        this.totalPrjQty = new SimpleStringProperty();
        this.leadPrjQty = new SimpleStringProperty();
        this.asstPrjQty = new SimpleStringProperty();
        this.amount = new SimpleStringProperty();
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

    public String getTotalPrjQty() {
        return totalPrjQty.get();
    }

    public StringProperty totalPrjQtyProperty() {
        return totalPrjQty;
    }

    public String getLeadPrjQty() {
        return leadPrjQty.get();
    }

    public StringProperty leadPrjQtyProperty() {
        return leadPrjQty;
    }

    public String getAsstPrjQty() {
        return asstPrjQty.get();
    }

    public StringProperty asstPrjQtyProperty() {
        return asstPrjQty;
    }

    public String getAmount() {
        return amount.get();
    }

    public StringProperty amountProperty() {
        return amount;
    }
}
