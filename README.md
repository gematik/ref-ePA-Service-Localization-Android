de.gematik.ti.epa.android.fdv.service.localization

# ePA-Service-Localization-Android

# Introduction

This part describes the usage of ePA Service Localization in your application.

# API Documentation

Generated API docs are available at <https://gematik.github.io/ref-ePA-Service-Localization-Android>.

# License

Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

# Overview

![ePA Service Localization Android structure](../images/esl4a/generated/localization.png)

  

## ServiceLocator

The ServiceLocator class implements the IServiceLocalizer interface from ePA service localization api and provides methods to start a new DNS lookup, e.g. if previous one ended erroneously,
to get the endpoint URL for a given interface name after a successful lookup, the status of
service lookup in DNS and the home community (OID, which the file system provider has requested from DIMDI).
The implementation in Android should be asynchronous as in the following example:

        private class DnsTxtRecord {
            private final String fqdn;

            public DnsTxtRecord(String fqdn) {
                this.fqdn = fqdn;
            }

            @SuppressLint("StaticFieldLeak")
            public DnsTxtRecord invoke() {
                new AsyncTask<Void,Void,Void>(){

                    @Override
                    protected Void doInBackground(Void... voids) {
                        IServiceLocator locator = new ServiceLocatorFactory.getInstance();
                        locator.lookup(fqdn);

                        LookupStatus lookupStatus = locator.getLookupStatus();
                        URL endpoint = locator.endpointURLForInterface(ServiceInterfaceName.IAccountManagementInsurant);
                        String hcid = locator.getHomeCommunityID();

                        this.cancel(true);
                        return null;
            }

        }.execute();

                return null;
            }
        }

## GatewayModulePathType

The GatewayModulePathType object represents the properties (path, time to live, valid until) of an ePA service module.

# Getting Started

## Build setup

To use ePA Service Localization Android library in a project, you need just to include following dependency:

**Gradle dependency settings to use ePA Service Localization Androidlibrary.**

    dependencies {
        implementation group: 'de.gematik.ti.fdv.android', name: 'epa.service.localization', version: '1.1.0'
    }

**Maven dependency settings to use ePA Service Localization Android library.**

    <dependencies>
        <dependency>
            <groupId>de.gematik.ti.fdv.android</groupId>
            <artifactId>epa.service.localization</artifactId>
            <version>1.1.0</version>
        </dependency>
    </dependencies>
