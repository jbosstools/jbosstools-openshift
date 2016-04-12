/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;

/**
 * Detects the type of an {@link IProject}
 * 
 * @author Fred Bricon
 */
public class ProjectBuilderTypeDetector {

	private static IProjectBuilderType UNKNOWN = new IProjectBuilderType(){
		@Override
		public boolean applies(IProject project) {
			return true;
		}

		@Override
		public String getTags(IProject project) {
			return "";
		};
	};
	private Collection<IProjectBuilderType> detectors;

	public ProjectBuilderTypeDetector() {
		//XXX might consider using Ext. Points in the future
		//See Detection table in https://docs.openshift.org/latest/dev_guide/new_app.html#language-detection 
		this(new SimpleTypeDetector("eap", "pom.xml"),//TODO be more specific wrt Tomcat 7/8, Micro services
			 new SimpleTypeDetector("php", "index.php", "composer.json"),
			 new SimpleTypeDetector("ruby", "Rakefile", "Gemfile", "config.ru"),
			 new SimpleTypeDetector("python", "requirements.txt", "config.py"),
			 new SimpleTypeDetector("perl", "index.pl", "cpanfile"),
			 new SimpleTypeDetector("node", "app.json", "package.json")
			 );
	}

	public ProjectBuilderTypeDetector(IProjectBuilderType...detectors) {
		this.detectors = Arrays.asList(detectors);
	}

	protected IProjectBuilderType identify(final IProject project) {
		if (project == null) {
			return UNKNOWN;
		}
		return detectors.stream()
						.filter(d -> d.applies(project))
						.findFirst()
						.orElse(UNKNOWN);
	}

	public String findTemplateFilter(final IProject project) {
		if (project == null) {
			return null;
		}
		return identify(project)
				.getTags(project);//that's a bit ugly but we'll prolly need to get specific tags 
		                          //depending on some other project settings
	}

	private static class SimpleTypeDetector implements IProjectBuilderType {

		private String type;
		private Collection<String> files;

		SimpleTypeDetector(String type, String...files) {
			this.type = type;
			this.files = files == null ? Collections.emptyList():Arrays.asList(files);
		}

		@Override
		public String getTags(IProject project) {
			return type;
		}

		protected boolean hasAnyFile(final IProject project, Collection<String> files) {
			return files.stream()
						.filter(f -> project.getFile(f).exists())
						.findFirst()
						.isPresent();
		}

		@Override
		public boolean applies(IProject project) {
			return ProjectUtils.isAccessible(project) && hasAnyFile(project, files);
		}

		@Override
		public String toString() {
			return "["+type+"] type detector";
		}
	}

}
