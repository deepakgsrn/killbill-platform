/*
 * Copyright 2016 Groupon, Inc
 * Copyright 2016 The Billing Project, LLC
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

package org.killbill.killbill.osgi.libs.killbill;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class OSGIKillbillServiceReference implements ServiceReference {

    private static final String MDC_KEY = "MDC";

    private final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    private final ServiceReference delegate;

    public OSGIKillbillServiceReference(final ServiceReference delegate, final Map<String, String> mdcMap) {
        this.delegate = delegate;
        if (delegate != null) {
            for (final String property : delegate.getPropertyKeys()) {
                properties.put(property, delegate.getProperty(property));
            }
        }
        properties.put(MDC_KEY, mdcMap);
    }

    @Override
    public Object getProperty(final String key) {
        return properties.get(key);
    }

    @Override
    public String[] getPropertyKeys() {
        final Set<String> s = properties.keySet();
        return s.toArray(new String[s.size()]);
    }

    @Override
    public Bundle getBundle() {
        return delegate == null ? null : delegate.getBundle();
    }

    @Override
    public Bundle[] getUsingBundles() {
        return delegate == null ? null : delegate.getUsingBundles();
    }

    @Override
    public boolean isAssignableTo(final Bundle bundle, final String className) {
        return delegate != null && delegate.isAssignableTo(bundle, className);
    }

    @Override
    public int compareTo(final Object reference) {
        return delegate == null ? -1 : delegate.compareTo(reference);
    }
}
