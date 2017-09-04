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
package org.jboss.tools.openshift.internal.js;

import org.jboss.tools.foundation.core.plugin.BaseCorePlugin;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftNodejsActivator extends BaseCorePlugin {

    public static final String PLUGIN_ID = "org.jboss.tools.openshift.js"; //$NON-NLS-1$
    private static OpenShiftNodejsActivator instance;

    public OpenShiftNodejsActivator() {
        super();
        instance = this;
    }

    public IPluginLog getLogger() {
        return pluginLogInternal();
    }

    public static OpenShiftNodejsActivator getDefault() {
        return instance;
    }
}
