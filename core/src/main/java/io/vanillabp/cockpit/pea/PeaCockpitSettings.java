package io.vanillabp.cockpit.pea;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the Business Cockpit's Process-Engine-API half reads out of the application's
 * configuration, and what it says when it does not like it.
 * <p>
 * There is one setting: how many delivered user tasks a node remembers for one adapter id. It
 * exists because this BPMS cannot be asked about a task twice. Everything else about the cockpit
 * is configured once for every BPMS, by the extension's platform-neutral half.
 * <p>
 * The setting stands below the adapter id, because each adapter id is an engine of its own and
 * the memory replaces the task query of that engine. During a migration two engines run side by
 * side, and one which delivers many tasks must not push out the tasks of the other. A value per
 * workflow module would promise a division which does not exist, so the key is read at the
 * adapter only. The neutral half refuses it at every other level, see {@link PeaCockpitKeys}.
 * <p>
 * The value is read and checked while the application starts. A value nobody can use is then a
 * message on the first boot rather than an exception in the middle of a working day.
 */
public final class PeaCockpitSettings {

  private static final Logger logger = LoggerFactory.getLogger(PeaCockpitSettings.class);

  /**
   * How many delivered user tasks a node remembers at once for one adapter id, below
   * <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit</code>.
   */
  public static final String REMEMBERED_USER_TASKS = "remembered-user-tasks";

  /**
   * Enough for the open tasks of a busy engine, which is what this memory is asked about, and
   * small enough to be paid for without thinking about it.
   */
  public static final int DEFAULT_REMEMBERED_USER_TASKS = 1000;

  /**
   * Every key this half reads below <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit</code>.
   * The test which holds the configuration metadata an IDE reads walks this list. A new key goes
   * here, and that test then asks for its description.
   */
  public static final List<String> KEYS_READ = List.of(REMEMBERED_USER_TASKS);

  private PeaCockpitSettings() {
  }

  /**
   * The full key of a setting of this half for one adapter id.
   *
   * @param adapterId The adapter id, or a placeholder for a message
   * @param key One of {@link #KEYS_READ}
   * @return e.g. <code>vanillabp.adapters.pea.cockpit.remembered-user-tasks</code>
   */
  public static String keyOf(
      final String adapterId,
      final String key) {

    return "vanillabp.adapters.%s.cockpit.%s".formatted(adapterId, key);

  }

  /**
   * Reads how many delivered user tasks a node remembers for each adapter id of this BPMS, and
   * checks the values. Where nothing is configured for an id, it is
   * {@link #DEFAULT_REMEMBERED_USER_TASKS}.
   *
   * @param configured What the application wrote at
   *          <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit.remembered-user-tasks</code>, by
   *          adapter id. Ids of other BPMS may be in there; the neutral half refuses those
   * @param adapterIds The adapter ids of this BPMS
   * @return How many delivered user tasks a node is to remember, for each of the adapter ids
   * @throws IllegalStateException If a configured value is not a number or not positive; the
   *           message names the key and the default
   */
  public static Map<String, Integer> rememberedUserTasks(
      final Map<String, String> configured,
      final Collection<String> adapterIds) {

    final var remembered = new LinkedHashMap<String, Integer>();
    adapterIds
        .forEach(adapterId -> remembered.put(adapterId, rememberedUserTasks(adapterId, configured.get(adapterId))));
    return remembered;

  }

  private static int rememberedUserTasks(
      final String adapterId,
      final String configured) {

    final var key = keyOf(adapterId, REMEMBERED_USER_TASKS);
    if ((configured == null) || configured.isBlank()) {
      logger
          .debug(
              "The Business Cockpit remembers {} delivered user tasks of adapter '{}' per node; set '{}' to change it",
              DEFAULT_REMEMBERED_USER_TASKS,
              adapterId,
              key);
      return DEFAULT_REMEMBERED_USER_TASKS;
    }
    final int remembered;
    try {
      remembered = Integer.parseInt(configured.trim());
    } catch (final NumberFormatException e) {
      throw new IllegalStateException(
          """
              The Business Cockpit's Process-Engine-API half was configured with '%s: %s', which is \
              not a number. The key says how many delivered user tasks of this engine one node \
              remembers, and a node has to remember them because the Process-Engine-API cannot be \
              asked about a task a second time. Write a positive number there, or remove the key, \
              which leaves it at %d."""
              .formatted(key, configured, DEFAULT_REMEMBERED_USER_TASKS), e);
    }
    if (remembered < 1) {
      throw new IllegalStateException(
          """
              The Business Cockpit's Process-Engine-API half was configured with '%s: %d'. A node \
              which remembers no user task reports none, because the Process-Engine-API cannot be \
              asked about a task a second time. Write a positive number there, or remove the key, \
              which leaves it at %d."""
              .formatted(key, remembered, DEFAULT_REMEMBERED_USER_TASKS));
    }
    return remembered;

  }

}
