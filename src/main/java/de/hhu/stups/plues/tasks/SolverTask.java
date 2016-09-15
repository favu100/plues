package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.hhu.stups.plues.prob.Solver;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Nullable;


public class SolverTask<T> extends Task<T> {

  private static final ListeningExecutorService EXECUTOR;
  private static final ListeningScheduledExecutorService TIMER;

  static {
    final ThreadFactory threadFactoryBuilder
        = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("solver-task-runner-%d").build();

    EXECUTOR = MoreExecutors.listeningDecorator(
      Executors.newSingleThreadExecutor(threadFactoryBuilder));
    TIMER = MoreExecutors.listeningDecorator(
      Executors.newSingleThreadScheduledExecutor(threadFactoryBuilder));

  }

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());
  private final Callable<T> function;
  private final Solver solver;
  private final TimeUnit timeUnit;
  private final int timeout;
  private ListenableFuture<T> future;
  private ListenableScheduledFuture<?> timer;
  private String reason = "Task cancelled";

  SolverTask(final String title, final String message, final Solver solver,
             final Callable<T> func) {
    this(title, message, solver, func, 30, TimeUnit.SECONDS);
  }

  SolverTask(final String title, final String message, final Solver solver, final Callable<T> func,
      final int timeout, final TimeUnit timeUnit) {
    this.function = timedCallableWrapper(title, func);
    this.solver = solver;
    this.timeout = timeout;
    this.timeUnit = timeUnit;

    updateTitle(title);
    updateMessage(message);
  }

  private Callable<T> timedCallableWrapper(final String title, final Callable<T> func) {
    return () -> {
      final long start = System.nanoTime();
      final T result = func.call();
      final long end = System.nanoTime();
      logger.info("SolverTask " + title + " finished in "
          + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms.");
      return result;
    };
  }

  @Override
  protected T call() throws InterruptedException, ExecutionException {

    updateProgress(10, 100);
    timer = TIMER.schedule(this::timeOut, this.timeout, this.timeUnit);
    future = EXECUTOR.submit(function);

    Futures.addCallback(future, new FutureCallback<T>() {
      @Override
      public void onSuccess(@Nullable final T result) {
        timer.cancel(true);
      }

      @Override
      public void onFailure(final Throwable throwable) {
        timer.cancel(true);
      }
    });

    int percentage = 10;
    while (!future.isDone()) {
      percentage = (percentage + 2) % 95;
      updateProgress(percentage, 100);
      if (this.isCancelled()) {
        logger.info("cancelled");
        return null;
      }
      if (future.isCancelled()) {
        logger.info("future cancelled");
        updateMessage("ProB exited");
        this.cancel();
        return null;
      }

      try {
        TimeUnit.MILLISECONDS.sleep(100);
      } catch (final InterruptedException interrupted) {
        if (isCancelled()) {
          logger.info("Task cancelled while sleeping " + this.toString());
          return null;
        }
      }
    }
    updateProgress(100, 100);

    return future.get();
  }

  private void timeOut() {
    logger.info("Timeout");
    this.reason = "Task timeout";
    this.cancel();
  }

  @Override
  protected void cancelled() {
    logger.info("Cancelled handler");
    super.cancelled();

    logger.info(this.reason);
    updateMessage(this.reason);

    if (timer != null) {
      timer.cancel(true);
    }
    if (future != null) {
      future.cancel(true);
    }

    solver.interrupt();
  }

  @Override
  protected void succeeded() {
    logger.info("succeeded handler");
    super.succeeded();

    updateMessage("Done!");
    final T i = this.getValue();
    logger.info("Result: " + i.toString());
  }

  @Override
  protected void failed() {
    logger.info("failed handler");
    updateMessage("failed");

    if (timer != null) {
      timer.cancel(true);
    }
    if (future != null) {
      future.cancel(true);
    }

    solver.interrupt();

  }
}
