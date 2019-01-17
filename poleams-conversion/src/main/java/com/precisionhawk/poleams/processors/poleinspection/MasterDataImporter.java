package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.processors.ProcessListener;
import java.io.File;

/**
 *
 * @author pchapman
 */
public interface MasterDataImporter {
    boolean process(Environment env, ProcessListener listener, File poleDataShapeFile, String orderNum, String otherData);    
}
