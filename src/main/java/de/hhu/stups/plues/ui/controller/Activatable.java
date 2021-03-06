package de.hhu.stups.plues.ui.controller;

import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;

/**
 * Interface for controllers that are called from other components via the {@link Router} and need
 * to activate themselves in any way.
 */
@FunctionalInterface
public interface Activatable {
  void activateController(RouteNames route, Object... args);
}
