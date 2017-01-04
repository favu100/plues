package de.hhu.stups.plues.ui;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import org.hibernate.annotations.common.util.impl.LoggerFactory;
import org.jboss.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourceManager {
  private final Delayed<Store> delayedStore;
  private final ExecutorService executorService;

  private final Logger logger = LoggerFactory.logger(getClass());

  /**
   * ResourceManager class used to manage resources that need to be closed when shutting down the
   * application.
   *
   * @param delayedStore    Delayed store
   * @param executorService ExecutorService
   */
  @Inject
  public ResourceManager(final Delayed<Store> delayedStore,
                         final ListeningExecutorService executorService) {
    this.delayedStore = delayedStore;
    this.executorService = executorService;
  }

  /**
   * Close all managed resources.
   *
   * @throws InterruptedException thrown if any of the executors throws it.
   */
  public void close() throws InterruptedException {
    delayedStore.whenAvailable(Store::close);
    logger.info("Store closed");

    this.executorService.shutdown();
    logger.info("shutdown");

    this.executorService.awaitTermination(10, TimeUnit.SECONDS);
    logger.info("waited for termination");

    this.executorService.shutdownNow();
    logger.info("killed");
  }
}
