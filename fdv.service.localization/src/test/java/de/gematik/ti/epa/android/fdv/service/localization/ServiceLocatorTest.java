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

import org.awaitility.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;

import static org.awaitility.Awaitility.await;

public class ServiceLocatorTest extends AbstractServiceLocatorTestTest {

    private ServiceLocator serviceLocator;
    private boolean ready = false;

    @Before
    public void setUp() throws Exception {
        callback = lookupStatus -> {
            ready = true;
        };
        serviceLocator = new ServiceLocator();
        AbstractServiceLocator abstractServiceLocator = new ServiceLocatorV9();
        initServiceLocatorV9(abstractServiceLocator,true);
        setFinalField(ServiceLocator.class, "abstractServiceLocator", abstractServiceLocator, serviceLocator);
    }

    @Test
    public void endpointURLForInterface() {
        lookup();
        checkUrls(serviceLocator);
    }

    @Test
    public void getLookupStatus() {
        lookup();
        Assert.assertEquals(LookupStatus.SUCCESS, serviceLocator.getLookupStatus());

    }

    private void lookup() {
        serviceLocator.lookup(FQDN, callback);
        await().atMost(Duration.TEN_SECONDS).with().pollInterval(Duration.TWO_HUNDRED_MILLISECONDS).until(() -> ready);
    }
}
