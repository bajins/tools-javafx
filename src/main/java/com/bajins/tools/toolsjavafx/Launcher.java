package com.bajins.tools.toolsjavafx;

import javafx.application.Application;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;

/**
 * <pre>
 * --add-opens javafx.controls/javafx.scene.control.skin=org.controlsfx.controls
 * --add-exports javafx.controls/javafx.scene.control.skin=org.controlsfx.controls
 * --add-opens javafx.graphics/javafx.scene=org.controlsfx.controls
 * --add-exports javafx.graphics/javafx.scene=org.controlsfx.controls
 * --enable-native-access=javafx.graphics
 * </pre>
 * @author bajin
 */
public class Launcher {
    public static void main(String[] args) {
        /*System.setProperty("java.system.class.loader", "jdk.internal.loader.ClassLoaders$AppClassLoader");
        // Add the VM argument programmatically
        ModuleLayer.Controller controller = ModuleLayer.boot().defineModulesWithOneLoader(
                ModuleFinder.of(Path.of("/path/to/controlsfx.jar")),
                ClassLoader.getSystemClassLoader()
        );*/
        Application.launch(ToolsjavafxApplication.class, args);
    }
}
