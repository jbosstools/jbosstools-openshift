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

}
