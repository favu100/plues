package de.hhu.stups.plues.ui.components.detailview;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

public class SessionDetailView extends VBox implements Initializable {

  private final ObjectProperty<Session> sessionProperty;
  private final ObjectProperty<SessionFacade> sessionFacadeProperty;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private Label session;
  @FXML
  @SuppressWarnings("unused")
  private Label title;
  @FXML
  @SuppressWarnings("unused")
  private Label group;
  @FXML
  @SuppressWarnings("unused")
  private Label semesters;
  @FXML
  @SuppressWarnings("unused")
  private Label tentative;
  @FXML
  @SuppressWarnings("unused")
  private TableView<CourseTableEntry> courseTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> courseKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, String> module;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<CourseTableEntry, Integer> abstractUnit;

  /**
   * Constructor.
   * @param inflater Inflater instance to load FXMl
   */
  @Inject
  public SessionDetailView(final Inflater inflater, final Router router) {
    sessionProperty = new SimpleObjectProperty<>();
    sessionFacadeProperty = new SimpleObjectProperty<>();
    this.router = router;

    inflater.inflate("components/detailview/SessionDetailView", this, this, "detailView");
  }

  /**
   * Set content for detail view.
   *
   * @param sessionFacade SessionFacade to build content for
   */
  @SuppressWarnings("WeakerAccess")
  public void setSession(final SessionFacade sessionFacade) {
    this.sessionProperty.set(sessionFacade.getSession());
    this.sessionFacadeProperty.set(sessionFacade);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.title.textProperty().bind(Bindings.when(sessionProperty.isNotNull()).then(
        Bindings.selectString(sessionProperty, "group", "unit", "title")).otherwise(""));
    this.session.textProperty().bind(Bindings.when(sessionProperty.isNotNull()).then(
        Bindings.selectString(sessionFacadeProperty, "slot")).otherwise(""));
    this.group.textProperty().bind(Bindings.when(sessionProperty.isNotNull()).then(
        Bindings.selectString(sessionProperty, "group", "id")).otherwise(""));
    this.semesters.textProperty().bind(new StringBinding() {
      {
        bind(sessionProperty);
      }

      @Override
      protected String computeValue() {
        final Session session = sessionProperty.get();
        if (session == null) {
          return "";
        }
        return Joiner.on(", ").join(session.getGroup().getUnit().getSemesters());
      }
    });
    this.tentative.textProperty().bind(Bindings.createStringBinding(() -> {
      final Session session = sessionProperty.get();
      if (session == null) {
        return "?";
      }

      return session.isTentative() ? "✔︎" : "✗";
    }, sessionProperty));

    courseTable.itemsProperty().bind(new ListBinding<CourseTableEntry>() {
      {
        bind(sessionProperty);
      }

      @Override
      protected ObservableList<CourseTableEntry> computeValue() {
        final Session session = sessionProperty.get();
        if (session == null) {
          return FXCollections.observableArrayList();
        }
        final Set<AbstractUnit> abstractUnits
            = session.getGroup().getUnit().getAbstractUnits();
        final ObservableList<CourseTableEntry> result = FXCollections.observableArrayList();
        abstractUnits.forEach(au ->
            au.getModuleAbstractUnitTypes().forEach(entry ->
              entry.getModule().getCourses().forEach(course -> {
                final Module entryModule = entry.getModule();
                final CourseTableEntry tableEntry = new CourseTableEntry(course, entryModule, au,
                    entryModule.getSemestersForAbstractUnit(au), entry.getType());
                result.add(tableEntry);
              })));
        return result;
      }
    });

    courseTable.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> courseTable.setOnMouseClicked(event -> {
          if (event.getClickCount() < 2) {
            return;
          }

          final TableColumn column
              = courseTable.getSelectionModel().getSelectedCells().get(0).getTableColumn();


          if (column.equals(module)) {
            router.transitionTo(RouteNames.MODULE_DETAIL_VIEW, newValue.getModule());
          } else if (column.equals(abstractUnit)) {
            router.transitionTo(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW, newValue.getAbstractUnit());
          } else if (column.equals(courseKey)) {
            router.transitionTo(RouteNames.COURSE_DETAIL_VIEW, newValue.getCourse());
          }
        }));
  }

  public String getTitle() {
    return title.getText();
  }

  @SuppressWarnings("WeakerAccess")
  public static final class CourseTableEntry {
    private final String courseKey;
    private final Course course;
    private final Module module;
    private final AbstractUnit abstractUnit;
    private final Set<Integer> semesters;
    private final Character type;


    /**
     * Constructor for course table.
     */
    CourseTableEntry(final Course course,
                     final Module module,
                     final AbstractUnit abstractUnit,
                     final Set<Integer> semesters,
                     final Character type) {
      this.course = course;
      this.courseKey = course.getKey();
      this.module = module;
      this.abstractUnit = abstractUnit;
      this.semesters = semesters;
      this.type = type;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }

      final CourseTableEntry that = (CourseTableEntry) other;

      if (!courseKey.equals(that.courseKey)) {
        return false;
      }
      if (!module.getTitle().equals(that.module.getTitle())) {
        return false;
      }
      if (!abstractUnit.getTitle().equals(that.abstractUnit.getTitle())) {
        return false;
      }
      if (!semesters.equals(that.semesters)) {
        return false;
      }
      return type.equals(that.type);

    }

    @Override
    public int hashCode() {
      int result = courseKey.hashCode();
      result = 31 * result + module.hashCode();
      result = 31 * result + abstractUnit.hashCode();
      result = 31 * result + semesters.hashCode();
      result = 31 * result + type.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "CourseTableEntry{"
        + "courseKey='" + courseKey + '\''
        + ", module='" + module + '\''
        + ", abstractUnit='" + abstractUnit + '\''
        + ", semesters=" + semesters
        + ", type=" + type
        + '}';
    }

    @SuppressWarnings("unused")
    public String getCourseKey() {
      return courseKey;
    }

    @SuppressWarnings("unused")
    public String getModuleTitle() {
      return module.getTitle();
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitTitle() {
      return abstractUnit.getTitle();
    }

    @SuppressWarnings("unused")
    public Module getModule() {
      return module;
    }

    @SuppressWarnings("unused")
    public AbstractUnit getAbstractUnit() {
      return abstractUnit;
    }

    /**
     * Create string based on semesters.
     *
     * @return String with comma separated semesters.
     */
    public String getSemesters() {
      return Joiner.on(',').join(semesters);
    }

    public Character getType() {
      return type;
    }

    public Course getCourse() {
      return course;
    }
  }
}