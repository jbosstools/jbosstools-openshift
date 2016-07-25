/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.client.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class OpenShiftClientActivator extends Plugin {
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		//Eclipse finds multiple SLF4J bindings in its classpath and we can't control the Jetty logging behavior then.
		//Particularly, WebSockets pollutes the Eclipse log really badly (JBIDE-21596)
		//Until we find a proper way to fix the problem, we take the fugly approach.
		ClassLoader orig=Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader cl = getClass().getClassLoader();
			//Change class loader to make jetty-logging.properties available and 
			//force Jetty to use the StrErrLog logger, where we can control the logging level
			Thread.currentThread().setContextClassLoader(cl);
		} finally {
			Thread.currentThread().setContextClassLoader(orig);
		}
	}
}
