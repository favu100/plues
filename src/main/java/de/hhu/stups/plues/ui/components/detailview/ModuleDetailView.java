package de.hhu.stups.plues.ui.components.detailview;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ModuleDetailView extends VBox implements Initializable {

  private final ObjectProperty<Module> moduleProperty;
  private final Router router;

  @FXML
  private Label pordnr;
  @FXML
  private Label title;
  @FXML
  private Label name;
  @FXML
  private Label mandatory;
  @FXML
  private Label creditPoints;
  @FXML
  private Label electiveUnits;
  @FXML
  private TableView<Course> courseTableView;
  @FXML
  private TableView<AbstractUnit> abstractUnitTableView;

  /**
   * Constructor for ModuleDetailView.
   * @param inflater Inflater to handle fxml and lang files
   */
  @Inject
  public ModuleDetailView(final Inflater inflater,
                          final Router router) {
    moduleProperty = new SimpleObjectProperty<>();
    this.router = router;

    inflater.inflate("/components/detailview/ModuleDetailView", this, this, "detailView", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    pordnr.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "pordnr")).otherwise(""));
    title.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "title")).otherwise(""));
    name.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "name")).otherwise(""));
    mandatory.textProperty().bind(Bindings.createStringBinding(() -> {
      final Module module = moduleProperty.get();
      if (module == null) {
        return "?";
      }

      return module.getMandatory() ? "✔︎" : "✗";
    }, moduleProperty));
    creditPoints.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "creditPoints")).otherwise(""));
    electiveUnits.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "electiveUnits")).otherwise(""));

    courseTableView.itemsProperty().bind(new ListBinding<Course>() {
      {
        bind(moduleProperty);
      }

      @Override
      protected ObservableList<Course> computeValue() {
        final Module module = moduleProperty.get();
        if (module == null) {
          return FXCollections.emptyObservableList();
        }

        return FXCollections.observableArrayList(module.getCourses());
      }
    });
    abstractUnitTableView.itemsProperty().bind(new ListBinding<AbstractUnit>() {
      {
        bind(moduleProperty);
      }

      @Override
      protected ObservableList<AbstractUnit> computeValue() {
        final Module module = moduleProperty.get();
        if (module == null) {
          return FXCollections.emptyObservableList();
        }

        return FXCollections.observableArrayList(module.getAbstractUnits());
      }
    });

    courseTableView.setOnMouseClicked(DetailViewHelper.getCourseMouseHandler(
        courseTableView, router));

    abstractUnitTableView.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitTableView, router));
  }

  public void setModule(final Module module) {
    this.moduleProperty.set(module);
  }

  public String getTitle() {
    return moduleProperty.get().getTitle();
  }
}