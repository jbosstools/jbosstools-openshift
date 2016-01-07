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

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;

import com.openshift.restclient.model.IResource;

/**
 * Generic tabbed property section for displaying
 * an openshift resource
 * @author jeff.cantrill
 *
 */
public class OpenShiftResourcePropertySection extends AbstractPropertySection implements OpenShiftAPIAnnotations {

	private static final String CONTEXT_MENU_ID = "popup:org.jboss.tools.openshift.ui.properties.tab.ResourceTab";
	private TableViewer table;
	private PropertySheetPage details;
	private TabbedPropertySheetPage page;

	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		this.page = aTabbedPropertySheetPage;
		
		SashForm container = new SashForm(parent, SWT.VERTICAL);
		Composite tableContainer = new Composite(container, SWT.NONE);
		
		tableContainer.setLayout(new FillLayout());
		this.table = createTable(tableContainer);

		details = new PropertySheetPage();
		details.createControl(container);
		aTabbedPropertySheetPage.getSite().setSelectionProvider(new ISelectionProvider() {
			
			@Override
			public void setSelection(ISelection selection) {
			}
			
			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			}
			
			@Override
			public ISelection getSelection() {
				return table.getSelection();
			}
			
			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
			}
		});
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableViewer viewer = new TableViewerBuilder(table, tableContainer)
			.contentProvider(new ObservableListContentProvider())
			.sorter(new ViewerSorter() {
				
				@Override
				public int compare(Viewer viewer, Object e1, Object e2) {
					IResource r1 = ((IResourceUIModel)e1).getResource();
					IResource r2 = ((IResourceUIModel)e2).getResource();
					return r1.getName().compareTo(r2.getName());
				}
				
			})
			.column(new IColumnLabelProvider<IResourceUIModel>() {
				@Override
				public String getValue(IResourceUIModel model) {
					return model.getResource().getName();
				}
			}).name("Name").align(SWT.LEFT).weight(1).minWidth(10).buildColumn()
			.column(new IColumnLabelProvider<IResourceUIModel>() {
				@Override
				public String getValue(IResourceUIModel model) {
					return model.getResource().getCreationTimeStamp();
				}
			}).name("Created").align(SWT.LEFT).weight(1).minWidth(5).buildColumn()
			.column(new IColumnLabelProvider<IResourceUIModel>() {
				@Override
				public String getValue(IResourceUIModel model) {
					return StringUtils.serialize(model.getResource().getLabels());
				}
			}).name("Labels").align(SWT.LEFT).weight(1).minWidth(25).buildColumn()
			.buildViewer();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				details.selectionChanged(null, event.getSelection());
			}
		});
		addContextMenu(table);
		return viewer;
	}
	
	private void addContextMenu(Table table) {
		IMenuManager contextMenu = UIUtils.createContextMenu(table);
		UIUtils.registerContributionManager(CONTEXT_MENU_ID, contextMenu, table);
    }
	
	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		Object model = UIUtils.getFirstElement(selection);
		if(model == null) return;
		ITabDescriptor tab = page.getSelectedTab();
		if(tab == null) return;
		
		String id = tab.getId();
		String property = org.apache.commons.lang.StringUtils.right(id, id.length() - id.lastIndexOf(".") - 1);
		table.setInput(BeanProperties.list(property).observe(model));
	}

	@Override
	public void dispose() {
		if(this.table != null && this.table.getTable() != null) {
			this.table.getTable().dispose();
		}
		if(this.details != null) {
			this.details.dispose();
		}
	}

	@Override
	public boolean shouldUseExtraSpace() {
		return true;
	}

	@Override
	public void refresh() {
		if(!DisposeUtils.isDisposed(table)) {
			this.table.refresh();
		}
		if(!DisposeUtils.isDisposed(this.details.getControl())) {
			this.details.refresh();
		}
	}
}
