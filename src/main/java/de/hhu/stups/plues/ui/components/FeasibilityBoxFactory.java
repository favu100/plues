package de.hhu.stups.plues.ui.components;

import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;

import java.util.Set;

import javafx.scene.layout.VBox;

@FunctionalInterface
public interface FeasibilityBoxFactory {
  FeasibilityBox create(@Assisted("major") Course major, @Assisted("minor") Course minor,
                        @Assisted("impossibleCourses") Set<String> impossibleCourses,
                        @Assisted("parent") VBox parent);
}