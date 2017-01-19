/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.TreeItem;
import org.hamcrest.Matcher;
import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.core.matcher.WithTextMatcher;
import org.jboss.reddeer.core.util.Display;
import org.jboss.reddeer.core.util.ResultRunnable;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.Service;

/**
 * Wait condition to wait for an existence of a pod in OpenShift explorer.
 * Pod is placed right under a service. It is possible to either let 
 * the default project PROJECT1 to be chosen or to specify desired project and 
 * a service. This wait condition rely on working Watcher and automatic update
 * of resources (builds, pods) on a service. If you want use more reliable way 
 * and do not want to test/rely on watcher, use ResourceExists wait condition.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class PodExists extends AbstractWaitCondition{

	private Service service;
	private Matcher<String>[] matchers;
	private TreeItem serviceItem;
	
	@SuppressWarnings("unchecked")
	public PodExists(String serviceName, String podName) {
		this(serviceName, new WithTextMatcher(podName));
	}
	
	@SuppressWarnings("unchecked")
	public PodExists(String serviceName, Matcher<String>... podNameMatchers) {
		this(DatastoreOS3.PROJECT1_DISPLAYED_NAME, serviceName, podNameMatchers);
	}
	
	@SuppressWarnings("unchecked")
	public PodExists(String project, String serviceName, Matcher<String>... podNameMatchers) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		matchers = podNameMatchers;
		service = explorer.getOpenShift3Connection().getProject(project).
				getService(serviceName);
		service.refresh();
		service.expand();
		service.select();
		serviceItem = service.getTreeItem().getSWTWidget();
	}

	@Override
	public boolean test() {
		List<String> treeItemsTexts = Display.syncExec(new ResultRunnable<List<String>>() {
			@Override
			public List<String >run() {
				List<String> texts = new ArrayList<String>();
				for (TreeItem treeItem: serviceItem.getItems()) {
					texts.add(treeItem.getText());
				}
				return texts;
			}
		});
		
		if (treeItemsTexts.size() == 0) {
			return false;
		}
		
		for (String text: treeItemsTexts) {
			boolean matches = true;
			if (matchers != null) {
				for (Matcher<String> matcher: matchers) {
					if (!matcher.matches(text)) {
						matches = false;
						break;
					}
				}
			}
			if (matches) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
}
