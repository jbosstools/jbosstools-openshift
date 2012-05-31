/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jboss.tools.openshift.express.internal.ui.utils.FileUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUserConfig;
import org.junit.Test;

import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public class SSHUserConfigTest {

	@Test
	public void shouldDetectIdentityFile() throws OpenShiftException, IOException {
		String config = 
				"# comment\n" +
				"Host *.redhat.com\n" +
				"    IdentityFile ~/.ssh/id_rsa_redhat\n" +
				"    VerifyHostKeyDNS yes\n" +
				"    StrictHostKeyChecking no\n" +
				"\n" +
				"# comment\n" +
				"Host *.dev.rhcloud.com\n" +
				"    IdentityFile ~/.ssh/libra_id_rsa_dev\n" +
				"    VerifyHostKeyDNS yes\n" +
				"    StrictHostKeyChecking no\n" +
				"    UserKnownHostsFile ~/.ssh/libra_known_hosts\n";
		SSHUserConfig sshUserConfig = getSSHUserConfig(config, createConfigFile(config));
		assertEquals("~/.ssh/libra_id_rsa_dev", sshUserConfig.getLibraIdentityFile());
	}

	@Test
	public void shouldNotDetectIdentityFile() throws OpenShiftException, IOException {
		String config = 
				"# comment\n" +
				"Host *.redhat.com\n" +
				"    IdentityFile ~/.ssh/id_rsa_redhat\n" +
				"    VerifyHostKeyDNS yes\n" +
				"    StrictHostKeyChecking no\n" +
				"\n" +
				"# comment\n" +
				"Host *.jboss.org\n" +
				"    IdentityFile ~/.ssh/libra_id_rsa_dev\n" +
				"    VerifyHostKeyDNS yes\n" +
				"    StrictHostKeyChecking no\n" +
				"    UserKnownHostsFile ~/.ssh/libra_known_hosts\n";
		SSHUserConfig sshUserConfig = getSSHUserConfig(config, createConfigFile(config));
		assertNull(sshUserConfig.getLibraIdentityFile());
	}

	private SSHUserConfig getSSHUserConfig(String configFileContent, File sshConfigFile) throws FileNotFoundException {
		return new SSHUserConfig(sshConfigFile);
	}

	private File createConfigFile(String configFileContent) throws FileNotFoundException {
		final File configFile = 
				new File(FileUtils.getSystemTmpFolder(), String.valueOf(System.currentTimeMillis()));
		FileUtils.writeTo(configFileContent, configFile);
		return configFile;
	}
}
