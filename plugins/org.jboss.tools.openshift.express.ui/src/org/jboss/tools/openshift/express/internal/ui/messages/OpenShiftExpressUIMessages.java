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

	public static String CANNOT_CREATE_NO_USER;
	public static String CREATING_APPLICATION;
	public static String COULD_NOT_CREATE_APPLICATION;
	public static String OPERATION_CANCELLED;
	public static String WAITING_FOR_REACHABLE;
	public static String APPLICATION_NOT_ANSWERING;
	public static String APPLICATION_NOT_ANSWERING_CONTINUE_WAITING;
	public static String BTN_KEEP_WAITING;
	public static String BTN_CLOSE_WIZARD;
	public static String RESTARTING_APPLICATION;
	
	public static String ADDING_REMOVING_CARTRIDGES;
	
	public static String TAIL_SERVER_LOG_ACTION;
	public static String CREATE_OR_EDIT_DOMAIN_ACTION;
	public static String DELETE_DOMAIN_ACTION;
	public static String SHOW_IN_ACTION_GROUP;
	public static String SHOW_IN_BROWSER_ACTION;
	public static String DELETE_APPLICATION_ACTION;
	public static String CREATE_APPLICATION_ACTION;
	public static String IMPORT_APPLICATION_ACTION;
	public static String CREATE_SERVER_ADAPTER_ACTION;
	public static String EDIT_CARTRIDGES_ACTION;
	public static String MAKE_SNAPSHOT_ACTION;
	public static String SHOW_PROPERTIES_VIEW_ACTION;
	public static String SHOW_ENVIRONMENT_ACTION;
	public static String SHOW_DETAILS_ACTION;
	public static String REFRESH_VIEWER_ACTION;
	public static String REFRESH_USER_ACTION;
	public static String DELETE_CONNECTION_ACTION;
	public static String RESTART_APPLICATION_ACTION;
	
	public static String USER_NOT_CONNECTED_LABEL;
	
	public static String LOADING_USER_APPLICATIONS_LABEL;

}
