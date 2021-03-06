package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.studienplaene.Renderer;
import de.hhu.stups.plues.ui.components.PdfGenerationSettings;
import de.hhu.stups.plues.ui.exceptions.RenderingException;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public class PdfRenderingTask extends Task<Path> {

  private final Delayed<Store> delayedStore;
  private final Course major;
  private final Course minor;
  private final SolverTask<FeasibilityResult> solverTask;
  private final ReadOnlyObjectProperty<PdfGenerationSettings> pdfGenerationSettingsProperty;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ResourceBundle resources;
  private static final ListeningExecutorService EXECUTOR_SERVICE;

  static {
    final ThreadFactory threadFactoryBuilder
        = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("pdfrendering-task-runner-%d").build();

    EXECUTOR_SERVICE = MoreExecutors.listeningDecorator(
        Executors.newSingleThreadExecutor(threadFactoryBuilder));
  }


  /**
   * Create a task for rendering a pdf.
   *
   * @param delayedStore Store containing necessary data
   * @param major        Course major or integrated course
   * @param minor        Course minor course, can be null
   */
  @Inject
  protected PdfRenderingTask(final Delayed<Store> delayedStore,
                             @Assisted("major") final Course major,
                             @Assisted("minor") @Nullable final Course minor,
                             @Assisted final SolverTask<FeasibilityResult> solverTask,
                             @Assisted final ReadOnlyObjectProperty<PdfGenerationSettings>
                                 pdfGenerationSettingsProperty) {
    this.delayedStore = delayedStore;
    this.resources = ResourceBundle.getBundle("lang.tasks");
    this.major = major;
    this.minor = minor;
    this.solverTask = solverTask;
    this.pdfGenerationSettingsProperty = pdfGenerationSettingsProperty;

    updateTitle(this.buildTitle());
    updateProgress(0, 100);
    updateMessage(resources.getString("waitingForExecution"));
  }

  @Override
  protected Path call() throws Exception {

    updateMessage(resources.getString("submit"));
    updateProgress(20, 100);

    if (this.isCancelled()) {
      return null;
    }

    updateMessage(resources.getString("waitingForExecution"));
    solverTask.setOnRunning(event -> this.updateMessage(resources.getString("running")));
    //noinspection ResultOfMethodCallIgnored
    EXECUTOR_SERVICE.submit(solverTask);

    updateProgress(40, -1);

    runTask();

    if (this.isCancelled() || solverTask.isCancelled()) {
      this.cancel();
      return null;
    }

    // we have to read from the task here, the future does not provide a result.
    final FeasibilityResult result = solverTask.get();

    return renderPdf(result);
  }

  private String buildTitle() {
    String names = "";
    if (this.major != null) {
      names += major.getKey();
    }
    if (this.minor != null) {
      names += ", " + minor.getKey();
    }
    return String.format(resources.getString("rendering"), names);
  }

  private void runTask() throws InterruptedException {
    while (!solverTask.isDone()) {
      if (solverTask.isCancelled() || this.isCancelled()) {
        updateMessage(resources.getString("cancelled"));
        break;
      }

      try {
        TimeUnit.MILLISECONDS.sleep(200);
      } catch (final InterruptedException exception) {

        logger.info("Task interrupted during sleep", exception);

        if (solverTask.isCancelled() || this.isCancelled()) {
          throw exception;
        }
      }
    }
  }

  private Path renderPdf(final FeasibilityResult result) throws RenderingException {
    final Store store = delayedStore.get();

    updateMessage(resources.getString("render"));
    updateProgress(60, 100);

    final Renderer renderer = getRenderer(store, result);

    updateProgress(80, 100);

    final File tmp = getTempFile(renderer);

    updateMessage(resources.getString("finished"));
    updateProgress(100, 100);

    return Paths.get(tmp.getAbsolutePath());
  }

  private Renderer getRenderer(final Store store, final FeasibilityResult result) {
    try {
      return new Renderer(store, result, major, minor, pdfGenerationSettingsProperty.get());
    } catch (final NullPointerException exc) {
      logger.error("Exception rendering PDF", exc);
      throw exc;
    }
  }

  @Override
  protected void cancelled() {
    super.cancelled();
    if (solverTask != null) {
      Platform.runLater(() -> solverTask.cancel(true));
    }
  }

  @Override
  protected void failed() {
    super.failed();
    if (solverTask != null && solverTask.isRunning()) {
      solverTask.cancel(true);
    }
    logger.error("Exception in Task", this.getException());
  }

  /**
   * Helper method to get temporary file.
   *
   * @param renderer Renderer object to create file
   */
  private File getTempFile(final Renderer renderer) throws RenderingException {

    final File temp;
    try {
      temp = File.createTempFile("timetable", ".pdf");
      temp.deleteOnExit();
    } catch (final IOException exc) {
      logger.error("IOException creating temp file", exc);
      throw new RenderingException("IOException creating temp file", exc);
    }

    try (final OutputStream out = new FileOutputStream(temp)) {
      renderer.getResult().writeTo(out);
    } catch (final RenderingException exc) {
      logger.error("RenderingException rendering PDF", exc.getCause());
      throw exc;
    } catch (final IOException exc) {
      final RenderingException renderingException
          = new RenderingException("IOException rendering PDF", exc);
      logger.error("IOException rendering PDF", renderingException);
      throw renderingException;
    }
    return temp;
  }

  public Course getMinor() {
    return minor;
  }

  public Course getMajor() {
    return major;
  }

}
