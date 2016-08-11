/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.internal.ui.property.tabbed;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.ICellToolTipProvider;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IEvent;

/**
 * Tabbed property section for displaying Events.
 *  
 * @author jeff.cantrill
 *
 */
public class EventsPropertySection extends OpenShiftResourcePropertySection {

	public EventsPropertySection() {
		super("popup:org.jboss.tools.openshift.ui.properties.tab.EventsTab", ResourceKind.EVENT);
	}
	
	@Override
	protected void setSorter(TableViewerBuilder tableViewerBuilder) {
		tableViewerBuilder.sorter(createCreationTimestampSorter(true));
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void addColumns(TableViewerBuilder tableViewerBuilder) {
		addCreatedColumn(tableViewerBuilder);
		ICellToolTipProvider toolTipProvider = new ICellToolTipProvider() {
			
			@Override
			public String getToolTipText(Object object) {
				if(object instanceof IResourceWrapper && ((IResourceWrapper) object).getWrapped() instanceof IEvent) {
					return ((IEvent)((IResourceWrapper) object).getWrapped()).getMessage();
				}
				return null;
			}
			
			@Override
			public int getToolTipDisplayDelayTime(Object event) {
				return 0;
			}
		};
		tableViewerBuilder
		//name
		.column(model -> StringUtils.substringBefore(getResource(model).getName(),".")).name("Name").align(SWT.LEFT).weight(1)
		.minWidth(15)
		.cellToolTipProvider(toolTipProvider)
		.buildColumn()
		//kind
		.column(model -> ((IEvent)getResource(model)).getInvolvedObject().getKind()).name("Kind").align(SWT.LEFT).weight(1).minWidth(5)
		.cellToolTipProvider(toolTipProvider)
		.buildColumn()
		//reason
		.column(model -> ((IEvent)getResource(model)).getReason()).name("Reason").align(SWT.LEFT).weight(1).minWidth(5)
		.cellToolTipProvider(toolTipProvider)
		.buildColumn();
	}
	
	
}
