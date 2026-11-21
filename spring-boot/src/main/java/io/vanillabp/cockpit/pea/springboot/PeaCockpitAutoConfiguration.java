package io.vanillabp.cockpit.pea.springboot;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.pea.PeaBusinessCases;
import io.vanillabp.cockpit.pea.PeaCockpitObserver;
import io.vanillabp.cockpit.pea.PeaCockpitSettings;
import io.vanillabp.cockpit.pea.PeaDeliveredUserTasksOfEachAdapter;
import io.vanillabp.cockpit.pea.PeaProcessVersions;
import io.vanillabp.cockpit.pea.PeaRecordedUserTasks;
import io.vanillabp.integration.extension.spi.election.WorkflowElection;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.resolver.TaskDeliveryLogResolver;
import io.vanillabp.integration.extension.spi.settings.VanillaBpSettings;
import io.vanillabp.integration.spi.workflowtask.WorkflowTaskClaims;
import io.vanillabp.pea.PeaAdapter;
import io.vanillabp.pea.deployment.PeaDeployedProcessesRegistry;

/**
 * Registers the Process-Engine-API half of the Business Cockpit extension on Spring Boot.
 * <p>
 * Nothing here decides anything. What the extension does with this BPMS is decided in the
 * platform-neutral module of this repository. This class does what only Spring can do: find the
 * beans, and put the extension's own beans where VanillaBP and the cockpit's neutral half collect
 * them.
 * <p>
 * It runs after VanillaBP's own auto-configuration, named rather than referenced, because an
 * extension does not compile against a platform integration.
 */
@AutoConfiguration(afterName = "io.vanillabp.integration.processservice.SpringBootMigrationAdapterAutoConfiguration")
@ConditionalOnBean(VanillaBpSettings.class)
@Import(PeaCockpitBeanRegistrar.class)
@EnableConfigurationProperties(PeaCockpitOverlayProperties.class)
public class PeaCockpitAutoConfiguration {

  /**
   * Spring Boot builds this class while it applies the auto-configuration. It holds no state
   * of its own. The beans come from its methods.
   */
  public PeaCockpitAutoConfiguration() {

  }

  /**
   * The memories of what a delivered user task said, one for each adapter id of this BPMS, each
   * sized by the application.
   * <p>
   * The configured values are read here, while the application starts, rather than when the
   * first task arrives. A number nobody can use is then a message on the first boot. A value at a
   * level where nobody reads it is refused by the cockpit's neutral half, see
   * {@code PeaCockpitKeys}.
   *
   * @param overlay What the application wrote below the adapter ids
   * @param properties VanillaBP's resolved configuration, which names the adapter ids of this
   *          BPMS
   * @return The memories
   */
  @Bean
  public PeaDeliveredUserTasksOfEachAdapter businessCockpitPeaDeliveredUserTasks(
      final PeaCockpitOverlayProperties overlay,
      final VanillaBpSettings properties) {

    return new PeaDeliveredUserTasksOfEachAdapter(
        PeaCockpitSettings
            .rememberedUserTasks(overlay.rememberedUserTasks(), properties.adapterIdsOfType(PeaAdapter.ADAPTER_TYPE)));

  }

  /**
   * What VanillaBP wrote down about the deliveries it processed. It is the durable half of what
   * this extension knows about a user task. The store is not picked here. The platform is asked,
   * because which store serves an aggregate follows the persistence VanillaBP resolved for it.
   *
   * @param handlers VanillaBP's answer to which workflow aggregate serves a BPMN process
   * @param deliveryLogs The platform's resolver of the store holding an aggregate's records
   * @param registry What the Process-Engine-API adapter recorded while deploying
   * @return The reader of the delivery log
   */
  @Bean
  public PeaRecordedUserTasks businessCockpitPeaRecordedUserTasks(
      final ExtensionHandlers handlers,
      final TaskDeliveryLogResolver deliveryLogs,
      final PeaDeployedProcessesRegistry registry) {

    return new PeaRecordedUserTasks(handlers, deliveryLogs, registry);

  }

  /**
   * The versions of the BPMN processes this application deployed, as the adapter recorded them.
   *
   * @param registry What the Process-Engine-API adapter recorded while deploying
   * @return The versions of the deployed processes
   */
  @Bean
  public PeaProcessVersions businessCockpitPeaProcessVersions(
      final PeaDeployedProcessesRegistry registry) {

    return PeaProcessVersions.of(registry);

  }

  /**
   * Where a user task the Process-Engine-API delivered is handed to the cockpit. The adapter
   * finds it by its type and calls it from its own subscription, which is the only place a
   * second pair of eyes can watch a delivery on this BPMS.
   *
   * @param registry What the Process-Engine-API adapter recorded while deploying, which is where
   *          the name of a process and the BPMN element behind a form reference come from
   * @param workflowTaskClaims VanillaBP's answer to whether a <code>&#64;WorkflowService</code>
   *          claims a BPMN process, which is what decides whether a delivery is reported
   * @param deliveredUserTasks The memories of what a delivery said, one for each adapter id
   * @param versions The versions of the deployed processes
   * @param publisher Where an observed event is reported. It is resolved on the first event
   *          rather than now: this bean is built while the application is still wiring itself
   *          together
   * @param handlers VanillaBP's list of the processes the application declares
   * @param election VanillaBP's note of the instance it started for an aggregate
   * @return The observer
   */
  @Bean
  public PeaCockpitObserver businessCockpitPeaUserTaskObserver(
      final PeaDeployedProcessesRegistry registry,
      final WorkflowTaskClaims workflowTaskClaims,
      final PeaDeliveredUserTasksOfEachAdapter deliveredUserTasks,
      final PeaProcessVersions versions,
      final ObjectProvider<BusinessCockpitEventPublisher> publisher,
      final ExtensionHandlers handlers,
      final WorkflowElection election) {

    return new PeaCockpitObserver(
        registry, workflowTaskClaims, deliveredUserTasks, versions, publisher::getObject, new PeaBusinessCases(
            workflowTaskClaims, handlers, election));

  }


}
