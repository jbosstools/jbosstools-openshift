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

package org.jboss.tools.openshift.internal.ui.property.tabbed;

import java.text.ParseException;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPart;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.models.Deployment;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;

import com.openshift.restclient.model.IBuild;

public class BuildsPropertySection extends OpenShiftResourcePropertySection implements OpenShiftAPIAnnotations {

	public BuildsPropertySection() {
		super("popup:org.jboss.tools.openshift.ui.properties.tab.BuildsTab");
	}

	@Override
	protected void setSorter(TableViewerBuilder tableViewerBuilder) {
		tableViewerBuilder.sorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				IBuild build1 = (IBuild)((IResourceUIModel)e1).getResource();
				IBuild build2 = (IBuild)((IResourceUIModel)e2).getResource();
				try {
					return -1 * DateTimeUtils.parse(build1.getCreationTimeStamp())
							.compareTo(DateTimeUtils.parse(build2.getCreationTimeStamp()));
				} catch (ParseException e) {
				}
				return 0;
			}
		});
	}

	@Override
	protected void addColumns(TableViewerBuilder tableViewerBuilder) {
		addNameColumn(tableViewerBuilder);
		tableViewerBuilder.column((IResourceUIModel model) -> {
				return ((IBuild)model.getResource()).getAnnotation(BUILD_NUMBER);
		}).name("Build").align(SWT.LEFT).weight(1).minWidth(5).buildColumn()
		.column((IResourceUIModel model) -> {
				return ((IBuild)model.getResource()).getStatus();
		}).name("Status").align(SWT.LEFT).weight(1).minWidth(25).buildColumn()
		.column((IResourceUIModel model) -> {
				return DateTimeUtils.formatSince(model.getResource().getCreationTimeStamp());
		}).name("Started").align(SWT.LEFT).weight(1).buildColumn();
	}

	
	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		Object model = UIUtils.getFirstElement(selection);
		if(model == null) return;
		table.setInput(BeanProperties.list(Deployment.PROP_BUILDS).observe(model));
	}
}
