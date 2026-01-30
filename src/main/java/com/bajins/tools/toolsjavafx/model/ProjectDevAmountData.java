package com.bajins.tools.toolsjavafx.model;

import com.bajins.tools.toolsjavafx.utils.TableCol;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
 *
 * @author bajins
 */
public class ProjectDevAmountData extends ProjectDevData {
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

    public ProjectDevAmountData() {
        super();
        this.prjAmount = new SimpleStringProperty();
        this.ujAmount = new SimpleStringProperty();
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