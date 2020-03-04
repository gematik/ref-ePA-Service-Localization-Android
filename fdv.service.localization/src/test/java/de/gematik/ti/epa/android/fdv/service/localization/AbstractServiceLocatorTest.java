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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;

public class AbstractServiceLocatorTest {

    @Test
    public void getLookupStatus() {
        AbstractServiceLocator asl = new AbstractServiceLocator() {

            @Override
            protected void doResolve(final String fqdn, final ThreadPoolExecutor executor, final Consumer<LookupStatus> callback) {
                // Not used
            }
        };

        Assert.assertEquals(LookupStatus.NOT_STARTED, asl.getLookupStatus());
        asl.setLookupStatus(LookupStatus.ERROR);
        Assert.assertEquals(LookupStatus.ERROR, asl.getLookupStatus());
    }
}
