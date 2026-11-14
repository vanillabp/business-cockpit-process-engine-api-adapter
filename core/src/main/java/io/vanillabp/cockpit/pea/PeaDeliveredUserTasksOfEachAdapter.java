package io.vanillabp.cockpit.pea;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One {@link PeaDeliveredUserTasks} for each adapter id of the Process Engine API.
 * <p>
 * Each adapter id is an engine of its own, and the memory replaces the task query of that engine.
 * With one memory for all of them, an engine which delivers many tasks would push out the tasks
 * of the other one, and the questions about those tasks would go unanswered. That is exactly the
 * case of a migration from one engine to another. So each id gets a memory of its own, sized by
 * its own key.
 */
public class PeaDeliveredUserTasksOfEachAdapter {

  private final Map<String, Integer> rememberedUserTasks;

  private final Map<String, PeaDeliveredUserTasks> memories = new ConcurrentHashMap<>();

  /**
   * Builds the memories, each one empty.
   *
   * @param rememberedUserTasks How many user tasks a node remembers, for each adapter id. An id
   *          which is not in there remembers {@link PeaCockpitSettings#DEFAULT_REMEMBERED_USER_TASKS}
   */
  public PeaDeliveredUserTasksOfEachAdapter(
      final Map<String, Integer> rememberedUserTasks) {

    this.rememberedUserTasks = Map.copyOf(rememberedUserTasks);

  }

  /**
   * The memories of adapter ids which somebody else built, for example a test which looks into
   * one of them.
   *
   * @param memories The memory of each adapter id. An id which is not in there gets a memory of
   *          {@link PeaCockpitSettings#DEFAULT_REMEMBERED_USER_TASKS} tasks when it is first asked
   * @return The memories
   */
  public static PeaDeliveredUserTasksOfEachAdapter holding(
      final Map<String, PeaDeliveredUserTasks> memories) {

    final var result = new PeaDeliveredUserTasksOfEachAdapter(Map.of());
    result.memories.putAll(memories);
    return result;

  }

  /**
   * The memory of one adapter id.
   *
   * @param adapterId The adapter id
   * @return Its memory, created on the first question
   */
  public PeaDeliveredUserTasks of(
      final String adapterId) {

    return memories
        .computeIfAbsent(
            adapterId, id -> new PeaDeliveredUserTasks(
                rememberedUserTasks.getOrDefault(id, PeaCockpitSettings.DEFAULT_REMEMBERED_USER_TASKS)));

  }

  /**
   * How many user tasks the memory of one adapter id holds at most.
   *
   * @param adapterId The adapter id
   * @return The size of its memory
   */
  public int rememberedUserTasksOf(
      final String adapterId) {

    return rememberedUserTasks.getOrDefault(adapterId, PeaCockpitSettings.DEFAULT_REMEMBERED_USER_TASKS);

  }

}
