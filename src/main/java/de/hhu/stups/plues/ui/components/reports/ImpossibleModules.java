package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.controlsfx.control.SegmentedButton;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ImpossibleModules extends VBox implements Initializable {

  private final SimpleListProperty<Module> incompleteModules;
  private final SimpleListProperty<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits;

  @FXML
  @SuppressWarnings("unused")
  private SegmentedButton segmentedButtons;
  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonIncompleteModules;
  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonMissingElectiveAbstractUnits;

  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewModules;

  /**
   * Default constructor for incomplete modules component.
   *
   * @param inflater Inflater to handle fxml files and resources
   */
  @Inject
  public ImpossibleModules(final Inflater inflater) {
    incompleteModules = new SimpleListProperty<>(FXCollections.observableArrayList());
    impossibleModulesBecauseOfMissingElectiveAbstractUnits = new SimpleListProperty<>(
        FXCollections.observableArrayList());

    inflater.inflate("components/reports/ImpossibleModules", this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    segmentedButtons.setToggleGroup(new PersistentToggleGroup());
    final ListBinding<Module> binding = new ListBinding<Module>() {
      {
        bind(buttonIncompleteModules.selectedProperty());
        bind(buttonMissingElectiveAbstractUnits.selectedProperty());
        bind(incompleteModules);
        bind(impossibleModulesBecauseOfMissingElectiveAbstractUnits);
      }

      @Override
      protected ObservableList<Module> computeValue() {
        if (buttonIncompleteModules.isSelected()) {
          return incompleteModules;
        } else {
          if (buttonMissingElectiveAbstractUnits.isSelected()) {
            return impossibleModulesBecauseOfMissingElectiveAbstractUnits;
          } else {
            return null;
          }
        }
      }
    };
    tableViewModules.itemsProperty().bind(binding);

    final StringBinding stringBinding = new StringBinding() {
      {
        bind(buttonIncompleteModules.selectedProperty());
        bind(buttonMissingElectiveAbstractUnits.selectedProperty());
      }

      @Override
      protected String computeValue() {
        final String string;
        if (buttonIncompleteModules.isSelected()) {
          string = resources.getString("explain.IncompleteModules");
        } else {
          if (buttonMissingElectiveAbstractUnits.isSelected()) {
            string = resources.getString(
                "explain.ImpossibleModulesBecauseOfMissingElectiveAbstractUnits");
          } else {
            string = null;
          }
        }

        return string;
      }
    };

    txtExplanation.textProperty().bind(stringBinding);
    txtExplanation.wrappingWidthProperty().bind(tableViewModules.widthProperty().subtract(25.0));
  }

  /**
   * Set data for this component.
   *
   * @param incompleteModules                                      Incomplete modules
   * @param impossibleModulesBecauseOfMissingElectiveAbstractUnits modules which are impossible
   *                                                               because of missing elective
   *                                                               abstract units
   */
  public void setData(final List<Module> incompleteModules,
                      final List<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits) {
    this.incompleteModules.setAll(incompleteModules);
    this.impossibleModulesBecauseOfMissingElectiveAbstractUnits.setAll(
        impossibleModulesBecauseOfMissingElectiveAbstractUnits);
  }
}
