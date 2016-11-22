package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.ResultBox;
import de.hhu.stups.plues.ui.components.ResultBoxFactory;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class Musterstudienplaene extends GridPane implements Initializable {

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
  private ProgressBar progressGenerate;
  @FXML
  @SuppressWarnings("unused")
  private VBox resultBox;
  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPane;

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

    this.setVgap(10.0);

    inflater.inflate("musterstudienplaene", this, this, "musterstudienplaene");
  }

  /**
   * Function to handle generation of resultbox containing result for chosen major and minor.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btGeneratePressed() {
    final Course selectedMajorCourse = courseSelection.getSelectedMajor();
    final Course selectedMinorCourse = courseSelection.getSelectedMinor();

    final ResultBox rb
        = resultBoxFactory.create(selectedMajorCourse, selectedMinorCourse, resultBox);

    resultBox.getChildren().add(0, rb);
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    btGenerate.setDefaultButton(true);
    btGenerate.disableProperty().bind(solverProperty.not());

    final IntegerBinding resultBoxChildren = Bindings.size(resultBox.getChildren());
    scrollPane.visibleProperty().bind(resultBoxChildren.greaterThan(0));

    resultBox.setSpacing(10.0);
    resultBox.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));

    delayedStore.whenAvailable(store ->
        PdfRenderingHelper.initializeCourseSelection(store, uiDataService, courseSelection));

    delayedSolverService.whenAvailable(s -> this.solverProperty.set(true));
  }
}
