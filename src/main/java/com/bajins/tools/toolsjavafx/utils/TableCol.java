package com.bajins.tools.toolsjavafx.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author bajin
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TableCol {

    // --- 1. 基础属性 ---

    /** 表头文字 (text) */
    String value();

    /** CSS ID (id)，用于高级 CSS 选择器 */
    String id() default "";

    /** CSS 类名 (styleClass)，用于引用外部 .css 文件中的样式 */
    String styleClass() default "";

    /** 内联 CSS 样式 (style)，例如 "-fx-alignment: CENTER;" -fx-font-weight: bold; -fx-text-fill: blue; */
    String style() default "";

    // --- 2. 尺寸属性 (Width) ---

    /** 首选宽度 (prefWidth) */
    double width() default 100.0;

    /** 最小宽度 (minWidth) */
    double minWidth() default 10.0;

    /** 最大宽度 (maxWidth) */
    double maxWidth() default 5000.0; // JavaFX 默认最大值

    // --- 3. 行为开关 (Boolean Flags) ---

    /** 是否可见 (visible) */
    boolean visible() default true;

    /** 是否允许调整大小 (resizable) */
    boolean resizable() default true;

    /** 是否允许排序 (sortable) */
    boolean sortable() default true;

    /** 是否允许拖拽改变列顺序 (reorderable) */
    boolean reorderable() default true;

    /** 是否可编辑 (editable) - 注意：表格本身也要设为 editable */
    boolean editable() default false;

    // --- 4. 快捷对齐方式 (Sugar) ---

    /**
     * 快捷对齐方式，会覆盖 style 中的 -fx-alignment。
     * 可选值: CENTER, CENTER-LEFT, CENTER-RIGHT, BASELINE-LEFT 等
     * 默认为空，不处理
     */
    String alignment() default "";

    /** 是否按数字格式排序 (默认 false) */
    boolean numeric() default false;

    /** 是否默认排序 -1 不排序，0正序排序，1逆序排序，实际使用枚举TableColumn.SortType */
    int defaultSort() default -1;
}