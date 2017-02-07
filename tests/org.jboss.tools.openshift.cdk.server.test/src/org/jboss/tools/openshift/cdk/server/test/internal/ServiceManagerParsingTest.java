/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.test.internal;

import java.util.HashMap;

import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerUtility;
import org.junit.Test;

import junit.framework.TestCase;

public class ServiceManagerParsingTest extends TestCase {
	private static final String LIN_CERT_PATH = "/home/rob/Downloads/cdk/20160608/cdk/components/rhel/rhel-ose/.vagrant/machines/default/virtualbox/docker";
	private static final String WIN_CERT_PATH = "C:\\my\\cdk\\components\\rhel\\rhel-ose\\.vagrant\\machines\\default\\virtualbox\\docker";
	@Test
	public void testServiceManager101Linux() throws Exception {
		String[] lines = serviceManager101Linux();
		HashMap<String, String> map = ServiceManagerUtility.parseLines(lines);
		ServiceManagerEnvironment env = new ServiceManagerEnvironment(map);
		serviceManager101(env, LIN_CERT_PATH);
	}
	@Test
	public void testServiceManager101Win() throws Exception {
		String[] lines = serviceManager101Win();
		HashMap<String, String> map = ServiceManagerUtility.parseLines(lines);
		ServiceManagerEnvironment env = new ServiceManagerEnvironment(map);
		serviceManager101(env, WIN_CERT_PATH);
	}
	
	@Test
	public void testServiceManagerWinSET() throws Exception {
		String[] lines = serviceManagerWinSET();
		HashMap<String, String> map = ServiceManagerUtility.parseLines(lines);
		ServiceManagerEnvironment env = new ServiceManagerEnvironment(map);
		serviceManager110(env, WIN_CERT_PATH);
	}
	
	
	private void serviceManager101(ServiceManagerEnvironment env, String certPath) throws Exception {
		assertEquals(env.get("DOCKER_HOST"), "tcp://10.1.2.2:2376");
		assertEquals(env.get("DOCKER_CERT_PATH"), certPath);
		assertEquals(env.get("DOCKER_TLS_VERIFY"), "1");
		assertEquals(env.get("DOCKER_API_VERSION"), "1.21");
		assertEquals(env.get("OPENSHIFT_URL"), null);
		assertEquals(env.get("OPENSHIFT_WEB_CONSOLE"),null);
		assertEquals(env.get("DOCKER_REGISTRY"),null);
	}

	@Test
	public void testServiceManager102Linux() throws Exception {
		String[] lines = serviceManager102Linux();
		HashMap<String, String> map = ServiceManagerUtility.parseLines(lines);
		ServiceManagerEnvironment env = new ServiceManagerEnvironment(map);
		serviceManager102(env, LIN_CERT_PATH);
	}

	@Test
	public void testServiceManager102Win() throws Exception {
		String[] lines = serviceManager102Win();
		HashMap<String, String> map = ServiceManagerUtility.parseLines(lines);
		ServiceManagerEnvironment env = new ServiceManagerEnvironment(map);
		serviceManager102(env, WIN_CERT_PATH);
	}

	private void serviceManager102(ServiceManagerEnvironment env, String certPath) throws Exception {
		assertEquals(env.get("DOCKER_HOST"), "tcp://10.1.2.2:2376");
		assertEquals(env.get("DOCKER_CERT_PATH"), certPath);
		assertEquals(env.get("DOCKER_TLS_VERIFY"), "1");
		assertEquals(env.get("DOCKER_API_VERSION"), "1.21");
		assertEquals(env.get("OPENSHIFT_URL"), "https://10.1.2.2:8443");
		assertEquals(env.get("OPENSHIFT_WEB_CONSOLE"),"https://10.1.2.2:8443/console");
		assertEquals(env.get("DOCKER_REGISTRY"),null);
	}

	@Test
	public void testServiceManager110Linux() throws Exception {
		String[] lines = serviceManager110Linux();
		HashMap<String, String> map = ServiceManagerUtility.parseLines(lines);
		ServiceManagerEnvironment env = new ServiceManagerEnvironment(map);
		serviceManager110(env, LIN_CERT_PATH);
	}

	@Test
	public void testServiceManager110Win() throws Exception {
		String[] lines = serviceManager110Win();
		HashMap<String, String> map = ServiceManagerUtility.parseLines(lines);
		ServiceManagerEnvironment env = new ServiceManagerEnvironment(map);
		serviceManager110(env, WIN_CERT_PATH);
	}

	private void serviceManager110(ServiceManagerEnvironment env, String certPath) throws Exception {
		assertEquals(env.get("DOCKER_HOST"), "tcp://10.1.2.2:2376");
		assertEquals(env.get("DOCKER_CERT_PATH"), certPath);
		assertEquals(env.get("DOCKER_TLS_VERIFY"), "1");
		assertEquals(env.get("DOCKER_API_VERSION"), "1.21");
		assertEquals(env.get("OPENSHIFT_URL"), "https://10.1.2.2:8443");
		assertEquals(env.get("OPENSHIFT_WEB_CONSOLE"),"https://10.1.2.2:8443/console");
		assertEquals(env.get("DOCKER_REGISTRY"),"hub.openshift.rhel-cdk.10.1.2.2.xip.io");
	}


	private String[] serviceManager101Linux() {
		return new String[]{
				"Configured services:",
				"docker - running",
				"openshift - running",
				"kubernetes - stopped",
				"",
				"docker env:",
				"# Set the following environment variables to enable access to the",
				"# docker daemon running inside of the vagrant virtual machine:",
				"export DOCKER_HOST=tcp://10.1.2.2:2376",
				"export DOCKER_CERT_PATH=/home/rob/Downloads/cdk/20160608/cdk/components/rhel/rhel-ose/.vagrant/machines/default/virtualbox/docker",
				"export DOCKER_TLS_VERIFY=1",
				"export DOCKER_API_VERSION=1.21",
				"# run following command to configure your shell:",
				"# eval # Set the following environment variables to enable access to the # docker daemon running inside of the vagrant virtual machine: export DOCKER_HOST=tcp://10.1.2.2:2376 export DOCKER_CERT_PATH=/home/rob/Downloads/cdk/20160608/cdk/components/rhel/rhel-ose/.vagrant/machines/default/virtualbox/docker export DOCKER_TLS_VERIFY=1 export DOCKER_API_VERSION=1.21 # run following command to configure your shell: # eval \"$(vagrant service-manager env docker)\"",
				"",
				"openshift env:",
				"You can access the OpenShift console on: https://10.1.2.2:8443/console",
				"To use OpenShift CLI, run: oc login https://10.1.2.2:8443",
				"",	
		};
	}
	private String[] serviceManager102Linux() {
		return new String[]{
				"",
				"# docker env:",
				"# Set the following environment variables to enable access to the",
				"# docker daemon running inside of the vagrant virtual machine:",
				"export DOCKER_HOST=tcp://10.1.2.2:2376",
				"export DOCKER_CERT_PATH=/home/rob/Downloads/cdk/20160608/cdk/components/rhel/rhel-ose/.vagrant/machines/default/virtualbox/docker",
				"export DOCKER_TLS_VERIFY=1",
				"export DOCKER_API_VERSION=1.21",
				"",
				"# openshift env:",
				"# You can access the OpenShift console on: https://10.1.2.2:8443/console",
				"# To use OpenShift CLI, run: oc login https://10.1.2.2:8443",
				"export OPENSHIFT_URL=https://10.1.2.2:8443",
				"export OPENSHIFT_WEB_CONSOLE=https://10.1.2.2:8443/console",
				"",
				"# run following command to configure your shell:",
				"# eval \"$(vagrant service-manager env)\"",	
		};
	}
	
	private String[] serviceManager110Linux() {
		return new String[]{
				"# docker env:",
				"# Set the following environment variables to enable access to the",
				"# docker daemon running inside of the vagrant virtual machine:",
				"export DOCKER_HOST=tcp://10.1.2.2:2376",
				"export DOCKER_CERT_PATH=/home/rob/Downloads/cdk/20160608/cdk/components/rhel/rhel-ose/.vagrant/machines/default/virtualbox/docker",
				"export DOCKER_TLS_VERIFY=1",
				"export DOCKER_API_VERSION=1.21",
				"",
				"# openshift env:",
				"# You can access the OpenShift console on: https://10.1.2.2:8443/console",
				"# To use OpenShift CLI, run: oc login https://10.1.2.2:8443",
				"export OPENSHIFT_URL=https://10.1.2.2:8443",
				"export OPENSHIFT_WEB_CONSOLE=https://10.1.2.2:8443/console",
				"export DOCKER_REGISTRY=hub.openshift.rhel-cdk.10.1.2.2.xip.io"
		};
	}

	

	private String[] serviceManager101Win() {
		return new String[]{
				"Configured services:",
				"docker - running",
				"openshift - running",
				"kubernetes - stopped",
				"",
				"docker env:",
				"# Set the following environment variables to enable access to the",
				"# docker daemon running inside of the vagrant virtual machine:",
				"setx DOCKER_HOST tcp://10.1.2.2:2376",
				"setx DOCKER_CERT_PATH C:\\my\\cdk\\components\\rhel\\rhel-ose\\.vagrant\\machines\\default\\virtualbox\\docker",
				"setx DOCKER_TLS_VERIFY 1",
				"setx DOCKER_API_VERSION 1.21",
				"# run following command to configure your shell:",
				"# eval # Set the following environment variables to enable access to the # docker daemon running inside of the vagrant virtual machine: export DOCKER_HOST=tcp://10.1.2.2:2376 export DOCKER_CERT_PATH=/home/rob/Downloads/cdk/20160608/cdk/components/rhel/rhel-ose/.vagrant/machines/default/virtualbox/docker export DOCKER_TLS_VERIFY=1 export DOCKER_API_VERSION=1.21 # run following command to configure your shell: # eval \"$(vagrant service-manager env docker)\"",
				"",
				"openshift env:",
				"You can access the OpenShift console on: https://10.1.2.2:8443/console",
				"To use OpenShift CLI, run: oc login https://10.1.2.2:8443",
				"",	
		};
	}
	private String[] serviceManager102Win() {
		return new String[]{
				"",
				"# docker env:",
				"# Set the following environment variables to enable access to the",
				"# docker daemon running inside of the vagrant virtual machine:",
				"setx DOCKER_HOST tcp://10.1.2.2:2376",
				"setx DOCKER_CERT_PATH C:\\my\\cdk\\components\\rhel\\rhel-ose\\.vagrant\\machines\\default\\virtualbox\\docker",
				"setx DOCKER_TLS_VERIFY 1",
				"setx DOCKER_API_VERSION 1.21",
				"",
				"# openshift env:",
				"# You can access the OpenShift console on: https://10.1.2.2:8443/console",
				"# To use OpenShift CLI, run: oc login https://10.1.2.2:8443",
				"setx OPENSHIFT_URL https://10.1.2.2:8443",
				"setx OPENSHIFT_WEB_CONSOLE https://10.1.2.2:8443/console",
				"",
				"# run following command to configure your shell:",
				"# eval \"$(vagrant service-manager env)\"",	
		};
	}
	
	private String[] serviceManager110Win() {
		return new String[]{
				"# docker env:",
				"# Set the following environment variables to enable access to the",
				"# docker daemon running inside of the vagrant virtual machine:",
				"setx DOCKER_HOST tcp://10.1.2.2:2376",
				"setx DOCKER_CERT_PATH C:\\my\\cdk\\components\\rhel\\rhel-ose\\.vagrant\\machines\\default\\virtualbox\\docker",
				"setx DOCKER_TLS_VERIFY 1",
				"setx DOCKER_API_VERSION 1.21",
				"",
				"# openshift env:",
				"# You can access the OpenShift console on: https://10.1.2.2:8443/console",
				"# To use OpenShift CLI, run: oc login https://10.1.2.2:8443",
				"setx OPENSHIFT_URL https://10.1.2.2:8443",
				"setx OPENSHIFT_WEB_CONSOLE https://10.1.2.2:8443/console",
				"setx DOCKER_REGISTRY hub.openshift.rhel-cdk.10.1.2.2.xip.io",
				"",
				"# run following command to configure your shell:",
				"# eval \"$(vagrant service-manager env)\"",	
		};
	}

	
	private String[] serviceManagerWinSET() {
		return new String[]{
				"# docker env:",
				"# Set the following environment variables to enable access to the",
				"# docker daemon running inside of the vagrant virtual machine:",
				"SET DOCKER_HOST=tcp://10.1.2.2:2376",
				"SET DOCKER_CERT_PATH=C:\\my\\cdk\\components\\rhel\\rhel-ose\\.vagrant\\machines\\default\\virtualbox\\docker",
				"SET DOCKER_TLS_VERIFY=1",
				"SET DOCKER_API_VERSION=1.21",
				"",
				"# openshift env:",
				"# You can access the OpenShift console on: https://10.1.2.2:8443/console",
				"# To use OpenShift CLI, run: oc login https://10.1.2.2:8443",
				"SET OPENSHIFT_URL=https://10.1.2.2:8443",
				"SET OPENSHIFT_WEB_CONSOLE=https://10.1.2.2:8443/console",
				"SET DOCKER_REGISTRY=hub.openshift.rhel-cdk.10.1.2.2.xip.io",
				"",
				"# run following command to configure your shell:",
				"# eval \"$(vagrant service-manager env)\"",	
		};
	}

}