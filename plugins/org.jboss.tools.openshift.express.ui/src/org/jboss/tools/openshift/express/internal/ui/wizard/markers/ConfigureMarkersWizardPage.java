/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.markers;

import java.util.Collection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.behaviour.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.core.marker.BaseOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.core.marker.IOpenShiftMarker;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftProjectUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * @author Andre Dietisheim
 */
public class ConfigureMarkersWizardPage extends AbstractOpenShiftWizardPage {

	private ConfigureMarkersWizardPageModel pageModel;
	private CheckboxTableViewer viewer;

	public ConfigureMarkersWizardPage(IProject project, IWizard wizard) {
		super(
				"Configure OpenShift Markers",
				NLS.bind("Add or remove markers to enable OpenShift features in the application {0}. "
						+ "\nThe markers will be created/deleted directly in {1}", 
						OpenShiftServerUtils.getProjectAttribute(project, OpenShiftServerUtils.SETTING_APPLICATION_NAME, "unknown"),
						OpenShiftProjectUtils.getMarkersFolder(project).getFullPath()),
				"ConfigureMarkers", wizard);
		this.pageModel = new ConfigureMarkersWizardPageModel(project);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		Group markersGroup = new Group(parent, SWT.NONE);
		markersGroup.setText("Markers");
		GridDataFactory.fillDefaults()
				.hint(SWT.DEFAULT, 300).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(markersGroup);
		GridLayoutFactory.fillDefaults()
				.margins(6, 6).applyTo(markersGroup);

		// markers table
		Composite tableContainer = new Composite(markersGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		dbc.bindSet(
				ViewerProperties.checkedElements(IOpenShiftMarker.class).observe(viewer),
				BeanProperties.set(
						ConfigureMarkersWizardPageModel.PROPERTY_CHECKED_MARKERS)
						.observe(pageModel));
		ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(viewer))
				.to(BeanProperties.value(ConfigureMarkersWizardPageModel.PROPERTY_SELECTED_MARKER)
						.observe(pageModel))
				.in(dbc);

		// marker description
		Group descriptionGroup = new Group(markersGroup, SWT.NONE);
		descriptionGroup.setText("Marker Description");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(descriptionGroup);
		GridLayoutFactory.fillDefaults()
				.margins(6, 6).applyTo(descriptionGroup);
		Text descriptionText = new Text(descriptionGroup, SWT.MULTI | SWT.WRAP);
		descriptionText.setEditable(false);
		descriptionText.setBackground(descriptionGroup.getBackground());
		GridDataFactory.fillDefaults()
				.hint(SWT.DEFAULT, 80).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(descriptionText);
		dbc.bindSet(
				ViewersObservables.observeCheckedElements(viewer, IOpenShiftMarker.class),
				BeanProperties.set(ConfigureMarkersWizardPageModel.PROPERTY_CHECKED_MARKERS).observe(pageModel));
		ValueBindingBuilder
				.bind(WidgetProperties.text().observe(descriptionText))
				.notUpdating(BeanProperties.value(ConfigureMarkersWizardPageModel.PROPERTY_SELECTED_MARKER)
						.observe(pageModel))
				.converting(new Converter(IOpenShiftMarker.class, String.class) {

					@Override
					public Object convert(Object fromObject) {
						if (!(fromObject instanceof BaseOpenShiftMarker)) {
							return null;
						}
						return ((IOpenShiftMarker) fromObject).getDescription();
					}

				})
				.in(dbc);
	}

	protected CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setComparer(new EqualityComparer());
		viewer.setContentProvider(new ArrayContentProvider());

		viewer.setSorter(new ViewerSorter() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof IEmbeddableCartridge && e2 instanceof IEmbeddableCartridge) {
					return ((IEmbeddableCartridge) e1).getDisplayName().compareTo(
							((IEmbeddableCartridge) e2).getDisplayName());
				}
				return super.compare(viewer, e1, e2);
			}
		});

		createTableColumn("Marker", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IOpenShiftMarker marker = (IOpenShiftMarker) cell.getElement();
				cell.setText(marker.getName());
			}
		}, viewer, tableLayout);
		createTableColumn("File", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IOpenShiftMarker marker = (IOpenShiftMarker) cell.getElement();
				cell.setText(marker.getFileName());
			}
		}, viewer, tableLayout);
		return viewer;
	}

	private void createTableColumn(String name, int weight, CellLabelProvider cellLabelProvider, TableViewer viewer,
			TableColumnLayout layout) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setText(name);
		column.setLabelProvider(cellLabelProvider);
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new LoadMarkersJob(), getContainer(), getDataBindingContext());
		} catch (Exception e) {
			// ignore
		}
	}
	
	private void setViewerCheckedElements(final Collection<IOpenShiftMarker> markers) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setCheckedElements(markers.toArray());
			}
		});
	}

	private void setViewerInput(final Collection<IOpenShiftMarker> marker) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				viewer.setInput(marker);
			}
		});
	}
	
	/**
	 * Returns the markers that the user has removed.
	 * 
	 * @return the markers that the user removed
	 */
	public Collection<IOpenShiftMarker> getRemovedMarkers() {
		return pageModel.getRemovedMarkers();
	}
	
	/**
	 * Returns the markers that the user has added.
	 * 
	 * @return the markers that the user added
	 */
	public Collection<IOpenShiftMarker> getAddedMarkers() {
		return pageModel.getAddedMarkers();
	}

	/**
	 * Viewer element comparer based on #equals(). The default implementation in
	 * CheckboxTableViewer compares elements based on instance identity.
	 * <p>
	 * We need this since the available cartridges (item listed in the viewer)
	 * are not the same instance as the ones in the embedded application (items
	 * to check in the viewer).
	 */
	private static class EqualityComparer implements IElementComparer {

		@Override
		public boolean equals(Object thisObject, Object thatObject) {
			if (thisObject == null) {
				return thatObject != null;
			}

			if (thatObject == null) {
				return false;
			}

			return thisObject.equals(thatObject);
		}

		@Override
		public int hashCode(Object element) {
			return element.hashCode();
		}
	}

	private class LoadMarkersJob extends AbstractDelegatingMonitorJob {

		public LoadMarkersJob() {
			super("Loading markers");
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			try {
				pageModel.loadMarkers();
				setViewerInput(pageModel.getAvailableMarkers());
				setViewerCheckedElements(pageModel.getCheckedMarkers());
				return Status.OK_STATUS;
			} catch (CoreException e) {
				return OpenShiftUIActivator.createErrorStatus(
						NLS.bind("Could not load markers for project {0}", pageModel.getProject().getName()), e);
			}

		}
	}

}