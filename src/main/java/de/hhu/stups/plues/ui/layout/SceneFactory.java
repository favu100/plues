package de.hhu.stups.plues.ui.layout;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class SceneFactory {

  /**
   * mach ich gleich.
   */
  public static Scene create(final Parent component) {
    final  Scene scene = new Scene(component);
    scene.getStylesheets().add("/styles/index.css");

    return scene;
  }
}