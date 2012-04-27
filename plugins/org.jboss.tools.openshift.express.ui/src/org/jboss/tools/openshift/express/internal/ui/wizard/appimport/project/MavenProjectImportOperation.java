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
package org.jboss.tools.openshift.express.internal.ui.wizard.appimport.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;

/**
 * @author Andre Dietisheim <adietish@redhat.com>
 * 
 */
public class MavenProjectImportOperation extends AbstractProjectImportOperation {

	private static final String POM_FILE = "pom.xml";

	public MavenProjectImportOperation(File projectFolder) {
		super(projectFolder);
	}

	public List<IProject> importToWorkspace(IProgressMonitor monitor)
			throws CoreException, InterruptedException {
		MavenPluginActivator mavenPlugin = MavenPluginActivator.getDefault();
		IProjectConfigurationManager configurationManager = mavenPlugin.getProjectConfigurationManager();
		MavenModelManager modelManager = mavenPlugin.getMavenModelManager();
		Set<MavenProjectInfo> projectInfos = getMavenProjects(getProjectDirectory(), modelManager, monitor);
		ProjectImportConfiguration projectImportConfiguration = new ProjectImportConfiguration();
		List<IMavenProjectImportResult> importResults =
				configurationManager.importProjects(projectInfos, projectImportConfiguration, monitor);
		return toProjects(importResults);
	}

	private List<IProject> toProjects(List<IMavenProjectImportResult> importResults) {
		List<IProject> projects = new ArrayList<IProject>();
		for (IMavenProjectImportResult importResult : importResults) {
			IProject project = importResult.getProject();
			if (project != null) {
				projects.add(importResult.getProject());
			}
		}
		return projects;
	}

	private Set<MavenProjectInfo> getMavenProjects(File directory, MavenModelManager modelManager,
			IProgressMonitor monitor) throws InterruptedException {
		LocalProjectScanner scanner = new LocalProjectScanner(directory.getParentFile(), directory.toString(), false,
				modelManager);
		scanner.run(monitor);
		return collectProjects(scanner.getProjects());
	}

	public boolean isMavenProject() {
		if (!isReadable(getProjectDirectory())
				|| !getProjectDirectory().isDirectory()) {
			return false;
		}

		return isReadable(new File(getProjectDirectory(), POM_FILE));
	}
	
	public Set<MavenProjectInfo> collectProjects(
			Collection<MavenProjectInfo> projects) {
		return new LinkedHashSet<MavenProjectInfo>() {
			private static final long serialVersionUID = 1L;

			public Set<MavenProjectInfo> collectProjects(
					Collection<MavenProjectInfo> projects) {
				for (MavenProjectInfo projectInfo : projects) {
					add(projectInfo);
					collectProjects(projectInfo.getProjects());
				}
				return this;
			}
		}.collectProjects(projects);
	}
}
