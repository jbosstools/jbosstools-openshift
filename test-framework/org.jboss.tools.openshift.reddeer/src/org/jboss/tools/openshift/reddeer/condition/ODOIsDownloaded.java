/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import java.io.File;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;

/**
 * Wait condition to wait for ODO download.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class ODOIsDownloaded extends AbstractWaitCondition {

	public static final String ODO_CACHE_DIR = System.getProperty("user.home") + File.separatorChar + ".odo"
			+ File.separatorChar + "cache" + File.separatorChar + "1.2.6" + File.separatorChar + "odo";
	public String odoPath = "";

	/**
	 * Constructs OdoIsDownloaded wait condition. Condition is met when odo is
	 * downloaded. Default ODO location is used.
	 * 
	 */
	public ODOIsDownloaded() {
		this.odoPath = ODO_CACHE_DIR;
	}

	/**
	 * Constructs OdoIsDownloaded wait condition. Condition is met when odo is
	 * downloaded.
	 * 
	 * @param path path to odo
	 */
	public ODOIsDownloaded(String path) {
		this.odoPath = path;
	}

	@Override
	public boolean test() {
		File tempFile = new File(ODO_CACHE_DIR);
		return tempFile.exists();
	}

	@Override
	public String description() {
		return "ODO is downloaded";
	}

}
