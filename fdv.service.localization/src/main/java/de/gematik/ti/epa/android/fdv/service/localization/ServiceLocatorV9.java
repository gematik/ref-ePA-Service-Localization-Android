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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsserverlookup.android21.AndroidUsingLinkProperties;
import org.minidns.hla.DnssecResolverApi;
import org.minidns.hla.ResolverResult;
import org.minidns.record.Data;
import org.minidns.record.TXT;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;
import de.gematik.ti.epa.fdv.service.localization.exceptions.ServiceLocatorException;

/**
 * include::{userguide}/ESL4A_Overview.adoc[tag=ServiceLocator]
 */
public class ServiceLocatorV9 extends AbstractServiceLocator {

    private static final String TAG = "ServiceLocator";
    private static final int DEFAULT_TTL = 30000;
    private DnssecResolverApi dnssecResolverApi = DnssecResolverApi.INSTANCE;

    public ServiceLocatorV9() {
        Context applicationContext = null;

        try {
            applicationContext = getApplicationUsingReflection1().getApplicationContext();
            Log.d(TAG, "Get applicationContext successful");
        } catch (Exception e) {
            Log.d(TAG, "Get applicationContext using Reflection failed, try other way.");
        }
        if (applicationContext == null) {
            try {
                applicationContext = getApplicationUsingReflection2().getApplicationContext();
                Log.d(TAG, "Get applicationContext on second way was successful");
            } catch (Exception e) {
                Log.d(TAG, "Get applicationContext using Reflection failed on second try.");
            }
        }
        if (applicationContext != null) {
            AndroidUsingLinkProperties.setup(applicationContext);
        } else {
            Log.e(TAG, "ServiceLocator for Android above API 29 could not initialize with application context. You can initialize with "
                    + "'AndroidUsingLinkProperties.setup(applicationContext);' before call lookup()");
        }
    }

    private Application getApplicationUsingReflection1() throws Exception {
        return (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
    }

    private Application getApplicationUsingReflection2() throws Exception {
        return (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null, (Object[]) null);
    }

    @Override
    protected void doResolve(final String fqdn, final ThreadPoolExecutor executor, final Consumer<LookupStatus> callback) {
        executor.execute(() -> {
            ResolverResult<TXT> resultTxt = null;
            List<Record> records = new ArrayList<>();
            try {
                resultTxt = dnssecResolverApi.resolve(fqdn, TXT.class);
                Set<TXT> resultTxtAnswers = resultTxt.getAnswers();
                DnsMessage rawAnswer = resultTxt.getRawAnswer();
                Optional<org.minidns.record.Record<? extends Data>> first = rawAnswer.additionalSection.stream().findFirst();
                long ttl = first.isPresent() ? first.get().ttl : DEFAULT_TTL;
                Name name = Name.fromString(fqdn + ".");
                for (TXT txt : resultTxtAnswers) {
                    records.add(Record.fromString(name, org.minidns.record.Record.TYPE.TXT.getValue(), DClass.ANY, ttl, txt.getText(), name));
                }
                handleAnswer(records.toArray(new Record[0]));
            } catch (Exception e) {
                setLookupStatus(LookupStatus.ERROR);
                throw new ServiceLocatorException("Error on resolve FQDN " + fqdn + " " + e.getMessage(), e);
            } finally {
                answerCallback(callback);
            }
        });
        executor.shutdown();
    }

}
