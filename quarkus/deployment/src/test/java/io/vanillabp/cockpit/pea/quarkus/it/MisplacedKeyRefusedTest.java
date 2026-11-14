package io.vanillabp.cockpit.pea.quarkus.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.pea.PeaCockpitSettings;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Tests that the key of this half, written below a workflow module, ends the build. This half
 * announces its key through the service loader of its jar, and the cockpit's neutral half reads
 * that announcement while Quarkus builds the application.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class MisplacedKeyRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = TestApplication.forTestClass(MisplacedKeyRefusedTest.class)
      // written into the build's configuration, because the cockpit checks the keys while
      // Quarkus builds the application
      .overrideConfigKey(
          "vanillabp.workflow-modules.pea-cockpit.adapters.pea.cockpit.%s"
              .formatted(PeaCockpitSettings.REMEMBERED_USER_TASKS),
          "25")
      .assertException(throwable -> {
        final var messages = new StringBuilder();
        for (var cause = throwable; cause != null; cause = cause.getCause()) {
          messages
              .append(cause.getMessage())
              .append('\n');
        }
        Assertions
            .assertTrue(
                messages
                    .toString()
                    .contains(PeaCockpitSettings.keyOf("<adapter-id>", PeaCockpitSettings.REMEMBERED_USER_TASKS)),
                "the build ends naming the key to write instead: "
                    + messages);
      });

  @Test
  @DisplayName("The key below a workflow module ends the build and names the key at the adapter")
  public void theKeyBelowAWorkflowModuleEndsTheBuild() {
    // the expected startup failure is asserted above, so this never runs
  }

}
