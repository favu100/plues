package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class Timetable extends BorderPane implements Initializable {

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());
  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final ObjectProperty<Course>
      courseProperty = new SimpleObjectProperty<>();
  private final BooleanProperty
      solverProperty = new SimpleBooleanProperty(false);

  @FXML
  private Label selection;
  @FXML
  private Button checkSelection;
  @FXML
  private Label result;
  @FXML
  private CourseFilter courseFilter;

  @FXML
  private GridPane timeTable;

  private SolverService solverService;


  /**
   * Timetable component.
   */
  @Inject
  public Timetable(final Inflater inflater, final Delayed<Store> delayedStore,
                          final Delayed<SolverService> delayedSolverService) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;

    // TODO: remove controller param if possible
    // TODO: currently not possible because of dependency circle
    inflater.inflate("components/Timetable", this, this);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.delayedStore.whenAvailable(s -> {
      Runtime.getRuntime().addShutdownHook(new Thread(s::close));
      this.courseFilter.setCourses(s.getCourses());
    });

    this.courseProperty.bind(this.courseFilter.selectedItemProperty());
    this.selection.textProperty().bind(
        Bindings.selectString(this.courseProperty, "name"));

    this.checkSelection.setDefaultButton(true);
    this.checkSelection.disableProperty().bind(
        this.courseProperty.isNull().or(this.solverProperty.not()));

    this.delayedSolverService.whenAvailable(s -> {
      this.solverService = s;
      this.solverProperty.set(true);
    });

    initSessionBoxes();
  }

  private void initSessionBoxes() {
    final int offX = 1, offY = 1, widthX = 5;

    IntStream.range(0, 35).forEach(i -> {
      ListView view = new ListView();
      timeTable.add(view, i % widthX + offX, (i / widthX) + offY);
    });
  }

  @FXML
  @SuppressWarnings({"UnusedParameters", "unused"})
  private void checkButtonPressed(final ActionEvent actionEvent) {
    final Course course = this.courseProperty.get();

    final SolverService s = this.solverService;
    assert s != null;

    final SolverTask<Boolean> t = s.checkFeasibilityTask(course);
    t.setOnSucceeded(event -> {
      final Boolean i = (Boolean) event.getSource().getValue();
      this.result.setText(i.toString());
      logger.info(course.getName() + ": " + i.toString());
    });
    s.submit(t);
  }
}
