package io.vanillabp.cockpit.pea.springboot.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import io.vanillabp.cockpit.extension.springboot.CockpitOverlayProperties;
import io.vanillabp.cockpit.pea.PeaCockpitSettings;
import io.vanillabp.cockpit.pea.springboot.PeaCockpitAutoConfiguration;
import io.vanillabp.cockpit.pea.springboot.PeaCockpitOverlayProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Holds what an IDE learns about the key of this half, and that the key reaches the reading.
 * <p>
 * The key stands below an adapter id:
 * <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit.remembered-user-tasks</code>. The format of
 * the configuration metadata has no name for "any adapter id", so no jar can describe it, and the
 * cockpit's <code>extensions-commons-spring-boot</code> no longer describes it at its old place.
 * The wiki page of this half is where a developer reads about it.
 * <p>
 * The keys this half reads are the list {@link PeaCockpitSettings#KEYS_READ}. One test here binds
 * each key of that list the way the application does, so the list cannot name a key nobody reads.
 */
@ExtendWith(SuppressOutputExtension.class)
public class EveryKeyIsKnownToTheIdeTest {

  private static final String METADATA = "META-INF/spring-configuration-metadata.json";

  private static Map<String, ConfigurationMetadataProperty> metadataOfTheCockpit;

  @BeforeAll
  public static void readTheMetadataOfTheCockpit() throws IOException {

    metadataOfTheCockpit = metadataNextTo(CockpitOverlayProperties.class);

  }

  @Test
  @DisplayName("no jar describes a key of this half, because the metadata cannot name a key below an adapter id")
  public void noJarDescribesAKeyOfThisHalf() {

    final var described = new TreeSet<String>();
    metadataOfTheCockpit
        .keySet()
        .stream()
        .filter(name -> PeaCockpitSettings.KEYS_READ.stream().anyMatch(key -> name.endsWith("."
            + key)))
        .forEach(described::add);
    assertTrue(
        described.isEmpty(),
        "extensions-commons-spring-boot describes a key of this half at a place where it is not read: "
            + described);

    final var location = PeaCockpitAutoConfiguration.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .toString();
    assertTrue(
        filesOf(location).isEmpty(),
        "%s has a %s, and the format cannot describe a key below an adapter id".formatted(location, METADATA));

  }

  @Test
  @DisplayName("each key of the list reaches the reading, for the adapter id it is written at")
  public void everyListedKeyIsWhatTheReadingReads() {

    // the value the default is not, so a key which does not reach the reading shows
    final var configured = PeaCockpitSettings.DEFAULT_REMEMBERED_USER_TASKS + 1;
    final var key = PeaCockpitSettings.keyOf("pea", PeaCockpitSettings.REMEMBERED_USER_TASKS);
    assertTrue(
        PeaCockpitSettings.KEYS_READ.contains(PeaCockpitSettings.REMEMBERED_USER_TASKS),
        "'%s' is read, and the list has to name it".formatted(key));
    // bound onto the class the application binds, so the key is what the application reads
    final var overlay = new Binder(new MapConfigurationPropertySource(Map.of(key, String.valueOf(configured))))
        .bind("vanillabp", PeaCockpitOverlayProperties.class)
        .get();
    assertEquals(
        Map.of("pea", configured, "other", PeaCockpitSettings.DEFAULT_REMEMBERED_USER_TASKS),
        PeaCockpitSettings.rememberedUserTasks(overlay.rememberedUserTasks(), List.of("pea", "other")),
        "what '%s' says reaches the reading of that adapter id, and only of that one".formatted(key));

  }

  private static List<URL> filesOf(
      final String location) {

    try {
      final var files = EveryKeyIsKnownToTheIdeTest.class
          .getClassLoader()
          .getResources(METADATA);
      final var result = new ArrayList<URL>();
      while (files.hasMoreElements()) {
        final var file = files.nextElement();
        // a directory of classes is named as it is, a jar as 'jar:<location>!/'
        final var name = file.toString();
        if (name.startsWith(location) || name.startsWith("jar:"
            + location
            + "!/")) {
          result.add(file);
        }
      }
      return result;
    } catch (final IOException e) {
      throw new AssertionError("cannot list %s".formatted(METADATA), e);
    }

  }

  /**
   * The metadata of the jar which holds the given class. The class path of the test holds the
   * files of other jars as well. It is read with Spring Boot's own reader of the format, which
   * tools use as well.
   */
  private static Map<String, ConfigurationMetadataProperty> metadataNextTo(
      final Class<?> type) throws IOException {

    final var location = type
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .toString();
    final var files = filesOf(location);
    if (files.isEmpty()) {
      throw new AssertionError("no %s next to %s, which version of extensions-commons-spring-boot is this?"
          .formatted(METADATA, location));
    }
    try (InputStream in = files
        .get(0)
        .openStream()) {
      return new TreeMap<>(ConfigurationMetadataRepositoryJsonBuilder
          .create(StandardCharsets.UTF_8)
          .withJsonResource(in)
          .build()
          .getAllProperties());
    }

  }

}
