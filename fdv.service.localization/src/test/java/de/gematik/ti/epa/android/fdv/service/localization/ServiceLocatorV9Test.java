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

import static org.awaitility.Awaitility.await;

import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;

public class ServiceLocatorV9Test extends AbstractServiceLocatorTestTest {
    private AtomicReference<LookupStatus> reveivedCallBack;

    @Before
    public void init() {
        reveivedCallBack = new AtomicReference<>();
        callback = reveivedCallBack::set;

        serviceLocator = new ServiceLocatorV9();
    }

    @Test
    public void doResolve() throws Exception {
        initServiceLocatorV9(serviceLocator, true);
        serviceLocator.lookup(FQDN, callback);
        await().atMost(Duration.FIVE_SECONDS).with().pollInterval(Duration.ONE_HUNDRED_MILLISECONDS).until(() -> reveivedCallBack.get() != null);
        checkUrls(serviceLocator);

        Assert.assertEquals(LookupStatus.SUCCESS, serviceLocator.getLookupStatus());
        Assert.assertFalse(serviceLocator.isRunning());
        Assert.assertNotNull(reveivedCallBack.get());
        Assert.assertEquals(LookupStatus.SUCCESS, reveivedCallBack.get());
    }

    @Test
    public void doResolveEmpty() throws Exception {
        initServiceLocatorV9(serviceLocator, false);
        serviceLocator.lookup(FQDN, callback);
        await().atMost(Duration.FIVE_SECONDS).with().pollInterval(Duration.ONE_HUNDRED_MILLISECONDS).until(() -> reveivedCallBack.get() != null);

        Assert.assertEquals(LookupStatus.MISSING_TXT_RECORD, serviceLocator.getLookupStatus());
        Assert.assertFalse(serviceLocator.isRunning());
        Assert.assertNotNull(reveivedCallBack.get());
        Assert.assertEquals(LookupStatus.MISSING_TXT_RECORD, reveivedCallBack.get());
    }

}
