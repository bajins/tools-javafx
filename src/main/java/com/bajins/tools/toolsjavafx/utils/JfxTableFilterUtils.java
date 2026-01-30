package com.bajins.tools.toolsjavafx.utils;

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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ControlsFX的在表头添加筛选实现：TableFilter.forTableView(tableView).apply();
 *
 * @author bajin
 */
public class JfxTableFilterUtils {

    // 定义一个静态缓存，防止每次搜索都重复反射，极大提升性能
    public static final Map<Class<?>, List<Method>> SEARCH_METHOD_CACHE = new ConcurrentHashMap<>();
    // 用于在 TableView 中存储“各列筛选条件 Map”的 Key
    private static final String TABLE_FILTERS_KEY = "table_global_filters_map";


    /**
     * 创建一个包含 TextField 的自定义表头输入框筛选节点
     *
     * @param title 表头标题
     * @param filteredList 数据源
     * @param type 筛选类型
     */
    private static <S, T, E> VBox createHeaderWithFilter(String title, FilteredList<S> filteredList, String type, TableColumn<S, T> column) {
        // 标题
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");

        // 筛选框
        TextField textField = new TextField();
        textField.setPromptText("筛选...");
        // 让输入框宽度跟随列宽变化
        // subtract(20) 是关键：预留约 20px 给左右内边距(Padding)和排序箭头(Sort Arrow)
        // 如果不减去这个值，输入框会把表头撑爆，导致死循环或显示异常
        textField.prefWidthProperty().bind(column.widthProperty().subtract(20));

        // 监听输入
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(row -> {
                // 如果输入框为空，显示所有
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                // 2. 空行保护
                if (row == null) {
                    return false;
                }

                String lowerVal = newValue.toLowerCase();
                Class<?> clazz = row.getClass();
                try {
                    String propName = type + "Property";
                    Method method = clazz.getMethod(propName);
                    // 调用方法获取值
                    Object result = method.invoke(row);

                    if (result == null) {
                        return false;
                    }

                    String valueStr;
                    // 关键修正：如果返回的是 Property 对象，需要解包取 getValue()
                    if (result instanceof ObservableValue) {
                        Object innerVal = ((ObservableValue<?>) result).getValue();
                        if (innerVal == null) {
                            return false;
                        }
                        valueStr = String.valueOf(innerVal);
                    } else {
                        // 如果是普通 Getter 返回的值
                        valueStr = String.valueOf(result);
                    }

                    // 包含匹配 (忽略大小写)
                    if (valueStr.toLowerCase().contains(lowerVal)) {
                        return true;
                    }

                } catch (Exception ignored) {
                    // 忽略反射异常，继续检查下一个字段
                }
                return false;
            });
        });

        // 阻止点击 TextField 时触发“列排序”
        // 默认情况下点击 Header 区域任何位置都会触发排序，这会导致输入时体验不好
        textField.setOnMouseClicked(Event::consume);

        VBox vbox = new VBox(5, label, textField);
        vbox.setAlignment(Pos.CENTER); // 居中对齐
        vbox.setPadding(new Insets(5, 0, 5, 0)); // 上下给点空隙，左右为0，靠 binding 控制宽度
        return vbox;
    }

    /**
     * 为列添加一个“仅悬停显示、靠右对齐”的筛选按钮
     *
     * @param column 目标列
     * @param onFilterClick 点击筛选图标时的回调逻辑
     */
    public static <S, T> void addHoverFilterToHeader(TableColumn<S, T> column, Function<Button, ContextMenu> onFilterClick) {

        // 1. 创建容器 StackPane
        StackPane headerPane = new StackPane();

        // 【关键布局】让 StackPane 的宽度始终跟随列宽，减去一点边距（预留排序箭头位置）
        // 如果不绑定宽度，StackPane 只会包裹文字，导致图标紧挨着文字而不是在最右边
        headerPane.prefWidthProperty().bind(column.widthProperty().subtract(20));

        // 2. 创建标题 Label
        Label titleLabel = new Label(column.getText());
        titleLabel.textProperty().bind(column.textProperty()); // 绑定原标题
        titleLabel.setStyle("-fx-font-weight: bold;");
        // 设为左对齐或居中，看你需要
        StackPane.setAlignment(titleLabel, Pos.CENTER);

        // 3. 创建筛选按钮
        // 这里可以用图标，如 FontAwesome、FontIcon (ikonli 库) 或 ImageView 加载一个漏斗图片
        Button filterBtn = new Button("▼");
        filterBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 10px;");
        filterBtn.setPadding(new Insets(2, 5, 2, 5)); // 调整点击区域大小
        // 强制靠右
        StackPane.setAlignment(filterBtn, Pos.CENTER_RIGHT);

        // 只有鼠标悬停在 Header 上时才显示按钮
        // 注意：这里绑定的是 headerPane 的 hover，而不是 column 的
        filterBtn.visibleProperty().bind(headerPane.hoverProperty());
        // 创建一个属性来记录“菜单是否打开”
        BooleanProperty isMenuOpen = new SimpleBooleanProperty(false);
        // 修改可见性绑定 逻辑：(鼠标悬停) 或者 (菜单打开了) -> 按钮都要显示
        filterBtn.visibleProperty().bind(headerPane.hoverProperty().or(isMenuOpen));
        // 当菜单打开时，文字变蓝；否则默认黑色
        filterBtn.styleProperty().bind(
                Bindings.when(isMenuOpen)
                        .then("-fx-background-color: transparent; -fx-text-fill: blue; -fx-font-weight: bold;")
                        .otherwise("-fx-background-color: transparent; -fx-text-fill: black;")
        );

        // 点击按钮时，执行筛选逻辑
        filterBtn.setOnAction(e -> {
            // 【非常重要】阻止事件冒泡！
            // 否则点击按钮不仅会触发筛选，还会触发表头的“排序”功能
            e.consume();
            if (onFilterClick != null) {
                ContextMenu popup = onFilterClick.apply(filterBtn);
                if (popup != null) {
                    // 当弹窗显示时，标记为 true -> 强制按钮保持显示
                    popup.setOnShown(event -> isMenuOpen.set(true));
                    // 当弹窗关闭时，标记为 false -> 恢复为只由 hover 控制
                    popup.setOnHidden(event -> isMenuOpen.set(false));
                }
            }
        });

        // 4. 组装
        headerPane.getChildren().add(titleLabel);
        headerPane.getChildren().add(filterBtn);

        // 5. 应用到列
        column.setGraphic(headerPane);
        // 清空原本的 Text，只用 Graphic 显示
        // column.setText("");
    }

    /**
     * 创建带筛选图标的表头
     *
     * @param col
     * @param filteredData
     * @return
     * @param <S>
     * @param <T>
     * @param <R>
     * @param <E>
     */
    public static <S, T, R, E> HBox createFilterHeader(TableColumn<S, T> col, FilteredList<S> filteredData) {
        /*Label label = new Label(col.getText());
        label.setMaxWidth(Double.MAX_VALUE);*/

        // 筛选按钮
        // 这里可以用图标，如 FontAwesome、FontIcon (ikonli 库) 或 ImageView 加载一个漏斗图片
        Button filterBtn = new Button("▼");
        filterBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 10px; -fx-padding: 0 0 0 5;");

        // 点击按钮弹出筛选菜单
        filterBtn.setOnAction(e -> {
            showFilterPopup(filterBtn, col, filteredData);
        });

        HBox hbox = new HBox(filterBtn);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        // HBox.setHgrow(label, Priority.ALWAYS);
        return hbox;
    }

    /**
     * 显示带搜索、全选功能的筛选弹窗
     *
     * @param owner
     * @param column
     * @param filteredData
     * @return
     * @param <S>
     * @param <T>
     */
    public static <S, T> ContextMenu showFilterPopup(Button owner, TableColumn<S, T> column, FilteredList<S> filteredData) {

        // 1. 获取数据源 (使用 getSource 获取原始全量数据)
        // 即使表格当前被过滤了，下拉框也应该显示所有可能的选项
        List<S> sourceList = (List<S>) filteredData.getSource();

        // 提取该列所有不重复的值 (转为 String 用于显示)
        List<String> uniqueValues = sourceList.stream()
                // 过滤空值
                .filter(Objects::nonNull)
                .map(item -> {
                    // 利用列的 CellValueFactory，无需知道具体实体类方法
                    T cellData = column.getCellData(item);
                    return cellData == null ? "" : cellData.toString();
                })
                .distinct()
                // 排序 注意：如果 T 没有实现 Comparable，这里会报错，需要额外处理
                /*.sorted((v1, v2) -> {
                    if (v1 instanceof Comparable) {
                        return ((Comparable) v1).compareTo(v2);
                    }
                    return v1.toString().compareTo(v2.toString());
                })*/
                // 收集结果
                .collect(Collectors.toList());

        // 2. 获取全局筛选 Map (从 TableView 中获取)
        TableView<S> tableView = column.getTableView();
        Map<TableColumn<S, T>, List<String>> globalFilterMap = getGlobalFilterMap(tableView);

        // 获取当前列之前的选中状态 (如果没有，默认全选)
        List<String> previousSelection = globalFilterMap.getOrDefault(column, uniqueValues);

        // 内部类：列表项数据模型,弹窗内部的数据结构
        // 包装类：包含值和选中状态
        class CheckItem {
            final String value;
            final BooleanProperty selected = new SimpleBooleanProperty();

            CheckItem(String v, boolean isSelected) {
                this.value = v;
                this.selected.set(isSelected);
            }

            @Override
            public String toString() {
                return value.isEmpty() ? "" : value;
            }

            public BooleanProperty selectedProperty() {
                return selected;
            }
        }

        // 初始化列表项
        ObservableList<CheckItem> allItems = FXCollections.observableArrayList();
        for (String val : uniqueValues) {
            // 如果历史记录包含该值，或者这是第一次筛选(Map里没这列记录)，则勾选
            boolean isChecked = false;
            if (previousSelection != null) {
                isChecked = previousSelection.contains(val) || !globalFilterMap.containsKey(column);
            }
            allItems.add(new CheckItem(val, isChecked));
        }

        // 3. 构建 UI 组件

        // 3.1 搜索框
        TextField searchBox = new TextField();
        searchBox.setPromptText("搜索...");

        // 3.2 列表视图 (FilteredList 用于搜索过滤)
        FilteredList<CheckItem> viewableItems = new FilteredList<>(allItems, p -> true);
        ListView<CheckItem> listView = new ListView<>(viewableItems);
        listView.setPrefHeight(200);
        listView.setCellFactory(CheckBoxListCell.forListView(CheckItem::selectedProperty));

        // 3.3 全选复选框
        CheckBox selectAllBox = new CheckBox("全选");
        selectAllBox.setPadding(new Insets(5, 0, 5, 5));

        // 点击全选框 -> 改变当前可见列表的状态
        selectAllBox.setOnAction(e -> {
            boolean isSelected = selectAllBox.isSelected();
            // 只操作当前 viewableItems (这就实现了：先搜索 A，点全选，只选中 A 相关的项)
            viewableItems.forEach(item -> item.selected.set(isSelected));
        });

        // 子项变化 -> 更新全选框样式 (选中/不选/半选)
        Runnable updateSelectAllState = () -> {
            // 统计当前可见项的选中情况
            long total = viewableItems.size();
            long selected = viewableItems.stream().filter(i -> i.selected.get()).count();

            if (total == 0) {
                selectAllBox.setSelected(false);
                selectAllBox.setIndeterminate(false);
            } else if (selected == total) {
                selectAllBox.setSelected(true);
                selectAllBox.setIndeterminate(false);
            } else if (selected == 0) {
                selectAllBox.setSelected(false);
                selectAllBox.setIndeterminate(false);
            } else {
                selectAllBox.setIndeterminate(true); // 半选状态 (横杠)
                selectAllBox.setSelected(false);     // 逻辑上视为未全选
            }
        };

        // 为每个 Item 添加监听，同时也监听列表内容变化(搜索时)
        allItems.forEach(item -> item.selected.addListener((o, n, v) -> updateSelectAllState.run()));
        viewableItems.addListener((ListChangeListener<CheckItem>) c -> updateSelectAllState.run());
        // 初始化状态
        updateSelectAllState.run();

        // 3.4 搜索框监听
        searchBox.textProperty().addListener((o, oldVal, newVal) -> {
            String lowerVal = (newVal == null) ? "" : newVal.toLowerCase();
            // 更新视觉过滤 (Predicate)
            viewableItems.setPredicate(item -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                return item.value.toLowerCase().contains(newVal.toLowerCase());
            });
            // 更新选中状态
            // 逻辑：
            // 1. 如果有搜索内容 -> 选中匹配项，取消未匹配项 (实现“未匹配的值取消选中”)
            // 2. 如果清空搜索 -> 恢复全选 (因为空字符串匹配所有值)

            // 注意：这里必须遍历 allItems (全量数据)，因为我们要操作那些被隐藏的项
            allItems.forEach(item -> {
                if (lowerVal.isEmpty()) {
                    // 如果清空了搜索框，默认恢复为“全选”状态，方便用户重新开始
                    item.selected.set(true);
                } else {
                    // 有搜索内容时：匹配的 -> 选中；不匹配的 -> 取消选中
                    boolean isMatch = item.value.toLowerCase().contains(lowerVal);
                    item.selected.set(isMatch);
                }
            });
        });

        // 3.5 底部按钮
        Button btnOk = new Button("确定");
        Button btnCancel = new Button("取消");
        HBox buttons = new HBox(10, btnOk, btnCancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(5));

        VBox root = new VBox(5, searchBox, selectAllBox, listView, buttons);
        root.setPadding(new Insets(10));
        root.setPrefWidth(250);

        // 4. 放入 Popup
        ContextMenu popup = new ContextMenu();
        CustomMenuItem item = new CustomMenuItem(root);
        item.setHideOnClick(false);
        popup.getItems().add(item);

        // 5. 事件处理
        btnCancel.setOnAction(e -> popup.hide());

        btnOk.setOnAction(e -> {
            // 1. 收集最终选中的值 (注意要从 allItems 收集，不能从 viewableItems，因为可能还有被搜索隐藏的选中项)
            List<String> selectedValues = allItems.stream()
                    .filter(i -> i.selected.get())
                    .map(i -> i.value)
                    .collect(Collectors.toList());

            // 2. 更新全局 Map
            // 如果全选了，其实可以从 Map 中移除该列 Key 以提高性能
            globalFilterMap.put(column, selectedValues);

            // 3. 更新 UI 状态 (如果没全选，让漏斗变色)
            if (selectedValues.size() < uniqueValues.size()) {
                owner.setStyle("-fx-background-color: transparent; -fx-text-fill: blue; -fx-font-weight: bold;");
            } else {
                // 如果是全选，恢复黑色，并且也可以考虑从 map 中移除 filter 减少计算
                owner.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
            }

            // 4. 重新计算组合 Predicate
            applyGlobalFilter(filteredData, globalFilterMap);

            popup.hide();
        });

        popup.show(owner, javafx.geometry.Side.BOTTOM, 0, 0);
        return popup;
    }

    /**
     * 辅助方法：获取或初始化存储在 TableView 中的筛选 Map
     */
    private static <S, T> Map<TableColumn<S, T>, List<String>> getGlobalFilterMap(TableView<S> tableView) {
        tableView.getProperties().computeIfAbsent(TABLE_FILTERS_KEY, k -> new HashMap<TableColumn<S, T>, List<String>>());
        return (Map<TableColumn<S, T>, List<String>>) tableView.getProperties().get(TABLE_FILTERS_KEY);
    }

    /**
     * 应用全局筛选逻辑
     * 遍历 Map 中每一列的筛选条件，执行 AND 操作
     */
    private static <S, T> void applyGlobalFilter(FilteredList<S> filteredData, Map<TableColumn<S, T>, List<String>> filterMap) {
        filteredData.setPredicate(item -> {
            // 遍历所有有筛选记录的列
            for (Map.Entry<TableColumn<S, T>, List<String>> entry : filterMap.entrySet()) {
                TableColumn<S, T> col = entry.getKey();
                List<String> allowedValues = entry.getValue();

                // 性能优化：如果该列的允许列表为空，说明全不选，直接隐藏所有行
                if (allowedValues == null || allowedValues.isEmpty()) {
                    return false;
                }

                // 获取当前行、当前列的值
                Object rawVal = col.getCellData(item); // 强转获取数据
                String strVal = (rawVal == null) ? "" : rawVal.toString();

                // 如果当前值不在允许列表中，直接返回 false (一票否决)
                if (!allowedValues.contains(strVal)) {
                    return false;
                }
            }
            // 所有列的校验都通过
            return true;
        });
    }

    // 辅助：处理泛型擦除后的 CellData 获取
    // 由于 Map 里的 Key 是通配符 TableColumn<?,?>，使用时需要一点小技巧
    private static <S, T> Object getCellDataSafely(TableColumn<S, T> col, S item) {
        return col.getCellData(item);
    }

    /**
     * 获取泛型列的不重复值列表
     *
     * @param dataList 数据源 (通常是 filteredList.getSource() 或者 filteredList 本身)
     * @param column   当前的列对象 (TableColumn<S, T>)
     * @param <S>      表格行数据类型 (例如 Person)
     * @param <T>      列数据类型 (例如 String, Integer)
     * @return 该列所有不重复的值
     */
    public static <S, T> List<T> getDistinctValues(List<S> dataList, TableColumn<S, T> column) {
        return dataList.stream()
                // 利用列的 CellValueFactory，无需知道具体实体类方法
                .map(column::getCellData)
                // 过滤空值
                .filter(Objects::nonNull)
                // 去重
                .distinct()
                // 排序 注意：如果 T 没有实现 Comparable，这里会报错，需要额外处理
                .sorted((v1, v2) -> {
                    if (v1 instanceof Comparable) {
                        return ((Comparable) v1).compareTo(v2);
                    }
                    return v1.toString().compareTo(v2.toString());
                })
                // 收集结果
                .collect(Collectors.toList());
    }


    /**
     * 通用搜索逻辑
     *
     * @param searchField
     * @param dataList
     * @param table
     * @return
     */
    public static <E> FilteredList<E> setupSearch(TextField searchField, ObservableList<E> dataList, TableView<E> table) {

        FilteredList<E> filteredData = new FilteredList<>(dataList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(row -> {
                // 1. 如果搜索框为空，显示所有数据
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                // 2. 空行保护
                if (row == null) {
                    return false;
                }

                String lowerVal = newValue.toLowerCase();
                Class<?> clazz = row.getClass();

                // 3. 获取该类所有可搜索的方法 (使用缓存)
                List<Method> methods = SEARCH_METHOD_CACHE.computeIfAbsent(clazz, JfxTableFilterUtils::findSearchableMethods);

                // 4. 遍历方法获取值进行比对
                for (Method method : methods) {
                    try {
                        // 调用方法获取值
                        Object result = method.invoke(row);

                        if (result == null) {
                            continue;
                        }

                        String valueStr;
                        // 关键修正：如果返回的是 Property 对象，需要解包取 getValue()
                        if (result instanceof ObservableValue) {
                            Object innerVal = ((ObservableValue<?>) result).getValue();
                            if (innerVal == null) {
                                continue;
                            }
                            valueStr = String.valueOf(innerVal);
                        } else {
                            // 如果是普通 Getter 返回的值
                            valueStr = String.valueOf(result);
                        }

                        // 包含匹配 (忽略大小写)
                        if (valueStr.toLowerCase().contains(lowerVal)) {
                            return true;
                        }

                    } catch (Exception ignored) {
                        // 忽略反射异常，继续检查下一个字段
                    }
                }
                return false;
            });
        });
        // 包装为 SortedList (支持点击表头排序)
        SortedList<E> sortedData = new SortedList<>(filteredData);
        // 将 SortedList 的比较器绑定到 TableView 的比较器
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        // 绑定到表格
        table.setItems(sortedData);

        return filteredData;
    }

    /**
     * 反射分析类，找到所有 property 方法或 getter 方法
     *
     * @param clazz
     * @return
     */
    public static List<Method> findSearchableMethods(Class<?> clazz) {
        List<Method> searchableMethods = new ArrayList<>();

        // 获取所有声明的字段
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            // 策略A：尝试找 xxxProperty() 方法 (优先，符合 JavaFX 规范)
            try {
                String propName = field.getName() + "Property";
                Method method = clazz.getMethod(propName);
                searchableMethods.add(method);
                continue; // 如果找到了 property 方法，就不用找 getter 了
            } catch (NoSuchMethodException ignored) {
            }

            // 策略B：尝试找 getXxx() 方法 (备选，兼容普通 POJO)
            try {
                String getterName = "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
                Method method = clazz.getMethod(getterName);
                // 排除无返回值或带参数的方法
                if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
                    searchableMethods.add(method);
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return searchableMethods;
    }
}
