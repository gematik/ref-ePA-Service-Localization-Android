/*
 * Copyright (c) 2019 gematik - Gesellschaft für Telematikanwendungen der Gesundheitskarte mbH
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

package de.gematik.ti.fdv.android.epa.service.localization;

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

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import android.net.DnsResolver;

import de.gematik.ti.fdv.epa.service.localization.api.LookupStatus;
import de.gematik.ti.fdv.epa.service.localization.api.ServiceInterfaceName;
import de.gematik.ti.fdv.epa.service.localization.spi.IServiceLocalizer;

/**
 * include::{userguide}/ESL4A_Structure.adoc[tag=ServiceLocator]
 */
public final class ServiceLocator implements IServiceLocalizer {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceLocator.class);
    private static final String SCHEME = "https://";
    private static final int PORT = 443;
    private final Map<String, GatewayModulePathType> dnsTxtRecordValues = new HashMap();

    private boolean running = false;
    private Record[] records;
    private LookupStatus lookupStatus = LookupStatus.NOT_STARTED;
    private String fqdn;

    /**
     * Constructor
     */
    public ServiceLocator() {
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
        if (running) {
            return;
        }
        running = true;
        records = null;
        dnsTxtRecordValues.clear();

        this.fqdn = fqdn;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        DnsResolver resolver = DnsResolver.getInstance();
        DnsResolver.Callback resolverCallback = initializeResolverCallback();

        if (resolver != null) {
            resolver.rawQuery(null, fqdn, resolver.CLASS_IN, Type.TXT, resolver.FLAG_EMPTY, executor, null, resolverCallback);
        }
        running = false;
        callback.accept(lookupStatus);
    }

    @NotNull
    private DnsResolver.Callback initializeResolverCallback() {
        return new DnsResolver.Callback() {

            @Override
            public void onAnswer(final Object o, final int i) {
                try {
                    Message message = new Message((byte[]) o);
                    records = message.getSectionArray(Section.ANSWER);
                    if (records.length > 0) {
                        fillDnsTxtRecordValues(records);
                    }
                } catch (IOException e) {
                    LOG.error("Error on create records from resolve object" + e);
                }
            }

            @Override
            public void onError(final DnsResolver.DnsException e) {
                lookupStatus = LookupStatus.ERROR;
            }
        };
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
        boolean isValid = handleTTL(serviceInterfaceName.getModuleName());
        if (lookupStatus.equals(LookupStatus.SUCCESS) && isValid) {
            String path = dnsTxtRecordValues.get(serviceInterfaceName.getModuleName()).getPath();
            try {
                URL url = new URL(SCHEME + fqdn + ":" + PORT + path + "/" + serviceInterfaceName.getServiceLocatorName());
                LOG.debug("endpointURLForInterface for Interface " + serviceInterfaceName.getServiceLocatorName() + ": " + url);
                return url;
            } catch (MalformedURLException e) {
                LOG.error("Malformed URL received from module " + path + e.getMessage());
            }
        }
        return null;
    }

    void fillDnsTxtRecordValues(final Record[] records) {
        lookupStatus = LookupStatus.SUCCESS;
        for (Record record : records) {
            String rData = record.rdataToString().replaceAll("\"", "");
            List<String> rDataTokens = Arrays.asList(rData.split("\\s+"));

            TXTRecord txtRecord = new TXTRecord(record.getName(), record.getDClass(), record.getTTL(), rDataTokens);
            List<String> recordStrings = txtRecord.getStrings();

            if (recordStrings != null && recordStrings.size() > 0) {
                for (String string : recordStrings) {
                    if (string.contains("=")) {
                        String[] splitToken = string.trim().split("=", 2);
                        fillModuleList(record, splitToken);
                    }
                }
            }
        }
    }

    private void fillModuleList(final Record record, final String[] splitToken) {
        if (splitToken.length == 2) {
            String txtRecordName = splitToken[0];
            if ("txtvers".equals(txtRecordName)) {
                checkTxtVersion(splitToken[1]);
            } else {
                GatewayModulePathType gatewayModulePathType = new GatewayModulePathType(splitToken[1], record.getTTL());
                dnsTxtRecordValues.put(splitToken[0], gatewayModulePathType);
                LOG.debug("TxtRecordName: " + txtRecordName + " gatewayModulePath: " + gatewayModulePathType.getPath() + " valid until: "
                        + gatewayModulePathType.getValidUntil().toString());
            }
        }
    }

    private void checkTxtVersion(final String version) {
        if (!"1".equals(version)) {
            throw new RuntimeException("wrong txtVersion");
        }
    }

    /**
     * Returns the status of service lookup in DNS
     * 
     * @return status of service lookup in DNS
     */
    @Override
    public LookupStatus getLookupStatus() {
        if (!running) {
            return lookupStatus;
        } else {
            return LookupStatus.IN_PROGRESS;
        }
    }

    /**
     * Returns home community (OID, which the file system provider has requested from DIMDI)
     * 
     * @return home community ID
     */
    public String getHomeCommunityID() {
        boolean isValid = handleTTL("hcid");
        if (isValid) {
            return dnsTxtRecordValues.get("hcid").getPath();
        } else {
            return null;
        }
    }

    private boolean handleTTL(final String txtRecordName) {
        Date validUntil = dnsTxtRecordValues.get(txtRecordName).getValidUntil();
        return validUntil.getTime() >= new Date().getTime();
    }
}
