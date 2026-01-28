# tools-javafx
GUI toolkit built with JavaFX.

```
ObservableValue<T>（只读可观察值）    WritableValue<T>（可写值）
               ↑                    ↑
                \                  /
                 \                /
                  \              /
                     Property<T>
                         ↑
      ┌──────────────────┴──────────────────┐
        │
        ├─ BooleanProperty ─┬─ SimpleBooleanProperty (标准实现类)
        │                   ├─ BooleanPropertyBase (抽象基类)
        │                   ├─ JavaBeanBooleanProperty (适配传统 JavaBean)
        │                   ├─ StyleableBooleanProperty (CSS 样式支持)
        │                   ├─ SimpleStyleableBooleanProperty
        │                   └─ ReadOnlyBooleanWrapper (包装器)
        │
        ├─ IntegerProperty （实现类命名规则基本与上面类似）
        │
        ├─ LongProperty
        │
        ├─ DoubleProperty
        │
        ├─ FloatProperty
        │
        ├─ StringProperty
        │
        ├─ ObjectProperty<T>
        │
        ├─ ListProperty<E>
        │
        ├─ SetProperty<E>
        │
        ├─ MapProperty<K,V>
        │
        └─ MapProperty<K,V>
```