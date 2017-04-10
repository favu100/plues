package de.hhu.stups.plues.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.routes.AboutWindowRoute;
import de.hhu.stups.plues.routes.AbstractUnitDetailViewRoute;
import de.hhu.stups.plues.routes.ChangelogRoute;
import de.hhu.stups.plues.routes.ControllerRouteFactory;
import de.hhu.stups.plues.routes.CourseDetailViewRoute;
import de.hhu.stups.plues.routes.HandbookRoute;
import de.hhu.stups.plues.routes.HandbookRouteFactory;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.MainControllerRoute;
import de.hhu.stups.plues.routes.ModuleDetailViewRoute;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.routes.SessionDetailViewRoute;
import de.hhu.stups.plues.routes.UnitDetailViewRoute;

public class RouterProvider implements Provider<Router> {

  private static final String TAB_TIMETABLE = "tabTimetable";

  private final ControllerRouteFactory controllerRouteFactory;
  private final HandbookRouteFactory handbookRouteFactory;
  private Router cache;

  private final Provider<MainControllerRoute> mainControllerRouteProvider;
  private final Provider<ChangelogRoute> changelogRouteProvider;
  private final Provider<AboutWindowRoute> aboutWindowRouteProvider;
  private final Provider<ModuleDetailViewRoute> moduleDetailViewRouteProvider;
  private final Provider<AbstractUnitDetailViewRoute> abstractUnitDetailViewRouteProvider;
  private final Provider<UnitDetailViewRoute> unitDetailViewRouteProvider;
  private final Provider<SessionDetailViewRoute> sessionDetailViewRouteProvider;
  private final Provider<CourseDetailViewRoute> courseDetailViewRouteProvider;
  private final Provider<IndexRoute> indexRouteProvider;

  /**
   * Constructor for routerProvider.
   */
  @Inject
  public RouterProvider(final Provider<IndexRoute> indexRouteProvider,
                        final Provider<ModuleDetailViewRoute> moduleDetailViewRouteProvider,
                        final Provider<AbstractUnitDetailViewRoute>
                            abstractUnitDetailViewRouteProvider,
                        final Provider<UnitDetailViewRoute> unitDetailViewRouteProvider,
                        final Provider<SessionDetailViewRoute> sessionDetailViewRouteProvider,
                        final Provider<CourseDetailViewRoute> courseDetailViewRouteProvider,
                        final Provider<AboutWindowRoute> aboutWindowRouteProvider,
                        final Provider<ChangelogRoute> changelogRouteProvider,
                        final Provider<MainControllerRoute> mainControllerRouteProvider,
                        final ControllerRouteFactory controllerRouteFactory,
                        final HandbookRouteFactory handbookRouteFactory) {
    this.indexRouteProvider = indexRouteProvider;
    this.moduleDetailViewRouteProvider = moduleDetailViewRouteProvider;
    this.abstractUnitDetailViewRouteProvider = abstractUnitDetailViewRouteProvider;
    this.unitDetailViewRouteProvider = unitDetailViewRouteProvider;
    this.sessionDetailViewRouteProvider = sessionDetailViewRouteProvider;
    this.courseDetailViewRouteProvider = courseDetailViewRouteProvider;
    this.aboutWindowRouteProvider = aboutWindowRouteProvider;
    this.changelogRouteProvider = changelogRouteProvider;
    this.mainControllerRouteProvider = mainControllerRouteProvider;
    this.controllerRouteFactory = controllerRouteFactory;
    this.handbookRouteFactory = handbookRouteFactory;
  }

  @Override
  public Router get() {
    if (cache == null) {
      cache = new Router();

      cache.put(RouteNames.INDEX, indexRouteProvider.get());
      cache.put(RouteNames.MODULE_DETAIL_VIEW, moduleDetailViewRouteProvider.get());
      cache.put(RouteNames.SESSION_DETAIL_VIEW,
          sessionDetailViewRouteProvider.get());
      cache.put(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW,
          abstractUnitDetailViewRouteProvider.get());
      cache.put(RouteNames.UNIT_DETAIL_VIEW, unitDetailViewRouteProvider.get());
      cache.put(RouteNames.COURSE_DETAIL_VIEW, courseDetailViewRouteProvider.get());
      cache.put(RouteNames.ABOUT_WINDOW, aboutWindowRouteProvider.get());
      cache.put(RouteNames.CHANGELOG, changelogRouteProvider.get());
      cache.put(RouteNames.HANDBOOK_HTML, handbookRouteFactory.create(HandbookRoute.Format.HTML));
      cache.put(RouteNames.HANDBOOK_PDF, handbookRouteFactory.create(HandbookRoute.Format.PDF));
      cache.put(RouteNames.TIMETABLE,
          controllerRouteFactory.create(TAB_TIMETABLE));
      cache.put(RouteNames.SESSION_IN_TIMETABLE,
          controllerRouteFactory.create(TAB_TIMETABLE));
      cache.put(RouteNames.CONFLICT_IN_TIMETABLE,
          controllerRouteFactory.create(TAB_TIMETABLE));
      cache.put(RouteNames.CHECK_FEASIBILITY_TIMETABLE,
          controllerRouteFactory.create(TAB_TIMETABLE));
      cache.put(RouteNames.PDF_TIMETABLES,
          controllerRouteFactory.create("tabPdfTimetables"));
      cache.put(RouteNames.PARTIAL_TIMETABLES,
          controllerRouteFactory.create("tabPartialTimetables"));
      cache.put(RouteNames.UNSAT_CORE,
          controllerRouteFactory.create("tabUnsatCore"));
      cache.put(RouteNames.OPEN_REPORTS, mainControllerRouteProvider.get());
      cache.put(RouteNames.CLOSE_APP, mainControllerRouteProvider.get());
    }

    return cache;
  }
}
