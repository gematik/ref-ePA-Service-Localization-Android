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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;
import de.gematik.ti.epa.fdv.service.localization.api.ServiceInterfaceName;
import de.gematik.ti.epa.fdv.service.localization.exceptions.ServiceLocatorException;
import de.gematik.ti.epa.fdv.service.localization.spi.IServiceLocalizer;

abstract class AbstractServiceLocator implements IServiceLocalizer {
    private static final String SCHEME = "https://";
    private static final int PORT = 443;
    private final Map<String, GatewayModulePathType> dnsTxtRecordValues = new HashMap();
    private boolean running = false;
    private LookupStatus lookupStatus = LookupStatus.NOT_STARTED;
    private String fqdn;

    /**
     * Returns the status of service lookup in DNS
     *
     * @return status of service lookup in DNS
     */
    public LookupStatus getLookupStatus() {
        if (!running) {
            return lookupStatus;
        } else {
            return LookupStatus.IN_PROGRESS;
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
        if (isRunning()) {
            return;
        }
        setRunning(true);
        clear();
        setFqdn(fqdn);
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        doResolve(fqdn, executor, callback);
    }

    protected abstract void doResolve(final String fqdn, final ThreadPoolExecutor executor, final Consumer<LookupStatus> callback);

    void handleAnswer(final Record[] records) {
        if (records.length > 0) {
            fillDnsTxtRecordValues(records);
        } else {
            setLookupStatus(LookupStatus.MISSING_TXT_RECORD);
        }
    }

    /**
     * After successful lookup, it returns endpointURL for a given interface name
     *
     * @param serviceInterfaceName
     *            name of the gateway interface where to get the URL for
     * @return URL of given interface name
     */
    @Override
    public URL endpointURLForInterface(final ServiceInterfaceName serviceInterfaceName) {
        final boolean isValid = handleTTL(serviceInterfaceName.getModuleName());
        if (lookupStatus.equals(LookupStatus.SUCCESS) && isValid) {
            final String path = dnsTxtRecordValues.get(serviceInterfaceName.getModuleName()).getPath();
            try {
                URL url;
                if(serviceInterfaceName.getServiceLocatorName().length() > 0) {
                    url = new URL(SCHEME + fqdn + ":" + PORT + path + "/" + serviceInterfaceName.getServiceLocatorName());
                } else {
                    url = new URL(SCHEME + fqdn + ":" + PORT + path);
                }
                return url;
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private void fillDnsTxtRecordValues(final Record[] records) {
        for (final Record record : records) {
            final String rData = record.rdataToString().replaceAll("\"", "");
            final List<String> rDataTokens = Arrays.asList(rData.split("\\s+"));

            final TXTRecord txtRecord = new TXTRecord(record.getName(), record.getDClass(), record.getTTL(), rDataTokens);
            final List<String> recordStrings = txtRecord.getStrings();

            if (recordStrings != null && !recordStrings.isEmpty()) {
                handleRecordsAsStrings(record, recordStrings);
            }
        }
        if (dnsTxtRecordValues.size() > 0) {
            lookupStatus = LookupStatus.SUCCESS;
        } else {
            setLookupStatus(LookupStatus.MISSING_TXT_RECORD);
        }
        setRunning(false);
    }

    private void handleRecordsAsStrings(final Record record, final List<String> recordStrings) {
        for (final String string : recordStrings) {
            if (string.contains("=")) {
                final String[] splitToken = string.trim().split("=", 2);
                fillModuleList(record, splitToken);
            }
        }
    }

    private void fillModuleList(final Record record, final String[] splitToken) {
        if (splitToken.length == 2) {
            final String txtRecordName = splitToken[0];
            if ("txtvers".equals(txtRecordName)) {
                checkTxtVersion(splitToken[1]);
            } else {
                final GatewayModulePathType gatewayModulePathType = new GatewayModulePathType(splitToken[1], record.getTTL());
                dnsTxtRecordValues.put(splitToken[0], gatewayModulePathType);
            }
        }
    }

    private void checkTxtVersion(final String version) {
        if (!"1".equals(version)) {
            throw new ServiceLocatorException("Wrong txtVersion in DNS Response found.");
        }
    }

    /**
     * Returns home community (OID, which the file system provider has requested from DIMDI)
     *
     * @return home community ID
     */
    @Override
    public String getHomeCommunityId() {
        final boolean isValid = handleTTL("hcid");
        if (isValid) {
            return dnsTxtRecordValues.get("hcid").getPath();
        } else {
            return null;
        }
    }

    private boolean handleTTL(final String txtRecordName) {
        final Date validUntil = dnsTxtRecordValues.get(txtRecordName).getValidUntil();
        return validUntil.getTime() >= new Date(System.currentTimeMillis()).getTime();
    }

    public boolean isRunning() {
        return running;
    }

    protected void setRunning(final boolean running) {
        this.running = running;
    }

    private void setFqdn(final String fqdn) {
        this.fqdn = fqdn;
    }

    void setLookupStatus(final LookupStatus lookupStatus) {
        this.lookupStatus = lookupStatus;
    }

    private void clear() {
        dnsTxtRecordValues.clear();
        fqdn = null;
    }

    protected void answerCallback(final Consumer<LookupStatus> callback) {
        setRunning(false);
        callback.accept(getLookupStatus());
    }
}
