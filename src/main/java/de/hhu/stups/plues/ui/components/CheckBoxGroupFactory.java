package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;

@FunctionalInterface
public interface CheckBoxGroupFactory {
  CheckBoxGroup create(Course course, Module module);
}
