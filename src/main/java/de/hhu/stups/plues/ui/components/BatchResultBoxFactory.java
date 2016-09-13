package de.hhu.stups.plues.ui.components;

import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;

import java.nio.file.Path;
import java.util.Set;

public interface BatchResultBoxFactory {
  BatchResultBox create(@Assisted("major") Course major, @Assisted("minor") Course minor,
                        Path tempDirectoryPath, Set<PdfRenderingTask> taskPool);
}