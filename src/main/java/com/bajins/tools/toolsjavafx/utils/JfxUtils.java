package com.bajins.tools.toolsjavafx.utils;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import com.bajins.tools.toolsjavafx.controller.FullscreenViewController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.table.TableFilter;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author bajin
 */
public class JfxUtils {

    /**
     * 缓存已查找的类字段，避免重复反射
     */
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 通用的数字字符串比较器
     * 支持整数、小数，处理了 null 和 非数字的情况
     */
    private static final Comparator<String> NUMERIC_COMPARATOR = (s1, s2) -> {
        // 处理 null (排在最后)
        if (StrUtil.isAllBlank(s1, s2)) {
            return 0;
        }
        if (StrUtil.isBlank(s1)) {
            return -1;
        }
        if (StrUtil.isBlank(s2)) {
            return 1;
        }

        try {
            // 2. 尝试转为 BigDecimal 比较 (精度最准)
            BigDecimal b1 = new BigDecimal(s1.trim());
            BigDecimal b2 = new BigDecimal(s2.trim());
            return b1.compareTo(b2);
        } catch (NumberFormatException ignored) {
            // 3. 如果解析失败（比如包含字母），回退到默认的字符串比较
            return s1.compareTo(s2);
        }
    };

    /**
     * 递归查找类及其所有父类中指定名称的字段
     *
     * @param clazz     开始查找的类
     * @param fieldName 要查找的字段名
     * @return 找到的字段对象，或 null 如果未找到
     */
    public static Field getFieldRecursive(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;

        // 循环向上查找，直到 Object 类
        while (currentClass != null && currentClass != Object.class) {
            try {
                // 尝试在当前类中找
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // 当前类没找到，继续去父类找
                currentClass = currentClass.getSuperclass();
            }
        }
        // 所有的类都找遍了也没找到
        return null;
    }

    /**
     * 递归查找类及其所有父类中所有字段
     *
     * @param clazz 开始查找的类
     * @return 找到的所有字段对象列表
     */
    public static List<Field> getFieldsRecursive(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        // computeIfAbsent: 如果缓存里有，直接返回；如果没有，执行后面的 lambda 计算并存入
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            List<Field> fields = new ArrayList<>();
            Class<?> currentClass = k;

            while (currentClass != null && currentClass != Object.class) {
                // Collections.addAll(fields, currentClass.getDeclaredFields());
                Field[] declaredFields = currentClass.getDeclaredFields();
                for (Field field : declaredFields) {
                    // 过滤掉编译器生成的合成字段 (如 this$0)
                    if (field.isSynthetic()) {
                        continue;
                    }
                    // 不需要静态字段
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    fields.add(field);
                }
                currentClass = currentClass.getSuperclass();
            }
            // 返回一个不可修改的 List，防止外部调用者修改缓存内容
            return Collections.unmodifiableList(fields);
        });
    }

    /**
     * 通用配置方法：将 @TableCol 注解的所有属性应用到 TableColumn
     *
     * @param column
     * @param clazz 在 Java 代码编译成字节码（.class 文件）后，泛型信息会被“泛型擦除” (Type Erasure)，因此必须传具体的类
     * @param fieldName
     */
    @SuppressWarnings({"unchecked"})
    public static <S, T> void configColumn(TableColumn<S, T> column, Class<?> clazz, String fieldName, FilteredList<S> filteredList) throws NoSuchMethodException {
        Field field = getFieldRecursive(clazz, fieldName);
        if (field == null) {
            return;
        }
        TableCol ann = field.getAnnotation(TableCol.class);

        configColumn(column, ann, field, filteredList);
        // --- B. 自动绑定 (数据部分) ---
        // 约定：字段 "colId" 对应的方法是 "colIdProperty()"
        String propertyMethodName = fieldName + "Property";

        // 优化：在配置列时查找一次 Method 对象，而不是在每次渲染单元格时查找
        Method method = clazz.getMethod(propertyMethodName);

        column.setCellValueFactory(cellData -> {
            try {
                // cellData.getValue() 获取的是行对象 (如 MainData 实例)
                Object rowData = cellData.getValue();
                // 调用 rowData.colIdProperty()
                return (ObservableValue) method.invoke(rowData);
            } catch (Exception ignored) {
                return null;
            }
        });
    }

    /**
     * 通用配置方法：将 @TableCol 注解的所有属性应用到 TableColumn
     *
     * @param column
     * @param ann
     * @param field
     */
    @SuppressWarnings({"unchecked"})
    public static <S, T> void configColumn(TableColumn<S, T> column, TableCol ann, Field field, FilteredList<S> filteredList) {
        if (ann != null) {
            // 1. 基础属性
            column.setText(ann.value());
            if (!ann.id().isEmpty()) {
                column.setId(ann.id());
            }
            if (!ann.styleClass().isEmpty()) {
                column.getStyleClass().add(ann.styleClass());
            }

            // 2. 尺寸
            column.setPrefWidth(ann.width());
            column.setMinWidth(ann.minWidth());
            column.setMaxWidth(ann.maxWidth());

            // 3. 行为开关
            column.setVisible(ann.visible());
            column.setResizable(ann.resizable());
            column.setSortable(ann.sortable());
            column.setReorderable(ann.reorderable());
            column.setEditable(ann.editable());

            // 4. 样式处理 (合并 style 和 alignment)
            String finalStyle = ann.style();

            // 处理快捷对齐 (如果设置了 alignment，自动转为 CSS)
            if (!ann.alignment().isEmpty()) {
                // 修正 CSS 语法，防止用户没加分号
                if (!finalStyle.endsWith(";") && !finalStyle.isEmpty()) {
                    finalStyle += ";";
                }
                // JavaFX 对齐 CSS 语法: -fx-alignment: CENTER;
                finalStyle += " -fx-alignment: " + ann.alignment() + ";";
            }

            if (!finalStyle.isEmpty()) {
                column.setStyle(finalStyle);
            }
            // 设置排序比较器
            if (ann.sortable() && ann.numeric()) {
                column.setComparator((Comparator<T>) NUMERIC_COMPARATOR);
            }
            if (field != null) {
                // 业务对象或上下文数据
                // column.setUserData(field.getName());
                // ObservableMap 可以监听内容的变化
                column.getProperties().put("fieldName", field.getName());
                if (filteredList != null) {
                    // 自定义表头：包含标题和搜索框
                    // column.setGraphic(JfxTableFilterUtils.createHeaderWithFilter(ann.value(), filteredList, field.getName(), column));
                    // column.setGraphic(JfxTableFilterUtils.createFilterHeader(column, filteredList));
                    // addHoverFilterToHeader(column, (btn) -> JfxTableFilterUtils.showFilterPopup(btn, column, filteredList));
                }
            }
        }
        if (field != null) {
            // 自动绑定(数据部分) Property (这里利用 Property 的命名规范: fieldName + "Property")
            // 约定：字段 "colId" 对应的方法是 "colIdProperty()"

            // PropertyValueFactory 会自动查找 fieldName + "Property()" 方法
            column.setCellValueFactory(new PropertyValueFactory<>(field.getName()));

            /*String propertyMethodName = field.getName() + "Property";
            // 优化：在配置列时查找一次 Method 对象，而不是在每次渲染单元格时查找
            java.lang.reflect.Method method = null;
            try {
                method = clazz.getMethod(propertyMethodName);
            } catch (NoSuchMethodException ignored) {
            }
            if (method != null) {
                java.lang.reflect.Method finalMethod = method;
                column.setCellValueFactory(cellData -> {
                    // cellData.getValue() 获取的是行对象 (如 MainData 实例)
                    Object rowData = cellData.getValue();
                    // 反射调用 getter 或者 property 方法
                    // 简单起见，这里假设 getter 叫 val1Property() 就是调用 rowData.colIdProperty()
                    // 实际项目中可以用 PropertyReferenceFactory 或更严谨的反射
                    try {
                        return (javafx.beans.value.ObservableValue) finalMethod.invoke(rowData);
                    } catch (Exception ignored) {
                        return null;
                    }
                });
            }*/
        }
    }


    /**
     * 自动反射生成列的工具方法
     *
     * @param table
     * @param clazz 在 Java 代码编译成字节码（.class 文件）后，泛型信息会被“泛型擦除” (Type Erasure)，因此必须传具体的类
     * @param <S>
     */
    public static <S, E> void createColumnsFromAnnotations(TableView<S> table, Class<S> clazz, FilteredList<S> filteredList) {
        // 获取所有声明的字段
        List<Field> fields = getFieldsRecursive(clazz);

        for (Field field : fields) {
            // 检查是否有 @TableCol 注解
            TableCol annotation = field.getAnnotation(TableCol.class);
            if (annotation == null) {
                continue;
            }
            // 创建列 在fxml中对应<columns>下的<TableColumn>
            TableColumn<S, String> column = new TableColumn<>(annotation.value());

            configColumn(column, annotation, field, filteredList);

            table.getColumns().add(column);

            if (annotation.sortable() && annotation.defaultSort() != -1) {
                // 设置默认排序列
                table.getSortOrder().add(column);
                // 设置排序类型 (ASCENDING 升序 / DESCENDING 降序)
                if (annotation.defaultSort() == 0) {
                    column.setSortType(TableColumn.SortType.ASCENDING);
                } else {
                    column.setSortType(TableColumn.SortType.DESCENDING);
                }
                // 触发排序
                table.sort();
            }
        }
        // “列宽自适应”策略 CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS 表格会自动调整所有列的宽度，手动设置的列宽将失效
        // table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        /*
        // 强制显示hbar 水平滚动条 vbar 垂直滚动条
        table.setStyle("-fx-hbar-policy: always;");
        // 注意：这种 lookup 方法需要在 Stage.show() 之后或者 Scene 渲染之后执行，否则可能找不到 ".virtual-flow" 节点。
        Platform.runLater(() -> {
            Node flow = table.lookup(".virtual-flow");
            if (flow != null) {
                flow.setStyle("-fx-vbar-policy: always; -fx-hbar-policy: always;");
            }
        });
        */
        // 这种内联方式在 Java 代码中比较难直接通过 setStyle 穿透到 .virtual-flow
        // 所以我们需要在 Scene 上挂载一段 CSS 数据 URL，或者你创建一个 style.css 文件
        /*table.skinProperty().addListener((a, b, newSkin) -> {
            // 当 Skin 加载完成后，查找 VirtualFlow
            javafx.scene.Node virtualFlow = table.lookup(".virtual-flow");
            if (virtualFlow != null) {
                virtualFlow.setStyle("-fx-vbar-policy: always;");
            }
        });*/

        TableFilter<S> tableFilter = TableFilter.forTableView(table).apply();
    }

    /**
     * 设置输入框在获取焦点时自动全选
     *
     * @param fields 需要应用此效果的输入框列表
     */
    public static void setupAutoSelectOnFocus(TextField... fields) {
        for (TextField field : fields) {
            field.focusedProperty().addListener((observable, oldValue, newValue) -> {
                // newValue = true 表示获得了焦点
                if (newValue) {
                    // 关键：使用 runLater 确保全选操作发生在鼠标点击定位光标之后
                    Platform.runLater(() -> {
                        if (!field.getText().isEmpty()) {
                            field.selectAll();
                        }
                    });
                }
            });
        }
    }

    /**
     * 限制输入框：
     * 1. 只能输入数字
     * 2. 限制最大值为 65535
     * 3. 限制不能超过 5 位数
     */
    public static void setupPortInputRestriction(TextField field) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();

            // 1. 如果是清空操作（内容为空），允许
            if (newText.isEmpty()) {
                return change;
            }

            // 2. 正则检查：必须全是数字
            if (newText.matches("\\d*")) {
                // 3. 长度检查：端口号最大是 65535 (5位)，超过5位直接拒绝，避免不必要的 parseInt 异常
                if (newText.length() > 5) {
                    return null;
                }

                try {
                    // 4. 范围检查：0 - 65535
                    long port = Long.parseLong(newText); // 用 Long 防止输入 99999 溢出 int
                    if (port >= 0 && port <= 65535) {
                        return change; // 合法，允许修改
                    }
                } catch (NumberFormatException ignored) {
                    // 解析失败（比如数字过大），拒绝
                }
            }

            // 不符合上述条件，返回 null 表示拒绝此次输入（用户按键无效）
            return null;
        };

        field.setTextFormatter(new TextFormatter<>(filter));
    }

    /**
     * 复制任意 TableView 的内容到剪贴板
     *
     * @param table
     * @param <S>
     */
    public static <S> void copyTableContent(TableView<S> table) {
        if (table.getItems().isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        ObservableList<? extends TableColumn<S, ?>> columns = table.getColumns();

        // 获取表头 (动态读取 TableColumn 的 text)
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<S, ?> tableColumn = columns.get(i);
            if (tableColumn.isVisible()) {
                sb.append(tableColumn.getText());
                // 如果不是最后一列，加制表符，否则换行
                sb.append(i == columns.size() - 1 ? "\n" : "\t");
            }
        }

        // 内容
        for (S row : table.getItems()) {
            for (int i = 0; i < columns.size(); i++) {
                TableColumn<S, ?> col = columns.get(i);
                if (col.isVisible()) {
                    // 使用 getCellData 动态获取该列对应的值
                    Object cellValue = col.getCellData(row);
                    // 处理 null 值，避免输出 "null" 字符串
                    String val = (cellValue == null) ? "" : cellValue.toString();

                    // 替换掉内容里可能存在的制表符和换行符，防止破坏格式
                    val = val.replace("\t", " ").replace("\n", " ");

                    sb.append(val);
                    sb.append(i == columns.size() - 1 ? "\n" : "\t");
                }
            }
        }
        ClipboardUtil.setStr(sb.toString());
        ToastUtils.alertInfo("已复制到剪贴板");
    }

    /**
     * 通用方法：导出任意 TableView 到 CSV
     */
    public static <S> void exportTableContent(TableView<S> table) {
        if (table.getItems().isEmpty()) {
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("导出数据");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File file = fc.showSaveDialog(table.getScene().getWindow());

        if (file != null) {
            List<List<String>> rows = new ArrayList<>();
            List<String> header = new ArrayList<>();

            // 表头
            for (TableColumn<S, ?> col : table.getColumns()) {
                if (col.isVisible()) {
                    // 动态获取 FXML 中定义的表头
                    header.add(col.getText());
                }
            }
            rows.add(header);

            // 内容
            for (S item : table.getItems()) {
                List<String> rowData = new ArrayList<>();
                for (TableColumn<S, ?> col : table.getColumns()) {
                    if (col.isVisible()) {
                        Object val = col.getCellData(item);
                        rowData.add(val == null ? "" : val.toString());
                    }
                }
                rows.add(rowData);
            }
            CsvUtil.getWriter(file, Charset.forName("GB2312")).write(rows);
        }
    }
}
