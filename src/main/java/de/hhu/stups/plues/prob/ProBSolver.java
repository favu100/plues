package de.hhu.stups.plues.prob;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.hhu.stups.plues.keys.MajorMinorKey;
import de.hhu.stups.plues.keys.OperationPredicateKey;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;
import de.prob.translator.types.Record;
import de.prob.translator.types.Set;

import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProBSolver implements Solver {
  private static final String CHECK = "check";
  private static final String CHECK_PARTIAL = "checkPartial";
  private static final String MOVE = "move";
  private static final String IMPOSSIBLE_COURSES = "getImpossibleCourses";
  private static final String UNSAT_CORE = "unsatCore";
  private static final String LOCAL_ALTERNATIVES = "localAlternatives";

  private static final String DEFAULT_PREDICATE = "1=1";
  private final StateSpace stateSpace;
  private final SolverCache solverResultCache;
  private final SolverCache operationExecutionCache;
  private final ReadOnlyMapProperty<MajorMinorKey, Boolean> courseCombinationResults;
  private Trace trace;

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());

  @Inject
  ProBSolver(final Api api, @Assisted final String modelPath)
      throws IOException, BException {

    final long t1 = System.nanoTime();
    this.stateSpace = api.b_load(modelPath);
    final long t2 = System.nanoTime();
    logger.info("Loaded machine in " + TimeUnit.NANOSECONDS.toMillis(t2 - t1) + " ms");

    this.stateSpace.getSubscribedFormulas()
        .forEach(it -> stateSpace.unsubscribe(this.stateSpace, it));
    this.solverResultCache = new SolverCache(100);
    this.operationExecutionCache = new SolverCache(100);
    this.courseCombinationResults = new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());

    final long t3 = System.nanoTime();
    this.trace = traceFrom(stateSpace);
    final long t4 = System.nanoTime();
    logger.info("Loaded trace in " + TimeUnit.NANOSECONDS.toMillis(t4 - t3) + " ms");
  }

  private Trace traceFrom(final StateSpace space) {
    Trace tracefromSpace = (Trace) space.asType(Trace.class);

    final long start = System.nanoTime();
    tracefromSpace = tracefromSpace.execute("$setup_constants");

    final long t = System.nanoTime();

    tracefromSpace = tracefromSpace.execute("$initialise_machine");
    final long end = System.nanoTime();

    logger.info("$setup_constants took " + TimeUnit.NANOSECONDS.toMillis(t - start) + " ms");
    logger.info("$initialise_machine took " + TimeUnit.NANOSECONDS.toMillis(end - t) + " ms");
    return tracefromSpace;
  }

  private static String getFeasibilityPredicate(final String[] courses) {
    final Iterator<String> iterator = Arrays.stream(courses)
        .filter(it -> it != null && !"".equals(it))
        .map(it -> "\"" + it + "\"").iterator();
    return "ccss={" + Joiner.on(", ").join(iterator) + "}";
  }

  private Boolean executeOperation(final String op, final String predicate) {

    final OperationPredicateKey key = new OperationPredicateKey(op, predicate);
    synchronized (operationExecutionCache) {
      final Boolean cacheObject = (Boolean) operationExecutionCache.get(key);
      if (cacheObject != null) {
        return cacheObject;
      }
    }

    final IEvalElement pred = stateSpace.getModel().parseFormula(predicate);
    final String stateId = trace.getCurrentState().getId();
    final GetOperationByPredicateCommand cmd
        = new GetOperationByPredicateCommand(this.stateSpace, stateId, op, pred, 1);

    stateSpace.execute(cmd);

    if (cmd.isInterrupted() || !cmd.isCompleted()) {
      logger.fine(String.format("%s %s interrupted %s completed %s", op, predicate,
          cmd.isInterrupted(), cmd.isCompleted()));

      synchronized (operationExecutionCache) {
        operationExecutionCache.put(key, false);
      }
      logger.fine(String.format("RESULT %s %s = TIMEOUT/CANCEL", op, predicate));
      return false;
    }

    final boolean result = !cmd.hasErrors();
    if (!result) {
      cmd.getErrors().forEach(logger::severe);
    }
    trace = trace.addTransitions(cmd.getNewTransitions());

    synchronized (operationExecutionCache) {
      operationExecutionCache.put(key, result);
    }

    logger.fine(String.format("RESULT %s %s = %s", op, predicate, result));
    return result;
  }

  private <T extends BObject> T executeOperationWithOneResult(
      final String op, final String predicate, final Class<T> type) throws SolverException {

    final List<T> modelResult = executeOperationWithResult(op, predicate, type);

    if (modelResult.size() != 1) {
      throw new SolverException("Expected one result got: " + modelResult.size());
    }
    return modelResult.get(0);
  }

  private <T extends BObject> T executeOperationWithOneResult(final String op, final Class<T> type)
      throws SolverException {

    return executeOperationWithOneResult(op, DEFAULT_PREDICATE, type);
  }

  @SuppressWarnings("unchecked")
  private <T extends BObject> List<T> executeOperationWithResult(
      final String op, final String predicate, final Class<T> type) throws SolverException {

    final OperationPredicateKey key = new OperationPredicateKey(op, predicate);
    synchronized (solverResultCache) {
      final List<T> cacheObject = (List<T>) solverResultCache.get(key);
      if (cacheObject != null) {
        return cacheObject;
      }
    }

    if (!executeOperation(op, predicate)) {
      throw new SolverException("Could not execute operation " + op + " - " + predicate);
    }

    final Transition trans = trace.getCurrentTransition();
    final List<String> returnValues = trans.evaluate(FormulaExpand.expand).getReturnValues();

    final List<T> result = returnValues.stream().map(i -> {
      try {
        return type.cast(Translator.translate(i));
      } catch (BException bexception) {
        logger.log(Level.SEVERE, "Translator Exception", bexception);
      }
      return null;
    }).collect(Collectors.toList());

    synchronized (solverResultCache) {
      solverResultCache.put(key, result);
    }

    return result;
  }

  /**
   * Checks if the version of the loaded model is compatible with the version provided as parameter.
   * Currently strings must be an exact match.
   */
  @Override
  public final synchronized void checkModelVersion(final String expectedVersion)
      throws SolverException { /* or read properties here? */
    final String modelVersion = this.getModelVersion();
    if (modelVersion.equals(expectedVersion)) {
      return;
    }
    throw new SolverException(
        "Incompatible model version numbers, expected "
            + expectedVersion
            + " but was " + modelVersion);

  }

  @Override
  public final void interrupt() {
    logger.info("Sending interrupt to state space");
    this.stateSpace.sendInterrupt();
  }

  // OPERATIONS

  /**
   * Check if a combination of major and minor courses is feasible.
   *
   * @param courses The combination of major and minor courses.
   * @return Return true if the combination is feasible otherwise false.
   */
  @Override
  public final synchronized Boolean checkFeasibility(final String... courses) {

    final String predicate = getFeasibilityPredicate(courses);
    final Boolean result = executeOperation(CHECK, predicate);
    addCourseCombinationResult(Arrays.asList(courses), result);
    return result;
  }

  /**
   * Compute the {@link FeasibilityResult feasibility result} for a given combination of major and
   * minor courses.
   *
   * @param courses The combination of major and minor courses.
   * @return Return the computed {@link FeasibilityResult FeasibilityResult}.
   */
  @Override
  public final synchronized FeasibilityResult computeFeasibility(final String... courses)
      throws SolverException {

    final String predicate = getFeasibilityPredicate(courses);
    /* Check returns values in the following order:
     *  0: Semester choice - map from abstract unit to a semester
     *  1: Group choice - map from unit to the group chosen for each
     *  2: Module choice - set of modules
     *  3: Unit choice - map from abstract units to units
     */
    try {
      final List<Set> modelResult = executeOperationWithResult(CHECK, predicate, Set.class);

      //
      final Map<Integer, Integer> semesterChoice = Mappers.mapSemesterChoice(modelResult.get(0));

      final Map<Integer, Integer> groupChoice = Mappers.mapGroupChoice(modelResult.get(1));

      final Map<String, java.util.Set<Integer>> moduleChoice
          = Mappers.mapModuleChoice(modelResult.get(2));
      //
      addCourseCombinationResult(Arrays.asList(courses), true);
      return new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);
    } catch (SolverException exception) {
      addCourseCombinationResult(Arrays.asList(courses), false);
      throw exception;
    }
  }

  /**
   * Compute if and how a list of courses might be feasible based on a partial setup of modules and
   * abstract units.
   *
   * @param courses            List of course keys as String
   * @param moduleChoice       map of course key to a set of module IDs already completed in that
   *                           course.
   * @param abstractUnitChoice List of abstract unit IDs already compleated
   * @return FeasibilityResult
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  @Override
  public final synchronized FeasibilityResult computePartialFeasibility(
      final List<String> courses, final Map<String, List<Integer>> moduleChoice,
      final List<Integer> abstractUnitChoice)
      throws SolverException {

    final String mc = Mappers.mapToModuleChoice(moduleChoice);
    final String ac = Joiner.on(',').join(
        abstractUnitChoice.stream().map(i -> "au" + i).iterator());

    final String predicate = getFeasibilityPredicate(courses.toArray(new String[0]))
        + " & partialModuleChoice=" + mc
        + " & partialAbstractUnitChoice={" + ac + "}";

    /* Check returns values in the following order:
     *  0: Semester choice - map from abstract unit to a semester
     *  1: Group choice - map from unit to the group chosen for each
     *  2: Module choice - set of modules
     *  3: Unit choice - map from abstract units to units
     */
    try {
      final List<Set> modelResult = executeOperationWithResult(CHECK_PARTIAL, predicate, Set.class);
      //
      final Map<Integer, Integer> computedSemesterChoice
          = Mappers.mapSemesterChoice(modelResult.get(0));

      final Map<Integer, Integer> computedGroupChoice
          = Mappers.mapGroupChoice(modelResult.get(1));

      final Map<String, java.util.Set<Integer>> computedModuleChoice
          = Mappers.mapModuleChoice(modelResult.get(2));
      //
      addCourseCombinationResult(courses, true);
      return new FeasibilityResult(
          computedModuleChoice, computedSemesterChoice, computedGroupChoice);
    } catch (SolverException exception) {
      addCourseCombinationResult(courses, false);
      throw exception;
    }
  }

  /**
   * For a given list of course keys computes the session IDs in one of the unsat-cores
   *
   * @param courses String[] of course keys
   * @return a list of sessions IDs
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  @Override
  public final synchronized List<Integer> unsatCore(final String... courses)
      throws SolverException {

    final String predicate = getFeasibilityPredicate(courses);
    //
    final Set uc = executeOperationWithOneResult(UNSAT_CORE, predicate, Set.class);
    //
    return Mappers.mapSessions(uc);
  }


  /**
   * Move a session identified by its ID to a new day and time slot.
   *
   * @param sessionId the ID of the Session
   * @param day       String day, valid values are "1".."7"
   * @param slot      Sting representing the selected time slot, valid values are "1".."8".
   */
  @Override
  public final synchronized void move(final String sessionId,
                                      final String day, final String slot) {
    final String predicate
        = "session=session" + sessionId + " & dow=" + day + " & slot=slot" + slot;
    executeOperation(MOVE, predicate);
    solverResultCache.clear();
    operationExecutionCache.clear();
    courseCombinationResults.clear();
  }

  /**
   * A course is impossible if it is statically known to be infeasible.
   *
   * @return Return the set of all impossible courses.
   */
  @Override
  public final synchronized java.util.Set<String> getImpossibleCourses() throws SolverException {

    final Record result = this.executeOperationWithOneResult(IMPOSSIBLE_COURSES, Record.class);

    return Mappers.mapCourseSet((Set) result.get("courses"));
  }

  /**
   * Compute alternative slots for a given session ID, in the context of a specific course
   * combination.
   *
   * @param session ID of the session for which alternatives should be computed
   * @param courses List of courses
   * @return List of alternatives
   * @throws SolverException if no result could be found or the solver did not exit cleanly (e.g.
   *                         interrupt)
   */
  @Override
  public final synchronized List<Alternative> getLocalAlternatives(
      final int session, final String... courses) throws SolverException {

    final String coursePredicate = getFeasibilityPredicate(courses);
    final String predicate = coursePredicate + " & session=" + Mappers.mapSession(session);
    final Set modelResult = this.executeOperationWithOneResult(
        LOCAL_ALTERNATIVES, predicate, Set.class);

    return Mappers.mapAlternatives(modelResult);
  }


  /**
   * Get the model's version.
   *
   * @return String the version string of the model
   */
  @SuppressWarnings("WeakerAccess")
  @Override
  public final synchronized String getModelVersion() throws SolverException {
    final BObject result = this.executeOperationWithOneResult("getVersion", BObject.class);
    return Mappers.mapString(result.toString());
  }

  /**
   * Get the solver cache for testing.
   *
   * @return Return the solver cache containing computed results by the solver.
   */
  final SolverCache getSolverResultCache() {
    return this.solverResultCache;
  }

  /**
   * Get the solver's operation execution cache for testing.
   *
   * @return Return the solver cache containing boolean values for executed operations.
   */
  final SolverCache getOperationExecutionCache() {
    return this.operationExecutionCache;
  }

  @Override
  public final ObservableMap<MajorMinorKey, Boolean> getCourseCombinationResults() {
    return courseCombinationResults;
  }

  /**
   * Add a boolean valued result to the cache {@link ProBSolver#courseCombinationResults}.
   *
   * @param courses The list of courses or a single standalone course.
   * @param result  The boolean valued feasibility result.
   */
  private void addCourseCombinationResult(final List<String> courses, boolean result) {
    final MajorMinorKey key;
    if (courses.size() == 1) {
      key = new MajorMinorKey(courses.get(0), null);
    } else {
      key = new MajorMinorKey(courses.get(0), courses.get(1));
    }
    // only replace if cache not contains key or the existing result is false
    if (!courseCombinationResults.containsKey(key) || !courseCombinationResults.get(key)) {
      courseCombinationResults.put(key, result);
    }
  }
}
