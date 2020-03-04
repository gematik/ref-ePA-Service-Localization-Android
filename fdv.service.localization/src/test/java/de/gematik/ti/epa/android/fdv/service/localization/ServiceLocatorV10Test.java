/*
 * Copyright (c) 2020 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.ti.epa.android.fdv.service.localization;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;

/**
 * Test {@link ServiceLocator}
 */
public class ServiceLocatorV10Test extends AbstractServiceLocatorTestTest {
    private AtomicReference<LookupStatus> reveivedCallBack;
    private Record answer;
    private Method fillDnsTxtRecordValues;

    @Before
    public void init() throws Exception {
        reveivedCallBack = new AtomicReference<>();
        callback = reveivedCallBack::set;
        serviceLocator = new ServiceLocatorV10();
        try {
            serviceLocator.lookup(FQDN, callback);
        } catch (Exception e) {
            // Exception by Test with Java because Android Object needed
        }
        final Name current = Name.fromString(FQDN_ABSOLUTE);
        answer = Record.fromString(current, TYPE_TXT, DClass.ANY, 0x20000, RECORD_TXT, current);

        fillDnsTxtRecordValues = AbstractServiceLocator.class.getDeclaredMethod("fillDnsTxtRecordValues", new Record[0].getClass());
        fillDnsTxtRecordValues.setAccessible(true);
    }

    @Test
    public void testGetServiceLocations() throws Exception {
        fillDnsTxtRecordValues.invoke(serviceLocator, new Object[] { new Record[] { answer } });
        checkUrls(serviceLocator);
        Assert.assertEquals(LookupStatus.SUCCESS, serviceLocator.getLookupStatus());
        Assert.assertFalse(serviceLocator.isRunning());
    }

    @Test
    public void testGetServiceLocationsEmpty() throws Exception {
        fillDnsTxtRecordValues.invoke(serviceLocator, new Object[] { new Record[0] });
        Assert.assertEquals(LookupStatus.MISSING_TXT_RECORD, serviceLocator.getLookupStatus());
        Assert.assertFalse(serviceLocator.isRunning());
    }
}
