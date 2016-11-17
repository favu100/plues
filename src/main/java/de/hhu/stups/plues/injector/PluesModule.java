package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.prob.MockSolver;
import de.hhu.stups.plues.prob.ProBSolver;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.prob.SolverFactory;
import de.hhu.stups.plues.provider.RouterProvider;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverLoader;
import de.hhu.stups.plues.tasks.SolverLoaderImpl;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverServiceFactory;
import de.hhu.stups.plues.tasks.StoreLoaderTaskFactory;
import de.hhu.stups.plues.ui.components.BatchResultBoxFactory;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.FeasibilityBoxFactory;
import de.hhu.stups.plues.ui.components.ResultBoxFactory;
import de.hhu.stups.plues.ui.components.timetable.SessionListViewFactory;
import de.hhu.stups.plues.ui.controller.MainController;
import de.prob.MainModule;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.ResourceBundle;

public class PluesModule extends AbstractModule {

  private final TypeLiteral<Delayed<Store>> delayedStoreType
      = new TypeLiteral<Delayed<Store>>() {};
  private final TypeLiteral<Delayed<ObservableStore>> delayedObservableStoreType
      = new TypeLiteral<Delayed<ObservableStore>>() {};

  private final TypeLiteral<Delayed<SolverService>> delayedSolverServiceType
      = new TypeLiteral<Delayed<SolverService>>() {};

  private final TypeLiteral<ObjectProperty<Date>> lastSavedType
      = new TypeLiteral<ObjectProperty<Date>>() {};

  private final Stage primaryStage;

  // bundle with default language
  private final ResourceBundle bundle = ResourceBundle.getBundle("lang.main");

  public PluesModule(final Stage primaryStage) {
    this.primaryStage = primaryStage;
  }

  @Override
  public final void configure() {
    // prob 2.0
    install(new MainModule());

    install(new PropertiesModule());
    install(new ComponentsModule());
    install(new ExecutorServiceModule());

    install(new FactoryModuleBuilder().build(SolverLoaderTaskFactory.class));
    install(new FactoryModuleBuilder().build(SolverServiceFactory.class));
    install(new FactoryModuleBuilder().build(PdfRenderingTaskFactory.class));
    install(new FactoryModuleBuilder().build(ResultBoxFactory.class));
    install(new FactoryModuleBuilder().build(FeasibilityBoxFactory.class));
    install(new FactoryModuleBuilder().build(BatchResultBoxFactory.class));
    install(new FactoryModuleBuilder().build(CheckBoxGroupFactory.class));
    install(new FactoryModuleBuilder().build(StoreLoaderTaskFactory.class));
    install(new FactoryModuleBuilder().build(SessionListViewFactory.class));

    install(new FactoryModuleBuilder()
        .implement(Solver.class, Names.named("prob"), ProBSolver.class)
        .implement(Solver.class, Names.named("mock"), MockSolver.class)
        .build(SolverFactory.class));

    bind(Stage.class).toInstance(primaryStage);
    bind(Router.class).toProvider(RouterProvider.class);
    bind(MainController.class);
    bind(ResourceBundle.class).toInstance(bundle);

    bind(SolverLoader.class).to(SolverLoaderImpl.class);

    final Delayed store = new Delayed(); // TODO: Unchecked Cast need to be solved
    bind(delayedStoreType).toInstance(store);
    bind(delayedObservableStoreType).toInstance(store);
    bind(delayedSolverServiceType).toInstance(new Delayed<>());

    bind(lastSavedType).toInstance(new SimpleObjectProperty<>(
        new Date(ManagementFactory.getRuntimeMXBean().getStartTime())));
  }

  @Provides
  final FXMLLoader provideLoader(final Injector injector,
                                 final GuiceBuilderFactory builderFactory,
                                 final ResourceBundle bundle) {

    final FXMLLoader fxmlLoader = new FXMLLoader();

    fxmlLoader.setBuilderFactory(builderFactory);
    fxmlLoader.setControllerFactory(injector::getInstance);
    fxmlLoader.setResources(bundle);

    return fxmlLoader;
  }
}
