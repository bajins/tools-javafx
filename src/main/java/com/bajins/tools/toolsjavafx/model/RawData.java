package com.bajins.tools.toolsjavafx.model;

import com.bajins.tools.toolsjavafx.utils.TableCol;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author bajin
 */
public class RawData {
    @TableCol(
            value = "项目编码",
            width = 200,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER"
            // minWidth = 80,
            // maxWidth = 80,
            // resizable = false,
            // sortable = false
    )
    private final StringProperty pmProjectCode;

    @TableCol(
            value = "项目金额",
            width = 200,
            // style = "-fx-font-weight: bold; -fx-text-fill: red;",
            alignment = "CENTER",
            // minWidth = 80,
            // maxWidth = 80,
            // resizable = false,
            // sortable = false
            numeric = true
    )
    private final StringProperty prjAmount;

    public RawData(String pmProjectCode, String prjAmount) {
        this.pmProjectCode = new SimpleStringProperty(pmProjectCode);
        this.prjAmount = new SimpleStringProperty(prjAmount);
    }

    public String getPmProjectCode() {
        return pmProjectCode.get();
    }

    public StringProperty pmProjectCodeProperty() {
        return pmProjectCode;
    }

    public String getPrjAmount() {
        return prjAmount.get();
    }

    public StringProperty prjAmountProperty() {
        return prjAmount;
    }
}
