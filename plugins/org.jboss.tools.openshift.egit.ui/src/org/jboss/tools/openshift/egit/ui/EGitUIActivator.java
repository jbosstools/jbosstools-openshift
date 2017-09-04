package org.jboss.tools.openshift.egit.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class EGitUIActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.jboss.tools.openshift.egit.ui"; //$NON-NLS-1$

    private static EGitUIActivator plugin;

    public EGitUIActivator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static EGitUIActivator getDefault() {
        return plugin;
    }

}
