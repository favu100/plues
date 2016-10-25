package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.FeasibilityResult;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestDataPreperatory {

  private MockStore store;
  private Course course;
  private FeasibilityResult result;

  /**
   * Test setup.
   */
  @Before
  public void setUp() {
    store = new MockStore();
    course = store.getCourseByKey("foo");

    final Map<Integer, Integer> groupChoice = new HashMap<>();
    groupChoice.put(1, 1);
    groupChoice.put(2, 12);
    groupChoice.put(3, 3);
    groupChoice.put(4, 4);
    groupChoice.put(5, 11);
    groupChoice.put(6, 6);
    groupChoice.put(7, 7);
    groupChoice.put(8, 10);
    groupChoice.put(9, 9);

    final Map<Integer, Integer> semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    final Map<String, Set<Integer>> moduleChoice = new HashMap<>();
    Set<Integer> integerSet = new HashSet<Integer>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    this.result = new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);
  }


  @Test
  public void testGetUnitGroup() {
    final DataPreparatory data = new DataPreparatory(store, result, course, null);

    final Map<AbstractUnit, Group> groups = data.getUnitGroup();

    // Test not null and size
    assertNotNull(groups);
    assertEquals(groups.size(), result.getGroupChoice().size());

    final Map<Integer, Integer> ids = groups.entrySet().stream().collect(Collectors.toMap(
        e -> e.getKey().getId(),
        e -> e.getValue().getId()));

    for (final Map.Entry<Integer, Integer> gc : result.getGroupChoice().entrySet()) {
      final Integer key = gc.getKey();
      final Integer value = gc.getValue();
      assertTrue(ids.containsKey(key));
      assertEquals(ids.get(key), value);
    }
  }

  @Test
  public void testUnitModuleMapping() {
    final Course course = store.getCourseByKey("foo");

    final DataPreparatory dp = new DataPreparatory(store, result, course, null);

    final Map<AbstractUnit, Module> um = dp.getUnitModule();
    final Map<Integer, Integer> ids = um.entrySet().stream().collect(Collectors.toMap(
        e -> e.getKey().getId(),
        e -> e.getValue().getId()));
    assertEquals(ids.get(2).intValue(), 1);
    assertEquals(ids.get(3).intValue(), 1);
    assertEquals(ids.get(6).intValue(), 3);
    assertEquals(ids.get(7).intValue(), 3);
  }


}
