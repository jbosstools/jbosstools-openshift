package org.jboss.tools.openshift.cdk.server.test;

import org.eclipse.ui.plugin.AbstractUIPlugin;

public class CDKTestActivator extends AbstractUIPlugin {
	static CDKTestActivator instance;
	public static CDKTestActivator getDefault() {
		return instance;
	}
	public CDKTestActivator() {
		instance = this;
	}

}
