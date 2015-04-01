package org.jboss.tools.openshift.express.internal.ui.messages;

import org.eclipse.osgi.util.NLS;

public class ExpressUIMessages extends NLS {

	private static final String BUNDLE_NAME = ExpressUIMessages.class.getName();

	static {
		NLS.initializeMessages(BUNDLE_NAME, ExpressUIMessages.class);
	}

	private ExpressUIMessages() {
		// Do not instantiate
	}

	public static String CANNOT_CREATE_NO_USER;
	public static String CREATING_APPLICATION;
	public static String CREATING_APPLICATION_WITH_EMBEDDED;
	public static String COULD_NOT_CREATE_APPLICATION;
	public static String DESTROYING_DOMAIN;
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
	public static String EDIT_CARTRIDGES_ACTION;
	public static String MAKE_SNAPSHOT_ACTION;
	public static String REFRESH_USER_ACTION;
	public static String DELETE_CONNECTION_ACTION;
	public static String RESTART_APPLICATION_ACTION;
	
	public static String USER_NOT_CONNECTED_LABEL;
	
	public static String LOADING_USER_APPLICATIONS_LABEL;
	
	public static String MANAGE_SSH_KEYS_WIZARD_PAGE;
	public static String MANAGE_SSH_KEYS_WIZARD_PAGE_DESCRIPTION;
	public static String SSH_PUBLIC_KEYS_GROUP;
	public static String ADD_EXISTING_BUTTON;
	public static String NEW_BUTTON;
	public static String REMOVE_BUTTON;
	public static String REFRESH_BUTTON;
	public static String SSH_PREFS_LINK;
	public static String REMOVE_SSH_KEY_DIALOG_TITLE;
	public static String REMOVE_SSH_KEY_QUESTION;
	
	public static String REMOVE_SSH_KEY_JOB;
	public static String REFRESH_SSH_KEYS_JOB;
	public static String REFRESH_VIEWER_JOB;

	public static String COULD_NOT_REMOVE_SSH_KEY;
	public static String COULD_NOT_LOAD_SSH_KEYS;
	public static String COULD_NOT_REFRESH_SSH_KEYS;
	public static String COULD_NOT_REFRESH_VIEWER;

}
