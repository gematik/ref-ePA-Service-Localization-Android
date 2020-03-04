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

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import android.annotation.TargetApi;
import android.net.DnsResolver;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;
import de.gematik.ti.epa.fdv.service.localization.exceptions.ServiceLocatorException;
import de.gematik.ti.epa.fdv.service.localization.spi.IServiceLocalizer;

/**
 * include::{userguide}/ESL4A_Overview.adoc[tag=ServiceLocator]
 */
@TargetApi(29)
public final class ServiceLocatorV10 extends de.gematik.ti.epa.android.fdv.service.localization.AbstractServiceLocator implements IServiceLocalizer {

    /**
     * Constructor
     */
    public ServiceLocatorV10() {
        // Nothing
    }

    protected void doResolve(final String fqdn, final ThreadPoolExecutor executor, final Consumer<LookupStatus> callback) {
        final DnsResolver resolver = DnsResolver.getInstance();
        final DnsResolver.Callback<? super byte[]> resolverCallback = initializeResolverCallback(callback);
        resolver.rawQuery(null, fqdn, DnsResolver.CLASS_IN, Type.TXT, DnsResolver.FLAG_EMPTY, executor, null, resolverCallback);
    }

    @NotNull
    private DnsResolver.Callback<? super byte[]> initializeResolverCallback(final Consumer<LookupStatus> callback) {
        return new DnsResolver.Callback<byte[]>() {

            @Override
            public void onAnswer(final byte[] bytes, final int i) {
                try {
                    final Message message = new Message(bytes);
                    handleAnswer(message.getSectionArray(Section.ANSWER));
                } catch (final IOException e) {
                    setLookupStatus(LookupStatus.ERROR);
                    throw new ServiceLocatorException("Error on create records from resolve " + "object" + e.getMessage(), e);
                } finally {
                    answerCallback(callback);
                }
            }

            @Override
            public void onError(final DnsResolver.DnsException e) {
                setLookupStatus(LookupStatus.ERROR);
                answerCallback(callback);
            }

        };
    }



}
