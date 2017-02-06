/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.lang.reflect.InvocationTargetException;

import javax.print.attribute.standard.Finishings;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.MultiListProperty;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.comparators.ProjectViewerComparator;
import org.jboss.tools.openshift.internal.ui.treeitem.Model2ObservableTreeItemConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 */
public class SelectResourceWizard extends AbstractOpenShiftWizard<ServerResourceViewModel> {

	private String description;

	public SelectResourceWizard(String description, IResource resource, IConnection connection) {
		super("Select Resource", new ServerResourceViewModel(resource, connection));
		this.description = description;
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	@Override
	public void addPages() {
		addPage(new SelectResourceWizardPage());
	}
	
	public IResource getResource() {
		return getModel().getResource();
	}
	
	public class SelectResourceWizardPage extends AbstractOpenShiftWizardPage {

		public SelectResourceWizardPage() {
			super(getWindowTitle(), description, "", SelectResourceWizard.this);
		}

		@Override
		protected void doCreateControls(Composite parent, DataBindingContext dbc) {
			GridLayoutFactory.fillDefaults()
				.margins(10, 10)
				.applyTo(parent);

			Composite container = new Composite(parent, SWT.None);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).grab(true, true)
					.applyTo(container);
			GridLayoutFactory.fillDefaults()
					.applyTo(container);

			// services
			Composite serviceComposite = createServiceControls(container, dbc);
			GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.applyTo(serviceComposite);

			loadResources();
		}

		private Composite createServiceControls(Composite container, DataBindingContext dbc) {
			Group servicesGroup = new Group(container, SWT.NONE);
			servicesGroup.setText("Services");
			GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(10,10)
				.applyTo(servicesGroup);

			Label selectorLabel = new Label(servicesGroup, SWT.NONE);
			selectorLabel.setText("Selector:");
			Text selectorText = UIUtils.createSearchText(servicesGroup);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(selectorText);

			final TreeViewer resourcesViewer = createServicesTreeViewer(servicesGroup, selectorText);
			IObservableList resourceItemsObservable = BeanProperties.list(ServerResourceViewModel.PROPERTY_RESOURCE_ITEMS).observe(getModel());
			DataBindingUtils.addDisposableListChangeListener(
					onServiceItemsChanged(resourcesViewer), resourceItemsObservable, resourcesViewer.getTree());
			GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).hint(SWT.DEFAULT, 160).grab(true, true)
				.applyTo(resourcesViewer.getControl());
			selectorText.addModifyListener(onFilterTextModified(resourcesViewer));
			IViewerObservableValue selectedResourceTreeItem = ViewerProperties.singleSelection().observe(resourcesViewer);
			ValueBindingBuilder
					.bind(selectedResourceTreeItem)
					.converting(new ObservableTreeItem2ModelConverter(IResource.class))
					.validatingAfterConvert(new IValidator() {
						
						@Override
						public IStatus validate(Object value) {
							if ((value instanceof IResource) && OpenShiftServerUtils.isAllowedForServerAdapter((IResource) value)) {
                                return ValidationStatus.ok();
							} else {
                                return ValidationStatus.cancel("Please select a resource that your adapter will publish to.");
							}
							
						}
					})
					.to(BeanProperties.value(ServerResourceViewModel.PROPERTY_RESOURCE).observe(getModel()))
					.converting(new Model2ObservableTreeItemConverter(new ServerSettingsWizardPageModel.ResourceTreeItemsFactory()))
					.in(dbc);

			// details
			Label detailsLabel = new Label(servicesGroup, SWT.NONE);
			detailsLabel.setText("Resource Details:");
			GridDataFactory.fillDefaults()
					.span(2, 1).align(SWT.FILL, SWT.FILL)
					.applyTo(detailsLabel);

			Composite detailsContainer = new Composite(servicesGroup, SWT.NONE);
			GridDataFactory.fillDefaults()
					.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150)
					.applyTo(detailsContainer);
			IObservableValue selectedResource = new WritableValue();
			ValueBindingBuilder
				.bind(selectedResourceTreeItem)
				.converting(new ObservableTreeItem2ModelConverter())
				.to(selectedResource)
				.notUpdatingParticipant()
				.in(dbc);
			new ResourceDetailViews(selectedResource, detailsContainer, dbc).createControls();

			return servicesGroup;
		}

		private IDoubleClickListener onDoubleClickService() {
			return new IDoubleClickListener() {

				@Override
				public void doubleClick(DoubleClickEvent event) {
					if (canFinish()) {
						Button finishButton = getShell().getDefaultButton();
						UIUtils.clickButton(finishButton);
					}
				}
			};
		}

		private IListChangeListener onServiceItemsChanged(final TreeViewer servicesViewer) {
			return new IListChangeListener() {
				
				@Override
				public void handleListChange(ListChangeEvent event) {
					servicesViewer.expandAll();
				}
			};
		}

		private TreeViewer createServicesTreeViewer(Composite parent, Text selectorText) {
			TreeViewer applicationTemplatesViewer =
					new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
			IListProperty childrenProperty = new MultiListProperty(
					new IListProperty[] {
							BeanProperties.list(ServerSettingsWizardPageModel.PROPERTY_RESOURCE_ITEMS),
							BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN) });
			ObservableListTreeContentProvider contentProvider =
					new ObservableListTreeContentProvider(childrenProperty.listFactory(), null);
			applicationTemplatesViewer.setContentProvider(contentProvider);
			applicationTemplatesViewer.setLabelProvider(new ResourcesViewLabelProvider());
			applicationTemplatesViewer.addFilter(new ServiceViewerFilter(selectorText));
			applicationTemplatesViewer.setComparator(ProjectViewerComparator.createProjectTreeSorter());
			applicationTemplatesViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
			applicationTemplatesViewer.setInput(getModel());
			return applicationTemplatesViewer;
		}	

		protected ModifyListener onFilterTextModified(final TreeViewer applicationTemplatesViewer) {
			return new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent e) {
					applicationTemplatesViewer.refresh();
					applicationTemplatesViewer.expandAll();
				}
			};
		}

		/**
		 * Loads the resources for this view, does it in a blocking way.
		 */
		private void loadResources() {
			final IStatus[] stats = new IStatus[1];
			try {
				WizardUtils.runInWizard(new Job("Loading services...") {
					
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							getModel().loadResources();
							stats[0] = Status.OK_STATUS;
						} catch(OpenShiftException ose) {
							stats[0] = OpenShiftUIActivator.statusFactory().errorStatus("Unable to load services from the given connection", ose);
						}
						//initializing the project combo with the project selected in the workspace
						return Status.OK_STATUS;
					}
				}, getWizard().getContainer());
			} catch (InvocationTargetException | InterruptedException e) {
				// swallow intentionally
			}
			if( !stats[0].isOK()) {
				setErrorMessage(stats[0].getMessage());
			}
		}

	}
}
