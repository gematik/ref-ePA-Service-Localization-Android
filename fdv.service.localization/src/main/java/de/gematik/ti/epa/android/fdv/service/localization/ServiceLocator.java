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

import java.net.URL;
import java.util.function.Consumer;

import android.os.Build;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;
import de.gematik.ti.epa.fdv.service.localization.api.ServiceInterfaceName;
import de.gematik.ti.epa.fdv.service.localization.spi.IServiceLocalizer;

/**
 * include::{userguide}/ESL4A_Overview.adoc[tag=ServiceLocator]
 */
public final class ServiceLocator implements IServiceLocalizer {

    private final IServiceLocalizer abstractServiceLocator;

    /**
     * Constructor
     */
    public ServiceLocator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            abstractServiceLocator = new ServiceLocatorV10();
        } else {
            abstractServiceLocator = new ServiceLocatorV9();
        }
    }

    /**
     * start a new DNS lookup, e.g. if previous one ended erroneously
     * 
     * @param fqdn
     *            fully qualified domain name
     * @param callback
     *            optional Consumer parameter to get callback the lookup status
     */
    @Override
    public void lookup(final String fqdn, final Consumer<LookupStatus> callback) {
        abstractServiceLocator.lookup(fqdn, callback);
    }

    @Override
    public URL endpointURLForInterface(final ServiceInterfaceName serviceInterfaceName) {
        return abstractServiceLocator.endpointURLForInterface(serviceInterfaceName);
    }

    @Override
    public LookupStatus getLookupStatus() {
        return abstractServiceLocator.getLookupStatus();
    }

    @Override
    public String getHomeCommunityId()  {
        return abstractServiceLocator.getHomeCommunityId();
    }
}
