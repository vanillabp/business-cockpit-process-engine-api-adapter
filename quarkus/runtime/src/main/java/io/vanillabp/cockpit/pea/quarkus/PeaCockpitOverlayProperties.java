package io.vanillabp.cockpit.pea.quarkus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * The keys this extension reads itself, as an OVERLAY of the shared <code>vanillabp.*</code>
 * configuration tree on Quarkus. It is one more RUN_TIME mapping over the same prefix as the
 * platform's own and the Process-Engine-API adapter's. SmallRye's check for unknown keys passes as soon as
 * one mapping declares a key, so this mapping is also what lets an application which writes the
 * key boot at all.
 * <p>
 * There is one key:
 *
 * <pre>
 * vanillabp.adapters.&lt;adapter-id&gt;.cockpit.remembered-user-tasks
 * </pre>
 *
 * It is declared as text and read by {@code PeaCockpitSettings} in the core, so a value which is
 * no number gets the same message here and on Spring Boot.
 * <p>
 * The mapping is not <code>&#64;StaticInitSafe</code>, like the Business Cockpit's own overlay:
 * an overlay belongs in the same phase as the platform's mapping it overlays.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "vanillabp")
public interface PeaCockpitOverlayProperties {

  /**
   * The sections of the adapter ids, as far as the Business Cockpit reads them.
   *
   * @return The sections, by adapter id
   */
  @ConfigDocMapKey("adapter-id")
  Map<String, Adapter> adapters();

  /**
   * What the application wrote about the memory of each adapter id.
   *
   * @return The raw values, for the adapter ids which carry the key
   */
  default Map<String, String> rememberedUserTasks() {

    final var remembered = new LinkedHashMap<String, String>();
    adapters()
        .forEach((
            adapterId,
            adapter) -> adapter
                .cockpit()
                .flatMap(Cockpit::rememberedUserTasks)
                .ifPresent(value -> remembered.put(adapterId, value)));
    return remembered;

  }

  /**
   * The section of one adapter id.
   */
  interface Adapter {

    /**
     * What the Business Cockpit reads for this adapter.
     *
     * @return The section <code>cockpit</code>, where the application wrote one
     */
    Optional<Cockpit> cockpit();

  }

  /**
   * <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit</code>.
   */
  interface Cockpit {

    /**
     * How many delivered user tasks of this engine a node remembers. The default is 1000.
     *
     * @return The number, as written
     */
    Optional<String> rememberedUserTasks();

  }

}
