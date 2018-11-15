package com.precisionhawk.poleams.support.elasticsearch;

import com.precisionhawk.ams.support.elasticsearch.IndexEnsuringLifecycleListener;
import javax.inject.Named;

/**
 *
 * @author pchapman
 */
@Named
public class LifecycleListenerImpl extends IndexEnsuringLifecycleListener {

    @Override
    public String[] getIndexes() {
        return ElasticSearchConstants.INDEXES;
    }
    
}
