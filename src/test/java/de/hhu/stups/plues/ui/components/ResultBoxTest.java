package de.hhu.stups.plues.ui.components;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.UiTestHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public abstract class ResultBoxTest extends ApplicationTest {
  private final Course major;
  private final Course minor;
  private Text icon;
  private PdfRenderingTask task;

  /**
   * Default constructor.
   */
  @SuppressWarnings("WeakerAccess")
  public ResultBoxTest() {
    this.major = new Course();
    major.setLongName("Major Course");
    major.setDegree("MA");
    major.setKzfa("H");
    major.setPo(2016);

    this.minor = new Course();
    minor.setLongName("Minor Course");
    minor.setDegree("MI");
    minor.setKzfa("N");
    minor.setPo(2013);
  }

  @Before
  public void sleepBeforeTests() {
    this.sleep(500, TimeUnit.MILLISECONDS); // sleep briefly for Task to finish
  }

  @Test
  public void majorLabel() {
    verifyThat("#lbMajor", LabeledMatchers.hasText(major.getFullName()));
  }

  @Test
  public void minorLabel() {
    verifyThat("#lbMinor", LabeledMatchers.hasText(minor.getFullName()));
  }

  @Test
  public void testIcon() {
    final Text mark = this.icon;

    final TaskProgressIndicator taskProgressIndicator = lookup("#taskProgressIndicator").query();
    final Label icon = taskProgressIndicator.getTaskStateIcon();
    final Text graphic = (Text) icon.getGraphic();
    Assert.assertEquals(mark.getText(), graphic.getText());
  }

  @After
  public void cleanup() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();
    setupStage(Stage::close);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void start(final Stage stage) throws Exception {
    final SolverService solverService = mock(SolverService.class);
    when(solverService.computeFeasibilityTask(any())).thenReturn(mock(SolverTask.class));

    final FXMLLoader loader = new FXMLLoader();
    loader.setBuilderFactory(type -> {
      if (type.equals(TaskProgressIndicator.class)) {
        return () -> new TaskProgressIndicator(new Inflater(new FXMLLoader()));
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final Delayed<SolverService> solver = new Delayed<>();
    final Inflater inflater = new Inflater(loader);

    solver.set(solverService);

    final ResultBox resultBox = new ResultBox(
        inflater, new Router(), solver, (major1, minor1, solverTask, colorScheme) -> task,
        Executors.newSingleThreadExecutor(), major, minor, new ListView<>(),
        new SimpleObjectProperty<>(UiTestHelper.getColorScheme()));

    final Scene scene = new Scene(resultBox, 200, 200);
    stage.setScene(scene);
    stage.show();
  }

  void setIcon(final Text icon) {
    this.icon = icon;
  }

  void setTask(final PdfRenderingTask task) {
    this.task = task;
  }
}
