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

import static org.mockito.ArgumentMatchers.anyString;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Assert;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.hla.DnssecResolverApi;
import org.minidns.hla.ResolverResult;
import org.minidns.record.Data;
import org.minidns.record.Record;
import org.minidns.record.TXT;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;
import de.gematik.ti.epa.fdv.service.localization.api.ServiceInterfaceName;
import de.gematik.ti.epa.fdv.service.localization.spi.IServiceLocalizer;

abstract class AbstractServiceLocatorTestTest {
    protected static final String FQDN = "My.test.fqdn";
    protected static final String RECORD_TXT = "\"txtvers=1\" \"hcid=1.2.276.0.76.3.1.91\" \"authn=/authn\" \"authz=/authz\" \"avzd=/avzd\" \"docv=/docv\" \"ocspf=/ocspf\" \"avzd=/avzd\" \"sgd1=/sgd1\" \"sgd2=/sgd2\"";
    protected static final long TTL = 200;
    protected static final String FQDN_ABSOLUTE = "My.test.fqdn.";
    protected static final int TYPE_TXT = 16;
    protected AbstractServiceLocator serviceLocator;
    protected Consumer<LookupStatus> callback;

    protected static void setFinalField(Class<?> clazz, String fieldName, Object value, Object ins) throws ReflectiveOperationException {

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(ins, value);
    }

    protected void checkUrls(final IServiceLocalizer serviceLocator) {
        Assert.assertEquals(LookupStatus.SUCCESS, serviceLocator.getLookupStatus());
        final String hicd = serviceLocator.getHomeCommunityId();
        Assert.assertEquals("1.2.276.0.76.3.1.91", hicd);

        final URL authnInsurantUrl = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_AUTHENTICATION_INSURANT);
        Assert.assertEquals("https://My.test.fqdn:443/authn/I_Authentication_Insurant", authnInsurantUrl.toString());

        final URL authzInsurantUrl = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_AUTHORIZATION_INSURANT);
        Assert.assertEquals("https://My.test.fqdn:443/authz/I_Authorization_Insurant", authzInsurantUrl.toString());

        final URL authzManagementInsurantUrl = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_AUTHORIZATION_MANAGEMENT_INSURANT);
        Assert.assertEquals("https://My.test.fqdn:443/authz/I_Authorization_Management_Insurant", authzManagementInsurantUrl.toString());

        final URL accountManagementInsurantUrl = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_ACCOUNT_MANAGEMENT_INSURANT);
        Assert.assertEquals("https://My.test.fqdn:443/docv/I_Account_Management_Insurant", accountManagementInsurantUrl.toString());

        final URL documentManagementConnectUrl = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_DOCUMENT_MANAGEMENT_CONNECT);
        Assert.assertEquals("https://My.test.fqdn:443/docv/I_Document_Management_Connect", documentManagementConnectUrl.toString());

        final URL documentManagementInsurantUrl = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_DOCUMENT_MANAGEMENT_INSURANT);
        Assert.assertEquals("https://My.test.fqdn:443/docv/I_Document_Management_Insurant", documentManagementInsurantUrl.toString());

        final URL oscpStatusInformationUrl = serviceLocator.endpointURLForInterface(ServiceInterfaceName.IOCSP_STATUS_INFORMATION);
        Assert.assertEquals("https://My.test.fqdn:443/ocspf/I_OCSP_Status_Information", oscpStatusInformationUrl.toString());

        final URL proxyDirectoryQueryUrl = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_PROXY_DIRECTORY_QUERY);
        Assert.assertEquals("https://My.test.fqdn:443/avzd/I_Proxy_Directory_Query", proxyDirectoryQueryUrl.toString());

        final URL sgd1Url = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_GET_KEY_SGD_1);
        Assert.assertEquals("https://My.test.fqdn:443/sgd1", sgd1Url.toString());

        final URL sgd2Url = serviceLocator.endpointURLForInterface(ServiceInterfaceName.I_GET_KEY_SGD_2);
        Assert.assertEquals("https://My.test.fqdn:443/sgd2", sgd2Url.toString());
    }

    protected void initServiceLocatorV9(final AbstractServiceLocator serviceLocator, boolean withRecords)
            throws ReflectiveOperationException, java.io.IOException {
        Field dnssecResolverApi = ServiceLocatorV9.class.getDeclaredField("dnssecResolverApi");
        dnssecResolverApi.setAccessible(true);
        DnssecResolverApi dnssecResolverApiMock = Mockito.mock(DnssecResolverApi.class);
        dnssecResolverApi.set(serviceLocator, dnssecResolverApiMock);

        ResolverResult<TXT> resultTxt = Mockito.mock(ResolverResult.class);
        Set<TXT> resultTxtAnswers = new HashSet<>();
        if (withRecords) {
            TXT txt = Mockito.mock(TXT.class);
            Mockito.when(txt.getText()).thenReturn(RECORD_TXT);
            resultTxtAnswers.add(txt);
        }
        Mockito.when(resultTxt.getAnswers()).thenReturn(resultTxtAnswers);
        DnsMessage dnsMessage = Mockito.mock(DnsMessage.class);
        List<Record<? extends Data>> additionalSection = new ArrayList<>();

        Record record = Mockito.mock(Record.class);
        setFinalField(Record.class, "ttl", TTL, record);
        additionalSection.add(record);
        setFinalField(DnsMessage.class, "additionalSection", additionalSection, dnsMessage);
        Mockito.when(resultTxt.getRawAnswer()).thenReturn(dnsMessage);
        Mockito.when(dnssecResolverApiMock.resolve(anyString(), ArgumentMatchers.<Class<TXT>> any())).thenReturn(resultTxt);
    }
}
