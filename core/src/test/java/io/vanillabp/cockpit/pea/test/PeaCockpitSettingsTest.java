package io.vanillabp.cockpit.pea.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.config.ConfigurationKeyCheck;
import io.vanillabp.cockpit.extension.spi.AdapterCockpitKeys;
import io.vanillabp.cockpit.pea.PeaCockpitKeys;
import io.vanillabp.cockpit.pea.PeaCockpitSettings;
import io.vanillabp.cockpit.pea.PeaDeliveredUserTasks;
import io.vanillabp.cockpit.pea.PeaDeliveredUserTasksOfEachAdapter;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.pea.PeaAdapter;

/**
 * Tests the one setting of this extension, and what an application is told when it sets a value
 * nobody can use. The value is read while the application starts. So a developer sees these
 * messages on the first boot and not in the middle of a working day.
 */
@ExtendWith(SuppressOutputExtension.class)
public class PeaCockpitSettingsTest {

  private static final String KEY_OF_PEA = PeaCockpitSettings.keyOf("pea", PeaCockpitSettings.REMEMBERED_USER_TASKS);

  @Test
  @DisplayName("An application which configures nothing remembers the default number of tasks")
  public void nothingConfiguredIsTheDefault() {

    assertEquals(
        Map.of("pea", PeaCockpitSettings.DEFAULT_REMEMBERED_USER_TASKS),
        PeaCockpitSettings.rememberedUserTasks(Map.of(), List.of("pea")));

  }

  @Test
  @DisplayName("What an application configured for an adapter id is what a node remembers for it")
  public void theConfiguredNumberIsUsedPerAdapterId() {

    assertEquals(
        Map.of("pea", 25, "pea-new", PeaCockpitSettings.DEFAULT_REMEMBERED_USER_TASKS),
        PeaCockpitSettings.rememberedUserTasks(Map.of("pea", " 25 "), List.of("pea", "pea-new")));

  }

  @Test
  @DisplayName("A value which is not a number names the key it stands under")
  public void aValueWhichIsNoNumberIsRefused() {

    final var refused = assertThrows(
        IllegalStateException.class,
        () -> PeaCockpitSettings.rememberedUserTasks(Map.of("pea", "plenty"), List.of("pea")));

    assertTrue(
        refused.getMessage().contains(KEY_OF_PEA),
        () -> "the message names the key to fix: "
            + refused.getMessage());

  }

  @Test
  @DisplayName("A node which remembers nothing reports nothing, so zero is refused")
  public void zeroIsRefused() {

    final var refused = assertThrows(
        IllegalStateException.class,
        () -> PeaCockpitSettings.rememberedUserTasks(Map.of("pea", "0"), List.of("pea")));

    assertTrue(
        refused.getMessage().contains(KEY_OF_PEA),
        () -> "the message names the key to fix: "
            + refused.getMessage());

  }

  @Test
  @DisplayName("Each adapter id has a memory of its own, sized by its own key")
  public void eachAdapterIdHasAMemoryOfItsOwn() {

    final var memories = new PeaDeliveredUserTasksOfEachAdapter(Map.of("pea", 1, "pea-new", 3));

    assertEquals(1, memories.rememberedUserTasksOf("pea"));
    assertEquals(3, memories.rememberedUserTasksOf("pea-new"));
    assertEquals(PeaCockpitSettings.DEFAULT_REMEMBERED_USER_TASKS, memories.rememberedUserTasksOf("unknown"));
    assertNotSame(memories.of("pea"), memories.of("pea-new"), "two adapter ids share one memory");
    assertSame(memories.of("pea"), memories.of("pea"), "an adapter id gets a new memory on every question");

    final var memory = new PeaDeliveredUserTasks(2);
    assertSame(memory, PeaDeliveredUserTasksOfEachAdapter.holding(Map.of("pea", memory)).of("pea"));

  }

  @Test
  @DisplayName("The key is announced to the neutral half, which refuses it below the adapter")
  public void theKeyIsRefusedBelowTheAdapter() {

    final List<AdapterCockpitKeys> announced = ConfigurationKeyCheck.keysOfTheBpmsHalves();
    assertTrue(
        announced.stream().anyMatch(PeaCockpitKeys.class::isInstance),
        "the service loader finds the announcement of this half");

    ConfigurationKeyCheck
        .refuseKeysNobodyReads(
            List.of(KEY_OF_PEA), adapterId -> PeaAdapter.ADAPTER_TYPE, announced);

    final var refused = assertThrows(
        IllegalStateException.class,
        () -> ConfigurationKeyCheck
            .refuseKeysNobodyReads(
                List.of("vanillabp.workflow-modules.taxi-ride.adapters.pea.cockpit.remembered-user-tasks"),
                adapterId -> PeaAdapter.ADAPTER_TYPE, announced));
    assertTrue(
        refused.getMessage().contains("reads 'remembered-user-tasks' at the adapter"),
        refused.getMessage());
    assertTrue(
        refused.getMessage().contains("vanillabp.adapters.<adapter-id>.cockpit.remembered-user-tasks: 1000"),
        refused.getMessage());

  }

  @Test
  @DisplayName("The place of a snapshot of version 2 names the new place")
  public void theOldPlaceNamesTheNewOne() {

    final var refused = assertThrows(
        IllegalStateException.class,
        () -> ConfigurationKeyCheck
            .refuseKeysNobodyReads(List.of("vanillabp.cockpit.process-engine-api.remembered-user-tasks")));

    assertTrue(
        refused.getMessage().contains("vanillabp.adapters.<adapter-id>.cockpit.remembered-user-tasks"),
        refused.getMessage());

  }

}
