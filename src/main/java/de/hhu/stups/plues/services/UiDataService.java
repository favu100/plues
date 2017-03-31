package de.hhu.stups.plues.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.timetable.SessionDisplayFormat;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Stupid data container that can be injected and used by all UI components to store global data.
 */

@Singleton
public class UiDataService {
  private final ListProperty<Integer> conflictMarkedSessionsProperty
      = new SimpleListProperty<>(FXCollections.emptyObservableList());

  private final ObjectProperty<SessionDisplayFormat> sessionDisplayFormatProperty
      = new SimpleObjectProperty<>(SessionDisplayFormat.UNIT_KEY);

  private final SetProperty<Course> impossibleCoursesProperty
      = new SimpleSetProperty<>(FXCollections.observableSet());

  private final ObjectProperty<Date> lastSavedDate
      = new SimpleObjectProperty<>(new Date(ManagementFactory.getRuntimeMXBean().getStartTime()));

  private final ExecutorService executorService;

  /**
   * Service to provide properties for UI relevant data.
   *
   * @param solverServiceDelayed Delayed SolverService
   * @param delayedStore Delayed Store
   * @param executorService ExecutorService
   */
  @Inject
  public UiDataService(final Delayed<SolverService> solverServiceDelayed,
                       final Delayed<Store> delayedStore,
                       final ExecutorService executorService) {
    this.executorService = executorService;
    delayedStore.whenAvailable(store ->
        solverServiceDelayed.whenAvailable(solverService ->
            this.loadImpossibleCourses(solverService, store)));
  }

  public Date getLastSavedDate() {
    return lastSavedDate.get();
  }

  public void setLastSavedDate(final Date lastSavedDate) {
    this.lastSavedDate.set(lastSavedDate);
  }

  public ObjectProperty<Date> lastSavedDateProperty() {
    return lastSavedDate;
  }

  public ObservableSet<Course> getImpossibleCourses() {
    return this.impossibleCoursesProperty.get();
  }

  public SetProperty<Course> impossibleCoursesProperty() {
    return this.impossibleCoursesProperty;
  }

  private void setImpossibleCourses(final ObservableSet<Course> courses) {
    this.impossibleCoursesProperty.set(courses);
  }


  private void loadImpossibleCourses(final SolverService solverService, final Store store) {
    final SolverTask<Set<String>> t = solverService.impossibleCoursesTask();
    executorService.submit(t);
    t.setOnSucceeded(event -> this.setImpossibleCourses(t.getValue().stream()
        .map(store::getCourseByKey)
        .collect(Collectors.collectingAndThen(Collectors.toSet(), FXCollections::observableSet))));
  }

  public SessionDisplayFormat getSessionDisplayFormat() {
    return sessionDisplayFormatProperty.get();
  }

  public void setSessionDisplayFormatProperty(
        final SessionDisplayFormat sessionDisplayFormatProperty) {
    this.sessionDisplayFormatProperty.set(sessionDisplayFormatProperty);
  }

  public ObjectProperty<SessionDisplayFormat> sessionDisplayFormatProperty() {
    return sessionDisplayFormatProperty;
  }

  public ObservableList<Integer> getConflictMarkedSessions() {
    return conflictMarkedSessionsProperty.get();
  }

  public void setConflictMarkedSessions(final ObservableList<Integer> conflictMarkedSessions) {
    this.conflictMarkedSessionsProperty.set(conflictMarkedSessions);
  }

  public ListProperty<Integer> conflictMarkedSessionsProperty() {
    return conflictMarkedSessionsProperty;
  }

  private ObservableSet<Course> getImpossibleCoursesProperty() {
    return impossibleCoursesProperty.get();
  }

  public SetProperty<Course> impossibleCoursesPropertyProperty() {
    return impossibleCoursesProperty;
  }
}
