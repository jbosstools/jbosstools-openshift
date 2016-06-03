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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.common.ui.utils.DateTimeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.comparators.CreationTimestampComparator;
import org.jboss.tools.openshift.internal.ui.models2.AbstractResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models2.ResourceContainer;

import com.openshift.restclient.model.IResource;

/**
 * Generic tabbed property section for displaying an openshift resource
 * 
 * @author jeff.cantrill
 *
 */
public class OpenShiftResourcePropertySection extends AbstractPropertySection implements OpenShiftAPIAnnotations {

	protected TableViewer table;
	protected PropertySheetPage details;
	protected TabbedPropertySheetPage page;
	private ISelectionProvider selectionProvider;
	private String menuContributionId;
	private String resourceKind;

	public OpenShiftResourcePropertySection(String menuContributionId, String kind) {
		this.resourceKind= kind;
		this.menuContributionId = menuContributionId;
	}

	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		createContents(parent, aTabbedPropertySheetPage);
	}

	@Override
	public void aboutToBeShown() {
		super.aboutToBeShown();
		if (page != null) {
			page.getSite().setSelectionProvider(selectionProvider);
		}
	}

	protected void createContents(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		this.page = aTabbedPropertySheetPage;
		parent.setLayout(new GridLayout());

		SashForm container = new SashForm(parent, SWT.VERTICAL);
		GridData d = new GridData(GridData.FILL_BOTH);
		d.widthHint = 100; // A dirty trick that keeps table from growing
							// unlimitedly within scrolled parent composite.
		d.heightHint = 100;
		container.setLayoutData(d);
		Composite tableContainer = new Composite(container, SWT.NONE);

		tableContainer.setLayout(new FillLayout());
		this.table = createTable(tableContainer);

		details = new PropertySheetPage();
		details.createControl(container);
		selectionProvider = new ISelectionProvider() {

			@Override
			public void setSelection(ISelection selection) {
			}

			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				table.removeSelectionChangedListener(listener);
			}

			@Override
			public ISelection getSelection() {
				return table.getSelection();
			}

			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				table.addSelectionChangedListener(listener);
			}
		};
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableViewerBuilder tableViewerBuilder = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ResourceContainerContentProvider(resourceKind));

		setSorter(tableViewerBuilder);
		addColumns(tableViewerBuilder);

		TableViewer viewer = tableViewerBuilder.buildViewer();
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				details.selectionChanged(null, event.getSelection());
			}
		});
		addContextMenu(viewer);
		return viewer;
	}

	protected void setSorter(TableViewerBuilder tableViewerBuilder) {
		tableViewerBuilder.sorter(createNameSorter());
	}

	protected void addColumns(TableViewerBuilder tableViewerBuilder) {
		addNameColumn(tableViewerBuilder);
		addCreatedColumn(tableViewerBuilder);
		addLabelsColumn(tableViewerBuilder);
	}

	protected IResource getResource(Object element) {
		if (element instanceof IResource) {
			return (IResource) element;
		}
		if (element instanceof IAdaptable) {
			return ((IAdaptable) element).getAdapter(IResource.class);
		}
		return null;
	}

	protected void addNameColumn(TableViewerBuilder tableViewerBuilder) {
		tableViewerBuilder.column(model -> getResource(model).getName()).name("Name").align(SWT.LEFT).weight(1)
				.minWidth(15).buildColumn();
	}

	protected void addCreatedColumn(TableViewerBuilder tableViewerBuilder) {
		tableViewerBuilder.column(model -> DateTimeUtils.formatSince(getResource(model).getCreationTimeStamp()))
				.name("Created").align(SWT.LEFT).weight(1).minWidth(5).buildColumn();
	}

	protected void addLabelsColumn(TableViewerBuilder tableViewerBuilder) {
		tableViewerBuilder.column(model -> StringUtils.serialize(getResource(model).getLabels())).name("Labels")
				.align(SWT.LEFT).weight(1).minWidth(25).buildColumn();
	}

	protected void addContextMenu(TableViewer viewer) {
		if (menuContributionId != null) {
			final IMenuManager contextMenu = UIUtils.createContextMenu(viewer.getTable());
			UIUtils.registerContributionManager(menuContributionId, contextMenu, viewer.getTable());
		}
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		ResourceContainer<?, ?> model = UIUtils.getFirstElement(selection, ResourceContainer.class);
		if (model == null)
			return;
		ITabDescriptor tab = page.getSelectedTab();
		if (tab == null)
			return;
		table.setInput(model);
	}

	@Override
	public void dispose() {
		if (this.table != null && this.table.getTable() != null) {
			this.table.getTable().dispose();
		}
		if (this.details != null) {
			this.details.dispose();
		}
	}

	@Override
	public boolean shouldUseExtraSpace() {
		return true;
	}

	@Override
	public void refresh() {
		if (!DisposeUtils.isDisposed(table)) {
			this.table.refresh();
		}
		if (!DisposeUtils.isDisposed(this.details.getControl())) {
			this.details.refresh();
		}
	}

	protected ViewerComparator createCreatedBySorter() {
		final CreationTimestampComparator comparator = new CreationTimestampComparator();
		return new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return comparator.compare((AbstractResourceWrapper<?, ?>) e1, (AbstractResourceWrapper<?, ?>) e2);
			}
		};
	}

	protected ViewerComparator createNameSorter() {
		return new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				IResource r1 = ((AbstractResourceWrapper<?, ?>) e1).getResource();
				IResource r2 = ((AbstractResourceWrapper<?, ?>) e2).getResource();
				return r1.getName().compareTo(r2.getName());
			}
		};
	}
}
