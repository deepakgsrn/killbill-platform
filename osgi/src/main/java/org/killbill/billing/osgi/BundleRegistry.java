/*
 * Copyright 2015 Groupon, Inc
 * Copyright 2015 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.osgi;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.killbill.billing.osgi.api.DefaultPluginsInfoApi.DefaultPluginServiceInfo;
import org.killbill.billing.osgi.api.OSGIServiceDescriptor;
import org.killbill.billing.osgi.api.PluginInfo.PluginServiceInfo;
import org.killbill.billing.osgi.api.config.PluginConfigServiceApi;
import org.killbill.billing.osgi.pluginconf.PluginFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;

public class BundleRegistry {

    private final FileInstall fileInstall;
    private final Map<String, BundleWithMetadata> registry;

    // We keep track of those to maintain the ordering on start, but probably we don't need to
    private List<BundleWithConfig> bundleWithConfigs;

    @Inject
    public BundleRegistry(final PureOSGIBundleFinder osgiBundleFinder,
                          final PluginFinder pluginFinder, final PluginConfigServiceApi pluginConfigServiceApi) {
        this.fileInstall = new FileInstall(osgiBundleFinder, pluginFinder, pluginConfigServiceApi);
        this.registry = new HashMap<String, BundleWithMetadata>();
    }

    public void installBundles(final Framework framework) {
        bundleWithConfigs = fileInstall.installBundles(framework);
        for (final BundleWithConfig bundleWithConfig : bundleWithConfigs) {
            registry.put(getPluginName(bundleWithConfig), new BundleWithMetadata(bundleWithConfig));
        }
    }

    public void startBundles() {
        for (final BundleWithConfig bundleWithConfig : bundleWithConfigs) {
            final boolean started = fileInstall.startBundle(bundleWithConfig.getBundle());
            final BundleWithMetadata bundleWithMetadata = registry.get(getPluginName(bundleWithConfig));
        }
    }

    public Collection<BundleWithMetadata> getBundles() {
        return registry.values();
    }

    public String getPluginName(final Bundle bundle) {
        for (final BundleWithMetadata cur : registry.values()) {
            if (bundle.getSymbolicName().equals(cur.getBundle().getSymbolicName())) {
                return getPluginName(cur);
            }
        }
        return bundle.getSymbolicName();
    }

    public void registerService(final OSGIServiceDescriptor desc, final String serviceName) {
        for (final BundleWithMetadata cur : registry.values()) {
            if (desc.getPluginSymbolicName().equals(cur.getBundle().getSymbolicName())) {
                cur.register(desc.getRegistrationName(), serviceName);
            }
        }
    }

    public void unregisterService(final OSGIServiceDescriptor desc, final String serviceName) {
        for (final BundleWithMetadata cur : registry.values()) {
            if (desc.getPluginSymbolicName().equals(cur.getBundle().getSymbolicName())) {
                cur.unregister(desc.getRegistrationName(), serviceName);
            }
        }
    }

    private static String getPluginName(final BundleWithConfig bundleWithConfig) {
        return bundleWithConfig.getConfig() != null && bundleWithConfig.getConfig().getPluginName() != null ? bundleWithConfig.getConfig().getPluginName() : bundleWithConfig.getBundle().getSymbolicName();
    }

    public static class BundleWithMetadata extends BundleWithConfig {

        private final Set<PluginServiceInfo> serviceNames;

        public BundleWithMetadata(final BundleWithConfig bundleWithConfig) {
            super(bundleWithConfig.getBundle(), bundleWithConfig.getConfig());
            serviceNames = new HashSet<PluginServiceInfo>();
        }

        public String getPluginName() {
            return BundleRegistry.getPluginName(this);
        }

        public String getVersion() {
            return getConfig() != null ? getConfig().getVersion() : null;
        }

        public void register(final String registrationName, final String serviceTypeName) {
            serviceNames.add(new DefaultPluginServiceInfo(serviceTypeName, registrationName));
        }

        public void unregister(final String registrationName, final String serviceTypeName) {
            serviceNames.remove(new DefaultPluginServiceInfo(serviceTypeName, registrationName));
        }

        public Set<PluginServiceInfo> getServiceNames() {
            return serviceNames;
        }
    }
}