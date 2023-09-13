/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.common.ui.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.jboss.tools.openshift.internal.test.OpenShiftTestActivator;
import org.jboss.tools.openshift.internal.ui.utils.DownloadHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class OdoDownloadHelperTest {

	public static final String ODO_CACHE_DIR = System.getProperty("user.home") + File.separatorChar + ".odo-tests"; //$NON-NLS-1$ //$NON-NLS-2$

	@BeforeClass
	public static void setup() throws IOException {
		FileUtils.deleteDirectory(new File(ODO_CACHE_DIR));
	}

	@AfterClass
	public static void teardown() throws IOException {
		FileUtils.deleteDirectory(new File(ODO_CACHE_DIR));
	}

	@Test
	public void testThatODOisDownloaded() throws IOException {
		DownloadHelper originalHelper = DownloadHelper.getInstance();
		DownloadHelper helper = spy(originalHelper);
		doReturn(Boolean.TRUE).when(helper).isDownloadAllowed("odo", "", "1.0.2");
		URL url = OpenShiftTestActivator.getDefault().getBundle().getEntry("/resources/test-tools.json");
		assertNotNull(url);
		String cmd = helper.downloadIfRequired("odo", url);
		assertNotNull(cmd);
		assertEquals(ODO_CACHE_DIR + File.separatorChar + "cache" + File.separatorChar + "1.0.2" + File.separatorChar
				+ "odo", cmd);
	}
}
