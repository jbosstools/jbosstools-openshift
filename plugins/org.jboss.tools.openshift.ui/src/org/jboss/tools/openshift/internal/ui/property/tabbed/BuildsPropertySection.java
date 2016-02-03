/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.internal.ui.property.tabbed;

import org.eclipse.swt.SWT;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;

import com.openshift.restclient.model.IBuild;

public class BuildsPropertySection extends OpenShiftResourcePropertySection implements OpenShiftAPIAnnotations {

	public BuildsPropertySection() {
		super("popup:org.jboss.tools.openshift.ui.properties.tab.BuildsTab");
	}

	@Override
	protected void setSorter(TableViewerBuilder tableViewerBuilder) {
		tableViewerBuilder.sorter(createCreatedBySorter());
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
}
