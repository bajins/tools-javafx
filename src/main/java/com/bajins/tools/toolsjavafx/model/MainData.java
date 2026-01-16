package com.bajins.tools.toolsjavafx.model;

import com.bajins.tools.toolsjavafx.utils.TableCol;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
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
    private final StringProperty pmProjectName;

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

    public MainData(StringProperty userCode, StringProperty userName, StringProperty devRegion, StringProperty pmProjectCode, StringProperty pmProjectName, StringProperty pmRegion, StringProperty leadUserCode, StringProperty leadUserName, StringProperty leadDevRegion, StringProperty pmDevHours, StringProperty pmPrjHours, StringProperty prjAmount, StringProperty ujAmount) {
        this.userCode = userCode;
        this.userName = userName;
        this.devRegion = devRegion;
        this.pmProjectCode = pmProjectCode;
        this.pmProjectName = pmProjectName;
        this.pmRegion = pmRegion;
        this.leadUserCode = leadUserCode;
        this.leadUserName = leadUserName;
        this.leadDevRegion = leadDevRegion;
        this.pmDevHours = pmDevHours;
        this.pmPrjHours = pmPrjHours;
        this.prjAmount = prjAmount;
        this.ujAmount = ujAmount;
    }

    public MainData(String userCode, String userName, String devRegion, String pmProjectCode, String pmProjectName, String pmRegion, String leadUserCode, String leadUserName, String leadDevRegion, String pmDevHours, String pmPrjHours, String prjAmount, String ujAmount) {
        this.userCode = new SimpleStringProperty(userCode);
        this.userName = new SimpleStringProperty(userName);
        this.devRegion = new SimpleStringProperty(devRegion);
        this.pmProjectCode = new SimpleStringProperty(pmProjectCode);
        this.pmProjectName = new SimpleStringProperty(pmProjectName);
        this.pmRegion = new SimpleStringProperty(pmRegion);
        this.leadUserCode = new SimpleStringProperty(leadUserCode);
        this.leadUserName = new SimpleStringProperty(leadUserName);
        this.leadDevRegion = new SimpleStringProperty(leadDevRegion);
        this.pmDevHours = new SimpleStringProperty(pmDevHours);
        this.pmPrjHours = new SimpleStringProperty(pmPrjHours);
        this.prjAmount = new SimpleStringProperty(prjAmount);
        this.ujAmount = new SimpleStringProperty(ujAmount);
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

    public String getPmProjectCode() {
        return pmProjectCode.get();
    }

    public StringProperty pmProjectCodeProperty() {
        return pmProjectCode;
    }

    public String getPmProjectName() {
        return pmProjectName.get();
    }

    public StringProperty pmProjectNameProperty() {
        return pmProjectName;
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
}