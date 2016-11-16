package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpossibleCourseModuleAbstractUnits extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private TreeView<String> treeViewCourseModuleAbstractUnits;

  @Inject
  public ImpossibleCourseModuleAbstractUnits(final Inflater inflater) {
    inflater.inflate("/components/reports/ImpossibleCourseModuleAbstractUnits",
        this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    treeViewCourseModuleAbstractUnits.setRoot(new TreeItem<>());
  }

  /**
   * Set data for this component.
   * @param courseModuleAbstractUnit data
   */
  public void setData(final Map<Course, Map<Module, Set<AbstractUnit>>> courseModuleAbstractUnit) {
    treeViewCourseModuleAbstractUnits.getRoot().getChildren().setAll(
        courseModuleAbstractUnit.entrySet().stream().map(courseMapEntry -> {
          TreeItem<String> courseItem = new TreeItem<>(courseMapEntry.getKey().getFullName());
          courseItem.getChildren().setAll(
              courseMapEntry.getValue().entrySet().stream().map(moduleSetEntry -> {
                TreeItem<String> moduleItem = new TreeItem<>(moduleSetEntry.getKey().getTitle());
                moduleItem.getChildren().setAll(
                    moduleSetEntry.getValue().stream().map(abstractUnit ->
                    new TreeItem<>(abstractUnit.getTitle())).collect(Collectors.toSet()));
                return moduleItem;
              }).collect(Collectors.toSet()));
          return courseItem;
        }).collect(Collectors.toSet()));
  }
}
