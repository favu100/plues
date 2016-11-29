package de.hhu.stups.plues.ui.components.conflictmatrix;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;

public class CourseGridCell extends Pane {

  private static final String VERTICAL = "vertical";

  private final String courseKey;
  private final String courseName;
  private final String orientation;

  /**
   * Create a grid cell representing a course name.
   *
   * @param orientation The orientation of the label, default is horizontal.
   */
  public CourseGridCell(final String courseKey, final String courseName, final String orientation) {
    this.courseKey = courseKey;
    this.courseName = courseName;
    this.orientation = orientation;
    Platform.runLater(this::initializeGridCell);
  }

  @SuppressWarnings("unused")
  private void initializeGridCell() {
    setId("conflictMatrixCellDefault");

    final Label label = new Label("  " + courseKey + "  ");
    if (VERTICAL.equals(orientation)) {
      label.setRotate(270.0);
      label.setTranslateY(100.0);
      label.setTranslateX(-70.0);
      label.setPrefWidth(200.0);
    } else {
      setPrefHeight(25.0);
    }
    final Tooltip tooltip = new Tooltip(courseName);
    label.setTooltip(tooltip);
    getChildren().add(new Group(label));
  }

}
