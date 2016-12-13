package de.hhu.stups.plues.routes;

public enum RouteNames {
  INDEX("index"),
  MODULE_DETAIL_VIEW("moduleDetailView"),
  SESSION_DETAIL_VIEW("sessionDetailView"),
  ABSTRACT_UNIT_DETAIL_VIEW("abstractUnitDetailView"),
  UNIT_DETAIL_VIEW("unitDetailView"),
  COURSE("courseDetailView"),
  ABOUT_WINDOW("aboutWindow"),
  REPORTS("Reports"),
  CHANGELOG("changelog");

  private final String routeName;

  RouteNames(final String routeName) {
    this.routeName = routeName;
  }

  public String getRouteName() {
    return routeName;
  }
}
