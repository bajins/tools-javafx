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
            value = "总项目数",
            width = 50,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty totalPrjQty;

    @TableCol(
            value = "主担项目数",
            width = 50,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
    )
    private final StringProperty leadPrjQty;

    @TableCol(
            value = "协从项目数",
            width = 50,
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

    public UserAmountData(String userCode, String userName, String totalPrjQty, String leadPrjQty, String asstPrjQty, String amount) {
        this.userCode = new SimpleStringProperty(userCode);
        this.userName = new SimpleStringProperty(userName);
        this.totalPrjQty = new SimpleStringProperty(totalPrjQty);
        this.leadPrjQty = new SimpleStringProperty(leadPrjQty);
        this.asstPrjQty = new SimpleStringProperty(asstPrjQty);
        this.amount = new SimpleStringProperty(amount);
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
