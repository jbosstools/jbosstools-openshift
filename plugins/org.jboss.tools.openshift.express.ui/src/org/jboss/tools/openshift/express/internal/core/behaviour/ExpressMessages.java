package org.jboss.tools.openshift.express.internal.core.behaviour;

import org.eclipse.osgi.util.NLS;

public class ExpressMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.openshift.express.internal.core.behaviour.expressMessages"; //$NON-NLS-1$
	
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ExpressMessages.class);		
	}
	public static String requestCommitAndPushTitle;
	public static String requestCommitAndPushMsg;
	public static String requestPushTitle;
	public static String requestPushMsg;
	public static String cannotModifyModules;
	public static String shareProjectTitle;
	public static String shareProjectMessage;	
	
}
