package io.vanillabp.cockpit.pea;

import java.util.List;

import io.vanillabp.cockpit.extension.spi.AdapterCockpitKey;
import io.vanillabp.cockpit.extension.spi.AdapterCockpitKeys;
import io.vanillabp.pea.PeaAdapter;

/**
 * Tells the Business Cockpit's neutral half which key this half reads below the adapter ids of
 * the Process Engine API, and at which level. With it, the neutral half refuses the key at a
 * workflow module, a workflow or a task, and a typo of it, on both platforms. The service loader
 * finds this class through the file below <code>META-INF/services</code> of this jar.
 */
public class PeaCockpitKeys implements AdapterCockpitKeys {

  /**
   * The service loader builds this class. It holds no state.
   */
  public PeaCockpitKeys() {
  }

  @Override
  public String adapterType() {

    return PeaAdapter.ADAPTER_TYPE;

  }

  @Override
  public List<AdapterCockpitKey> keys() {

    return List
        .of(
            AdapterCockpitKey
                .atTheAdapterOnly(
                    PeaCockpitSettings.REMEMBERED_USER_TASKS,
                    String.valueOf(PeaCockpitSettings.DEFAULT_REMEMBERED_USER_TASKS),
                    "The memory replaces the task query of one engine, so there is one memory for each adapter id, shared by every workflow module."));

  }

}
