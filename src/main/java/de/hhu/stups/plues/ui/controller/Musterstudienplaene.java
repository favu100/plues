package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.ResultBox;
import de.hhu.stups.plues.ui.components.ResultBoxFactory;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class Musterstudienplaene extends GridPane implements Initializable, Activatable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final ResultBoxFactory resultBoxFactory;
  private final UiDataService uiDataService;

  @FXML
  @SuppressWarnings("unused")
  private MajorMinorCourseSelection courseSelection;
  @FXML
  @SuppressWarnings("unused")
  private Button btGenerate;
  @FXML
  @SuppressWarnings("unused")
  private ListView<ResultBox> resultBoxWrapper;

  /**
   * This view presents a selection of major and minor courses where the user can choose a
   * combination of courses or a standalone course from. The specific pdf file can be generated and
   * success or failure is displayed in a list of {@link ResultBox result boxes}. The user is able
   * to view or save the pdf file or remove the result box from the list.
   *
   * @param inflater             Inflater to handle fxml loading
   * @param delayedStore         Store containing relevant data
   * @param delayedSolverService SolverService for usage of ProB solver
   * @param resultBoxFactory     Factory to create ResultBox entities
   */
  @Inject
  public Musterstudienplaene(final Inflater inflater,
                             final Delayed<Store> delayedStore,
                             final Delayed<SolverService> delayedSolverService,
                             final UiDataService uiDataService,
                             final ResultBoxFactory resultBoxFactory) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.resultBoxFactory = resultBoxFactory;
    this.uiDataService = uiDataService;

    this.solverProperty = new SimpleBooleanProperty(false);

    inflater.inflate("Musterstudienplaene", this, this, "musterstudienplaene");
  }

  /**
   * Function to handle generation of resultbox containing result for chosen major and minor.
   */
  @FXML
  @SuppressWarnings( {"unused,WeakerAccess"})
  public void btGeneratePressed() {
    final Course selectedMajorCourse = courseSelection.getSelectedMajor();
    final Course selectedMinorCourse = courseSelection.getSelectedMinor();

    final ResultBox rb
        = resultBoxFactory.create(selectedMajorCourse, selectedMinorCourse, resultBoxWrapper);

    resultBoxWrapper.getItems().add(0, rb);
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    btGenerate.disableProperty().bind(solverProperty.not());

    resultBoxWrapper.visibleProperty().bind(Bindings.isEmpty(resultBoxWrapper.getItems()).not());

    // disable list-view selection
    resultBoxWrapper.getSelectionModel().selectedIndexProperty().addListener(
        (observable, oldValue, newValue) ->
            Platform.runLater(() -> resultBoxWrapper.getSelectionModel().select(-1)));

    delayedStore.whenAvailable(store -> {
      courseSelection.setMajorCourseList(FXCollections.observableList(store.getMajors()));
      courseSelection.setMinorCourseList(FXCollections.observableList(store.getMinors()));
    });

    courseSelection.impossibleCoursesProperty().bind(uiDataService.impossibleCoursesProperty());

    delayedSolverService.whenAvailable(s -> this.solverProperty.set(true));
  }

  /**
   * Select the given courses within the {@link #courseSelection} when the user navigates to the
   * view via the {@link de.hhu.stups.plues.routes.ControllerRoute}.
   */
  @Override
  public void activateController(final RouteNames routeName, final Object... courses) {
    if (courses.length > 0) {
      courseSelection.selectCourse((Course) courses[0]);
    }
    if (courses.length > 1) {
      courseSelection.selectCourse((Course) courses[1]);
    }
    if (courses.length == 2) {
      btGeneratePressed();
    }
  }
}
