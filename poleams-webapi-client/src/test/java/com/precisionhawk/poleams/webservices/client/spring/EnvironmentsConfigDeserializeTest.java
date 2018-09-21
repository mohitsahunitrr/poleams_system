/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices.client.spring;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class EnvironmentsConfigDeserializeTest {
    private static final String CONFIG_YAML_1 =
        "- name: Local Dev\n" +
        "  tenantId: 4b07043f-40ce-464d-8715-8e2a4fd8d7d1\n" +
        "  clientId: 957fc39c-ec1f-4e7c-a3fb-5ad5543ba775\n" +
        "  clientSecret: SOMETHING_SECRET\n" +
        "  serviceAppId: 17722449-ef2e-4adc-ad1d-8b7ead334ec9\n" +
        "  serviceURI: http://localhost:7171/";
    private static final String CONFIG_YAML_2 =
        CONFIG_YAML_1 + "\n" +
        "- name: Entry 2\n" +
        "  tenantId: abcd\n" +
        "  clientId: efgh\n" +
        "  clientSecret: YOU DON'T KNOW THIS\n" +
        "  serviceAppId: ijkl\n" +
        "  serviceURI: http://somehost.somedomain/";
    
    @Test
    public void testDeserialize() throws YamlException {
        YamlReader reader = new YamlReader(CONFIG_YAML_1);
        List<EnvironmentConfig> list = reader.read(List.class, EnvironmentConfig.class);
        assertEquals(1, list.size());
        testEntry1(list.get(0));
        
        reader = new YamlReader(CONFIG_YAML_2);
        list = reader.read(List.class, EnvironmentConfig.class);
        assertEquals(2, list.size());
        testEntry1(list.get(0));
        testEntry2(list.get(1));
    }
    
    private void testEntry1(EnvironmentConfig cfg) {
        assertEquals("Local Dev", cfg.getName());
        assertEquals("4b07043f-40ce-464d-8715-8e2a4fd8d7d1", cfg.getTenantId());
        assertEquals("957fc39c-ec1f-4e7c-a3fb-5ad5543ba775", cfg.getClientId());
        assertEquals("SOMETHING_SECRET", cfg.getClientSecret());
        assertEquals("17722449-ef2e-4adc-ad1d-8b7ead334ec9", cfg.getServiceAppId());
        assertEquals("http://localhost:7171/", cfg.getServiceURI());
    }
    
    private void testEntry2(EnvironmentConfig cfg) {
        assertEquals("Entry 2", cfg.getName());
        assertEquals("abcd", cfg.getTenantId());
        assertEquals("efgh", cfg.getClientId());
        assertEquals("YOU DON'T KNOW THIS", cfg.getClientSecret());
        assertEquals("ijkl", cfg.getServiceAppId());
        assertEquals("http://somehost.somedomain/", cfg.getServiceURI());
    }
}
