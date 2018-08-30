/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.server.runtime;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.cdk.reddeer.core.enums.CDKVersion;
import org.jboss.tools.cdk.reddeer.core.runtime.Runtime;
import org.jboss.tools.cdk.reddeer.core.server.ServerAdapter;
import org.jboss.tools.cdk.reddeer.requirements.RemoveCDKServersRequirement.RemoveCDKServers;
import org.jboss.tools.cdk.reddeer.server.ui.runtime.RuntimeDetectionPreferencePage;
import org.jboss.tools.cdk.reddeer.server.ui.runtime.SearchingForRuntimesDialog;
import org.jboss.tools.cdk.reddeer.utils.CDKUtils;
import org.jboss.tools.cdk.ui.bot.test.CDKAbstractTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Abstract template class providing testing of runtime detection via preferences 
 * @author odockal
 *
 */
@RemoveCDKServers
@RunWith(RedDeerSuite.class)
public abstract class CDKRuntimeDetectionTemplate extends CDKAbstractTest {

	private static final String JBOSS_RUNTIMES = System.getProperty("user.home") + separator + "jboss-runtimes";
	
	private static final Path JBOSS_RUNTIMES_PATH = Paths.get(JBOSS_RUNTIMES);
	
	private Path newBinaryPath;
	
	private static final Logger log = Logger.getLogger(CDKRuntimeDetectionTemplate.class);
	
	public Path getDownloadPath() {
		return getServerAdapter().getMinishiftBinary();
	}
	
	public CDKVersion getCDKVersion() {
		return getServerAdapter().getVersion();
	}
	
	public String getCDKServerAdapterName() {
		return getServerAdapter().getAdapterName();
	}
	
	public abstract ServerAdapter getServerAdapter();
	
	private void copyMinishiftBinaryToJbossRuntimesDirectory() {
		try {
			Path from = getDownloadPath();
			Path to = JBOSS_RUNTIMES_PATH.resolve(from.getFileName());
			log.info("Copying binary over from " + from.toString() + " to " + to.toString());
			Files.createFile(to);
			newBinaryPath = Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
			log.info("Binary copied succesfully to " + newBinaryPath.toString());
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Before
	public void configureEnvironment() {
		if (!Files.exists(JBOSS_RUNTIMES_PATH)) {
			JBOSS_RUNTIMES_PATH.toFile().mkdirs();
		}
		copyMinishiftBinaryToJbossRuntimesDirectory();
	}
	
	@Test
	public void testSearchingCDKRuntimes() {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		RuntimeDetectionPreferencePage page = new RuntimeDetectionPreferencePage(dialog);
		dialog.select(page);
		page.removePathContaining(".minishift");
		assertThat("Runtime detection prefs should have predefined jboss-runtimes path", page.getAllPaths(), hasItem(JBOSS_RUNTIMES));
		assertThat("Runtime detection prefs should not have set .minishift in paths in this test", page.getAllPaths(), not(hasItem(DEFAULT_MINISHIFT_HOME)));
		SearchingForRuntimesDialog searchDialog = page.search();
		List<Runtime> runtimes = searchDialog.getRuntimes();
		assertThat(runtimes.size(), is(1));
		Runtime runtime = runtimes.get(0);
		assertThat(runtime.getType(), is(getCDKVersion().type().runtimeTypeName()));
		assertThat(runtime.getLocation(), is(JBOSS_RUNTIMES));
		searchDialog.ok();
		dialog.ok();
		Server server = CDKUtils.getAllServers().get(0);
		assertTrue(CDKUtils.isServerOfType(server, getCDKVersion().type().serverType()));
		assertThat(server.getLabel().getName(), is(getCDKServerAdapterName()));
	}
	
	@After
	public void cleanUpEnv() {
		CDKUtils.deleteCDKServerAdapter(getCDKServerAdapterName());
		try {
			Files.deleteIfExists(Paths.get(JBOSS_RUNTIMES, getDownloadPath().getFileName().toString()));
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
}
