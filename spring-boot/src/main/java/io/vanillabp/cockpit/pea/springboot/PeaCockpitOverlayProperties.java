package io.vanillabp.cockpit.pea.springboot;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.integration.extension.spi.settings.VanillaBpSettings;

/**
 * The key this half reads itself, as an OVERLAY of the shared <code>vanillabp.*</code>
 * configuration tree on Spring Boot. It is one more {@code @ConfigurationProperties("vanillabp")}
 * class beside the platform's own. Classes with the same prefix bind side by side, and a key which
 * one of them does not know is ignored by that one.
 * <p>
 * There is one key:
 *
 * <pre>
 * vanillabp.adapters.&lt;adapter-id&gt;.cockpit.remembered-user-tasks
 * </pre>
 *
 * It is bound as text and read by {@code PeaCockpitSettings} in the core, so a value which is no
 * number gets the same message here and on Quarkus.
 */
@ConfigurationProperties(VanillaBpSettings.PREFIX)
public class PeaCockpitOverlayProperties {

  /** The sections of the adapter ids, by adapter id. */
  private Map<String, Adapter> adapters = Map.of();

  /**
   * Creates the tree, which Spring Boot fills when it binds the properties.
   */
  public PeaCockpitOverlayProperties() {
  }

  /**
   * Gives the sections of the adapter ids.
   *
   * @return The sections of the adapter ids, by adapter id
   */
  public Map<String, Adapter> getAdapters() {

    return adapters;

  }

  /**
   * Sets the sections of the adapter ids. Spring Boot calls it while it binds.
   *
   * @param adapters The sections of the adapter ids, by adapter id
   */
  public void setAdapters(
      final Map<String, Adapter> adapters) {

    this.adapters = adapters;

  }

  /**
   * What the application wrote about the memory of each adapter id.
   *
   * @return The raw values, for the adapter ids which carry the key
   */
  public Map<String, String> rememberedUserTasks() {

    final var remembered = new LinkedHashMap<String, String>();
    adapters
        .forEach((
            adapterId,
            adapter) -> {
          if ((adapter != null) && (adapter.getCockpit() != null) && (adapter
              .getCockpit()
              .getRememberedUserTasks() != null)) {
            remembered.put(adapterId, adapter.getCockpit().getRememberedUserTasks());
          }
        });
    return remembered;

  }

  /**
   * The section of one adapter id, as far as the Business Cockpit reads it.
   */
  public static class Adapter {

    /** What the Business Cockpit reads for this adapter. */
    private Cockpit cockpit;

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public Adapter() {
    }

    /**
     * What the Business Cockpit reads for this adapter.
     *
     * @return The section <code>cockpit</code>, or <code>null</code>
     */
    public Cockpit getCockpit() {

      return cockpit;

    }

    /**
     * What the Business Cockpit reads for this adapter.
     *
     * @param cockpit The section <code>cockpit</code>
     */
    public void setCockpit(
        final Cockpit cockpit) {

      this.cockpit = cockpit;

    }

  }

  /**
   * <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit</code>.
   */
  public static class Cockpit {

    /** How many delivered user tasks of this engine a node remembers. */
    private String rememberedUserTasks;

    /**
     * Creates an empty section, which Spring Boot fills when it binds the properties.
     */
    public Cockpit() {
    }

    /**
     * How many delivered user tasks of this engine a node remembers.
     *
     * @return The number, as text
     */
    public String getRememberedUserTasks() {

      return rememberedUserTasks;

    }

    /**
     * How many delivered user tasks of this engine a node remembers.
     *
     * @param rememberedUserTasks The number, as text
     */
    public void setRememberedUserTasks(
        final String rememberedUserTasks) {

      this.rememberedUserTasks = rememberedUserTasks;

    }

  }

}
