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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import android.net.DnsResolver;

import de.gematik.ti.epa.fdv.service.localization.api.LookupStatus;
import de.gematik.ti.epa.fdv.service.localization.exceptions.ServiceLocatorException;

/**
 * Test {@link ServiceLocator}
 */
public class ServiceLocatorV10CallbackTest extends AbstractServiceLocatorTestTest {

    private DnsResolver.Callback<? super byte[]> dnsCallback;
    private AtomicReference<LookupStatus> reveivedCallBack;

    @Before
    public void init() throws Exception {
        reveivedCallBack = new AtomicReference<>();
        Consumer<LookupStatus> callback2 = reveivedCallBack::set;

        serviceLocator = new ServiceLocatorV10();

        Method initializeResolverCallback = ServiceLocatorV10.class.getDeclaredMethod("initializeResolverCallback", Consumer.class);
        initializeResolverCallback.setAccessible(true);
        dnsCallback = (DnsResolver.Callback<? super byte[]>) initializeResolverCallback.invoke(serviceLocator, callback2);

    }

    @Test
    public void testOnError() {
        dnsCallback.onError(Mockito.mock(DnsResolver.DnsException.class));
        Assert.assertEquals(LookupStatus.ERROR, serviceLocator.getLookupStatus());
        Assert.assertFalse(serviceLocator.isRunning());
        Assert.assertNotNull(reveivedCallBack.get());
        Assert.assertEquals(LookupStatus.ERROR, reveivedCallBack.get());

    }

    @Test
    public void testStopByInternalExceptions() {
        try {
            dnsCallback.onAnswer(new byte[0], 0);
        } catch (Exception e) {
            // Nothing in this TestCase
        }

        Assert.assertEquals(LookupStatus.ERROR, serviceLocator.getLookupStatus());
        Assert.assertFalse(serviceLocator.isRunning());
        Assert.assertNotNull(reveivedCallBack.get());
        Assert.assertEquals(LookupStatus.ERROR, reveivedCallBack.get());
    }

    @Test
    public void testOnAnswer() throws Exception {
        Message message = new Message();

        final Name current = Name.fromString(FQDN_ABSOLUTE);
        final Record answer = Record.fromString(current, TYPE_TXT, DClass.ANY, 0x20000, RECORD_TXT, current);
        message.addRecord(answer, Section.ANSWER);

        dnsCallback.onAnswer(message.toWire(), 0);

        Assert.assertEquals(LookupStatus.SUCCESS, serviceLocator.getLookupStatus());
        Assert.assertFalse(serviceLocator.isRunning());
        Assert.assertNotNull(reveivedCallBack.get());
        Assert.assertEquals(LookupStatus.SUCCESS, reveivedCallBack.get());
    }

    @Test(expected = ServiceLocatorException.class)
    public void testExceptions() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        dnsCallback.onAnswer(new byte[0], 0);
    }
}
