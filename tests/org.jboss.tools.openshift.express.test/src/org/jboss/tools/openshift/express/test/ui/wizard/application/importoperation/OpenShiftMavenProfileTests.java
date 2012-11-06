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
package org.jboss.tools.openshift.express.test.ui.wizard.application.importoperation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.importoperation.OpenShiftMavenProfile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftMavenProfileTests {

	private static final String PLUGIN_ID = "org.jboss.tools.openshift.express.test";
	private static final String POM_FILENAME = "pom.xml";

	private static final String POM_WITHOUT_OPENSHIFT =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
					+ "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
					+ "	<modelVersion>4.0.0</modelVersion>\n"
					+ "	<groupId>org.jboss.tools.openshift.tests</groupId>\n"
					+ "	<artifactId>org.jboss.tools.openshift.express.test</artifactId>\n"
					+ "	<packaging>eclipse-test-plugin</packaging>\n"
					+ "</project>\n";

	private static final String OPENSHIFT_PROFILE =
			"<!-- When built in OpenShift the 'openshift' profile will be used when invoking mvn. -->\n"
					+ "<!-- Use this profile for any OpenShift specific customization your app will need. -->\n"
					+ "<!-- By default that is to put the resulting archive into the 'deployments' folder. -->\n"
					+ "<!-- http://maven.apache.org/guides/mini/guide-building-for-different-environments.html -->\n"
					+ "<id>openshift</id>\n"
					+ "<build>\n"
					+ "  <finalName>{0}</finalName>\n"
					+ "  <plugins>\n"
					+ "    <plugin>\n"
					+ "      <artifactId>maven-war-plugin</artifactId>\n"
					+ "      <version>2.1.1</version>\n"
					+ "      <configuration>\n"
					+ "        <outputDirectory>deployments</outputDirectory>\n"
					+ "        <warName>ROOT</warName>\n"
					+ "      </configuration>\n"
					+ "    </plugin>\n"
					+ "  </plugins>\n"
					+ "</build>\n";

	private static final String POM_WITH_OPENSHIFT =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
					+ "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
					+ "	<modelVersion>4.0.0</modelVersion>\n"
					+ "	<groupId>org.jboss.tools.openshift.tests</groupId>\n"
					+ "	<artifactId>org.jboss.tools.openshift.express.test</artifactId>\n"
					+ "	<packaging>eclipse-test-plugin</packaging>\n"
					+ " <profiles>\n"
					+ "   <profile>\n"

					+ OPENSHIFT_PROFILE

					+ "   </profile>\n"
					+ " </profiles>\n"
					+ "</project>\n";

	private static final String POM_WITH_PROFILES_WITHOUT_OPENSHIFT = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
					+"    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
					+"\n"
					+"    <modelVersion>4.0.0</modelVersion>\n"
					+"\n"
					+"    <groupId>org.jboss.tools.example.richfaces</groupId>\n"
					+"    <artifactId>richfaces-webapp</artifactId>\n"
					+"    <name>RichFaces 4 Application</name>\n"
					+"    <version>0.0.1-SNAPSHOT</version>\n"
					+"    <packaging>war</packaging>\n"
					+"    <url>http://jboss.org/richfaces</url>\n"
					+"    <repositories>\n"
					+"        <!-- You should seriously consider using a repository manager or declare repositories in your settings.xml.\n"
					+"           See http://www.sonatype.com/people/2009/02/why-putting-repositories-in-your-poms-is-a-bad-idea/   -->\n"
					+"        <repository>\n"
					+"            <id>jboss-public-repository-group</id>\n"
					+"            <name>JBoss Public Maven Repository Group</name>\n"
					+"            <url>https://repository.jboss.org/nexus/content/groups/public-jboss/</url>\n"
					+"            <layout>default</layout>\n"
					+"            <releases>\n"
					+"                <enabled>true</enabled>\n"
					+"                <updatePolicy>never</updatePolicy>\n"
					+"            </releases>\n"
					+"            <snapshots>\n"
					+"                <enabled>true</enabled>\n"
					+"                <updatePolicy>never</updatePolicy>\n"
					+"            </snapshots>\n"
					+"        </repository>\n"
					+"    </repositories>\n"
					+"    <pluginRepositories>\n"
					+"        <pluginRepository>\n"
					+"            <id>jboss-public-repository-group</id>\n"
					+"            <name>JBoss Public Maven Repository Group</name>\n"
					+"            <url>https://repository.jboss.org/nexus/content/groups/public-jboss/</url>\n"
					+"            <layout>default</layout>\n"
					+"            <releases>\n"
					+"                <enabled>true</enabled>\n"
					+"                <updatePolicy>never</updatePolicy>\n"
					+"            </releases>\n"
					+"            <snapshots>\n"
					+"                <enabled>true</enabled>\n"
					+"                <updatePolicy>never</updatePolicy>\n"
					+"            </snapshots>\n"
					+"        </pluginRepository>\n"
					+"    </pluginRepositories>\n"
					+"\n"
					+"    <properties>\n"
					+"        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n"
					+"        <maven.compiler.source>1.6</maven.compiler.source>\n"
					+"        <maven.compiler.target>1.6</maven.compiler.target>\n"
					+"        <!-- Setting this property using archetype-metadata.xml requiredPorperty\n"
					+"             so that generated project uses correct version of richfaces.\n"
					+"         -->\n"
					+"        <org.richfaces.bom.version>4.1.0.Final</org.richfaces.bom.version>\n"
					+"    </properties>\n"
					+"\n"
					+"    <build>\n"
					+"        <finalName>richfaces-webapp</finalName>\n"
					+"        <plugins>\n"
					+"            <plugin>\n"
					+"                <artifactId>maven-compiler-plugin</artifactId>\n"
					+"                <version>2.3.2</version>\n"
					+"            </plugin>\n"
					+"            <plugin>\n"
					+"                <artifactId>maven-war-plugin</artifactId>\n"
					+"                <version>2.1.1</version>\n"
					+"            </plugin>\n"
					+"        </plugins>\n"
					+"    </build>\n"
					+"\n"
					+"    <dependencyManagement>\n"
					+"        <dependencies>\n"
					+"            <dependency>\n"
					+"                <groupId>org.richfaces</groupId>\n"
					+"                <artifactId>richfaces-bom</artifactId>\n"
					+"                <version>${org.richfaces.bom.version}</version>\n"
					+"                <scope>import</scope>\n"
					+"                <type>pom</type>\n"
					+"            </dependency>\n"
					+"        </dependencies>\n"
					+"    </dependencyManagement>\n"
					+"\n"
					+"    <dependencies>\n"
					+"        <dependency>\n"
					+"            <groupId>org.richfaces.ui</groupId>\n"
					+"            <artifactId>richfaces-components-ui</artifactId>\n"
					+"        </dependency>\n"
					+"        <dependency>\n"
					+"            <groupId>org.richfaces.core</groupId>\n"
					+"            <artifactId>richfaces-core-impl</artifactId>\n"
					+"        </dependency>\n"
					+"        <dependency>\n"
					+"            <groupId>javax.faces</groupId>\n"
					+"            <artifactId>javax.faces-api</artifactId>\n"
					+"            <scope>provided</scope>\n"
					+"        </dependency>\n"
					+"        <dependency>\n"
					+"            <groupId>org.glassfish</groupId>\n"
					+"            <artifactId>javax.faces</artifactId>\n"
					+"            <scope>compile</scope>\n"
					+"        </dependency>\n"
					+"        <dependency>\n"
					+"            <groupId>javax.servlet</groupId>\n"
					+"            <artifactId>servlet-api</artifactId>\n"
					+"            <scope>provided</scope>\n"
					+"        </dependency>\n"
					+"        <dependency>\n"
					+"            <groupId>javax.servlet.jsp</groupId>\n"
					+"            <artifactId>jsp-api</artifactId>\n"
					+"            <scope>provided</scope>\n"
					+"        </dependency>\n"
					+"        <dependency>\n"
					+"            <groupId>javax.el</groupId>\n"
					+"            <artifactId>el-api</artifactId>\n"
					+"            <scope>provided</scope>\n"
					+"        </dependency>\n"
					+"        <dependency>\n"
					+"            <groupId>javax.servlet.jsp.jstl</groupId>\n"
					+"            <artifactId>jstl-api</artifactId>\n"
					+"        </dependency>\n"
					+"\n"
					+"        <dependency>\n"
					+"            <groupId>net.sf.ehcache</groupId>\n"
					+"            <artifactId>ehcache</artifactId>\n"
					+"        </dependency>\n"
					+"    </dependencies>\n"
					+"\n"
					+"    <profiles>\n"
					+"        <profile>\n"
					+"            <id>jee6</id>\n"
					+"            <build>\n"
					+"                <plugins>\n"
					+"                    <plugin>\n"
					+"                        <artifactId>maven-war-plugin</artifactId>\n"
					+"                        <configuration>\n"
					+"                            <webappDirectory>${project.build.directory}/${project.build.finalName}-jee6</webappDirectory>\n"
					+"                            <classifier>jee6</classifier>\n"
					+"                        </configuration>\n"
					+"                    </plugin>\n"
					+"                </plugins>\n"
					+"            </build>\n"
					+"\n"
					+"            <dependencies>\n"
					+"                <dependency>\n"
					+"                    <groupId>javax.faces</groupId>\n"
					+"                    <artifactId>javax.faces-api</artifactId>\n"
					+"                    <scope>provided</scope>\n"
					+"                </dependency>\n"
					+"                <dependency>\n"
					+"                    <groupId>org.glassfish</groupId>\n"
					+"                    <artifactId>javax.faces</artifactId>\n"
					+"                    <scope>provided</scope>\n"
					+"                </dependency>\n"
					+"                <dependency>\n"
					+"                    <groupId>javax.transaction</groupId>\n"
					+"                    <artifactId>jta</artifactId>\n"
					+"                    <version>1.1</version>\n"
					+"                    <scope>provided</scope>\n"
					+"                </dependency>\n"
					+"            </dependencies>\n"
					+"        </profile>\n"
					+"        <profile>\n"
					+"            <id>release</id>\n"
					+"            <build>\n"
					+"                <plugins>\n"
					+"                    <plugin>\n"
					+"                        <artifactId>maven-war-plugin</artifactId>\n"
					+"                        <executions>\n"
					+"                            <execution>\n"
					+"                                <id>jee6</id>\n"
					+"                                <phase>package</phase>\n"
					+"                                <goals>\n"
					+"                                    <goal>war</goal>\n"
					+"                                </goals>\n"
					+"                                <configuration>\n"
					+"                                    <webappDirectory>${project.build.directory}/${project.build.finalName}-jee6</webappDirectory>\n"
					+"                                    <classifier>jee6</classifier>\n"
					+"                                    <packagingExcludes>WEB-INF/lib/javax.faces*</packagingExcludes>\n"
					+"                                    <warSourceExcludes>WEB-INF/lib/javax.faces*</warSourceExcludes>\n"
					+"                                </configuration>\n"
					+"                            </execution>\n"
					+"                        </executions>\n"
					+"                    </plugin>\n"
					+"                </plugins>\n"
					+"            </build>\n"
					+"        </profile>\n"
					+"    </profiles>\n"
					+"</project>\n";
	
	private IProject nonOpenShiftProject;
	private IFile pomWithoutOpenShiftProfile;
	private IProject nonOpenShiftProfilesProject;
	private IProject openShiftProject;
	private IFile pomWithOpenShiftProfile;

	@Test
	public void canDetectOpenShiftProfileNotPresent() throws CoreException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(pomWithoutOpenShiftProfile, PLUGIN_ID);
		assertFalse(profile.existsInPom());
	}

	@Test
	public void canDetectOpenShiftProfilePresent() throws CoreException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(pomWithOpenShiftProfile, PLUGIN_ID);
		assertTrue(profile.existsInPom());
	}

	@Test
	public void canDetectPomInProject() throws CoreException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(openShiftProject, PLUGIN_ID);
		assertTrue(profile.existsInPom());
		profile = new OpenShiftMavenProfile(nonOpenShiftProject, PLUGIN_ID);
		assertFalse(profile.existsInPom());
	}

	@Test
	public void canDetectOpenShiftProfileInComplexPom() throws CoreException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(nonOpenShiftProfilesProject, PLUGIN_ID);
		assertFalse(profile.existsInPom());
	}
	
	@Test
	public void canAddOpenShiftProfile() throws CoreException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(pomWithoutOpenShiftProfile, PLUGIN_ID);
		boolean added = profile.addToPom(nonOpenShiftProject.getName());
		assertTrue(added);
	}

	@Test
	public void pomHasOpenShiftProfileAfterAdd() throws CoreException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(pomWithoutOpenShiftProfile, PLUGIN_ID);
		profile.addToPom(nonOpenShiftProject.getName());
		profile.savePom(new NullProgressMonitor());
		profile = new OpenShiftMavenProfile(pomWithoutOpenShiftProfile, PLUGIN_ID);
		assertTrue(profile.existsInPom());
	}

	@Test
	public void canAddOpenShiftProfileToComplexPom() throws CoreException, IOException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(nonOpenShiftProfilesProject, PLUGIN_ID);
		boolean added = profile.addToPom(nonOpenShiftProfilesProject.getName());
		assertTrue(added);
		profile.savePom(new NullProgressMonitor());
		profile = new OpenShiftMavenProfile(nonOpenShiftProfilesProject, PLUGIN_ID);
		assertTrue(profile.existsInPom());
	}

	@Test
	public void addedOpenShiftProfileIsCorrect() throws CoreException, IOException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(nonOpenShiftProfilesProject, PLUGIN_ID);
		boolean added = profile.addToPom(nonOpenShiftProfilesProject.getName());
		assertTrue(added);
		profile.savePom(new NullProgressMonitor());
		String pomContent = toString(nonOpenShiftProfilesProject.getFile(POM_FILENAME));
		assertTrue(pomContent.indexOf("<id>openshift</id>") >= 0);
	}

	@Test
	public void doesNotAddOpenShiftProfileIfAlreadyPresent() throws CoreException {
		OpenShiftMavenProfile profile = new OpenShiftMavenProfile(pomWithOpenShiftProfile, PLUGIN_ID);
		boolean added = profile.addToPom(openShiftProject.getName());
		assertFalse(added);
	}

	@Before
	public void setUp() throws CoreException {
		this.openShiftProject = createTmpProject();
		this.pomWithOpenShiftProfile = createPomFile(POM_WITH_OPENSHIFT, openShiftProject);
		this.nonOpenShiftProfilesProject = createTmpProject();
		createPomFile(POM_WITH_PROFILES_WITHOUT_OPENSHIFT, nonOpenShiftProfilesProject);
		this.nonOpenShiftProject = createTmpProject();
		this.pomWithoutOpenShiftProfile = createPomFile(POM_WITHOUT_OPENSHIFT, nonOpenShiftProject);
	}

	@After
	public void tearDown() throws CoreException {
		deleteProject(openShiftProject);
		deleteProject(nonOpenShiftProject);
		deleteProject(nonOpenShiftProfilesProject);
	}

	private void deleteProject(final IProject project) throws CoreException {
		if (project == null
				|| !project.isAccessible()) {
			return;
		}
		project.getWorkspace().run(new IWorkspaceRunnable() {
			
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				project.close(null);
				project.delete(true, null);
			}
		}, null);
	}

	private IFile createPomFile(final String content, final IProject project) throws CoreException {
		final IFile pomFile = project.getFile(POM_FILENAME);
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				pomFile.create(
						new ByteArrayInputStream(content.getBytes())
						, true
						, new NullProgressMonitor());
				pomFile.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			}
		};
		project.getWorkspace().run(runnable, new NullProgressMonitor());
		return pomFile;
	}

	private IProject createTmpProject() throws CoreException {
		String name = String.valueOf(System.currentTimeMillis());
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				project.create(new NullProgressMonitor());
				project.open(new NullProgressMonitor());
			}
		};
		project.getWorkspace().run(runnable, new NullProgressMonitor());
		return project;
	}
	
	private String toString(IFile file) throws CoreException, IOException {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents()));
		for(String line = null; (line = reader.readLine()) != null; ) {
			builder.append(line);
		}
		return builder.toString();
	}
}
