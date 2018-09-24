package com.precisionhawk.poleams.support.elasticsearch;

import javax.inject.Named;
import org.elasticsearch.client.Client;
import us.pcsw.es.util.ClientLifecycleListener;
import us.pcsw.es.util.ESUtils;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class IndexEnsuringLifecycleListener implements ClientLifecycleListener, ElasticSearchConstants {

    @Override
    public void clientConnectionOpened(Client client) {
        for (String index : INDEXES) {
            ESUtils.prepareIndex(client, index, null);
        }
    }

    @Override
    public void prepareForClose(Client arg0) {}

    @Override
    public void afterClose() {}
}
