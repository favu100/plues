package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.hhu.stups.plues.prob.Solver;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;


public class SolverTask<T> extends Task<T> {

  private static final ExecutorService EXECUTOR;
  private static final ScheduledExecutorService TIMER;

  static {
    final ThreadFactory threadFactoryBuilder
      = new ThreadFactoryBuilder().setDaemon(true)
      .setNameFormat("solver-task-runner-%d")
      .build();
    EXECUTOR = Executors.newSingleThreadExecutor(threadFactoryBuilder);
    TIMER = Executors.newSingleThreadScheduledExecutor(threadFactoryBuilder);

  }

  private final Callable<T> function;
  private final Solver solver;
  private Future<T> future;
  private ScheduledFuture<?> timer;

  SolverTask(final String title, final String message, final Solver solver,
             final Callable<T> func) {
    this(title, solver, func);

    updateMessage(message);
  }

  private SolverTask(final String title, final Solver solver, final Callable<T> func) {
    this.function = func;
    this.solver = solver;

    updateTitle(title);
  }

  @Override
  protected T call() throws InterruptedException, ExecutionException {
    final int solverTaskTimeout = 5;    // minutes

    updateTitle("Solver task");
    updateProgress(10, 100);
    future = EXECUTOR.submit(function);
    timer = TIMER.schedule(this::timeOut, solverTaskTimeout, TimeUnit.MINUTES);

    int percentage = 10;
    while (!future.isDone()) {
      percentage = (percentage + 2) % 95;
      updateProgress(percentage, 100);
      if (this.isCancelled()) {
        return null;
      }
      if (future.isCancelled()) {
        updateMessage("ProB exited");
        this.cancel();
        return null;
      }
      
      try {
        TimeUnit.MILLISECONDS.sleep(100);
      } catch (final InterruptedException interrupted) {
        if (isCancelled()) {
          break;
        }
      }
    }
    updateProgress(100, 100);

    return future.get();
  }

  private void timeOut() {

    System.out.println("Task timeout.");
    updateMessage("Task timeout");

    this.cancel();
  }

  @Override
  protected void cancelled() {
    super.cancelled();

    System.out.println("Task cancelled.");
    updateMessage("Task cancelled");

    timer.cancel(true);
    future.cancel(true);

    solver.interrupt();
  }

  @Override
  protected void succeeded() {
    super.succeeded();

    timer.cancel(true);

    updateMessage("Done!");
    final T i = this.getValue();
    System.out.println("Result: " + i.toString());
  }
}
