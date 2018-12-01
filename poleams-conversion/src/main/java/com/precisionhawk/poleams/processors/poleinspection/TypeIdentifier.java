package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.ResourceType;

/**
 *
 * @author pchapman
 */
public interface TypeIdentifier {
    ResourceType identifyType(String fileName);
}
