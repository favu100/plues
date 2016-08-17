package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;

import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.Timetable;
import de.hhu.stups.plues.ui.components.CourseFilter;
import de.hhu.stups.plues.ui.controller.Musterstudienplaene;

class ComponentsModule extends AbstractModule {
  @Override
  public void configure() {
    bind(CourseFilter.class);
    bind(Musterstudienplaene.class);
    bind(MajorMinorCourseSelection.class);
    bind(Timetable.class);
  }
}