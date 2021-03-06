package de.hhu.stups.plues.tasks;

import de.hhu.stups.plues.data.Store;

@FunctionalInterface
public interface SolverLoaderTaskFactory {
  SolverLoaderTask create(Store store);
}
