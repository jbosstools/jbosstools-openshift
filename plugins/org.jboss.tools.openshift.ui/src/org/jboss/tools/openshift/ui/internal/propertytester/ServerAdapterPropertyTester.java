/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.internal.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;

import com.openshift.restclient.model.IResource;

/**
 * @author Jeff Maury
 *
 */
public class ServerAdapterPropertyTester extends PropertyTester {

    private final static String PROPERTY_IS_SERVER_ADAPTER_ALLOWED = "isServerAdapterAllowed";

    @Override
    public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
        if (PROPERTY_IS_SERVER_ADAPTER_ALLOWED.equals(property)) {
            return isServerAdapterAllowed(receiver, args, expectedValue);
        }
        return false;
    }

    private boolean isServerAdapterAllowed(Object receiver, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IResource)
                || !(expectedValue instanceof Boolean)) {
            return false;
        }
        IResource resource = (IResource) receiver;
        Boolean allowed = OpenShiftServerUtils.isAllowedForServerAdapter(resource);
        return ((Boolean) expectedValue).equals(allowed);
    }

}
