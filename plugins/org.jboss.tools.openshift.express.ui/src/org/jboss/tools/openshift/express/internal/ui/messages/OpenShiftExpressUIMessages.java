package org.jboss.tools.openshift.express.internal.ui.messages;

import org.eclipse.osgi.util.NLS;

public class OpenShiftExpressUIMessages extends NLS {

	private static final String BUNDLE_NAME = OpenShiftExpressUIMessages.class.getName();

	static {
		NLS.initializeMessages(BUNDLE_NAME, OpenShiftExpressUIMessages.class);
	}

	private OpenShiftExpressUIMessages() {
		// Do not instantiate
	}

	public static String HOSTNAME_NOT_ANSWERING;

	public static String TAIL_SERVER_LOG_ACTION;

	public static String CREATE_OR_EDIT_DOMAIN_ACTION;
	
	public static String SHOW_IN_ACTION_GROUP;
	
	public static String SHOW_IN_BROWSER_ACTION;
	
	public static String DELETE_APPLICATION_ACTION;
	
	public static String CREATE_APPLICATION_ACTION;
	
	public static String IMPORT_APPLICATION_ACTION;
	
	public static String CREATE_SERVER_ADAPTER_ACTION;
	
	public static String EDIT_CARTRIDGES_ACTION;
	
	public static String MAKE_SNAPSHOT_ACTION;

	public static String SHOW_PROPERTIES_VIEW_ACTION;


}
