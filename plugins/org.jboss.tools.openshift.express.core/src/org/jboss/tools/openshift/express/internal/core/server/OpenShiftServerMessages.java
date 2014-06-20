/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.express.internal.core.server;

import org.eclipse.osgi.util.NLS;

public class OpenShiftServerMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.openshift.express.internal.core.server.OpenShiftServerMessages"; //$NON-NLS-1$
	
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, OpenShiftServerMessages.class);		
	}
	public static String publishTitle;
	public static String commitAndPushMsg;
	public static String noChangesPushAnywayMsg;
	public static String committedChangesNotPushedYet;
	public static String cannotModifyModules;
	public static String shareProjectTitle;
	public static String shareProjectMessage;	
	public static String additionNotRequiredModule;
	public static String publishFailMissingProject;
	public static String publishFailMissingFolder;
}
