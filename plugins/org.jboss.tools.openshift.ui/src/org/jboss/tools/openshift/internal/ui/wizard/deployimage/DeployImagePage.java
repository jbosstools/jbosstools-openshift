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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemLabelProvider;
import org.jboss.tools.openshift.internal.ui.validator.DeployImageNameValidator;

import com.openshift.restclient.model.IProject;

/**
 * Page to (mostly) edit the config items for a page
 * 
 * @author jeff.cantrill
 */
public class DeployImagePage extends AbstractOpenShiftWizardPage {

	private static final String PAGE_DESCRIPTION = "This page allows you to choose an image and the name to be used for the deployed resources.";

	private IDeployImagePageModel model;

	protected DeployImagePage(IWizard wizard, IDeployImagePageModel model) {
		super("Deploy an Image", PAGE_DESCRIPTION, "Deployment Config Settings Page", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.numColumns(2)
			.margins(10, 10)
			.applyTo(parent);

		if(model.originatedFromDockerExplorer()) {
			createConnectionControl(parent, dbc);
		}else {
			createDockerConnectionControl(parent, dbc);
		}
		createProjectControl(parent, dbc);
		
		//Image
		Label lblImage = new Label(parent, SWT.NONE);
		lblImage.setText("Image: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(lblImage);
		Text txtImage = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.applyTo(txtImage);
		IObservableValue imageTextObservable = 
				WidgetProperties.text(SWT.Modify).observeDelayed(500, txtImage);
		Binding imageBinding = ValueBindingBuilder
			.bind(imageTextObservable)
			.validatingAfterConvert(new DockerImageValidator())
			.to(BeanProperties.value(IDeployImagePageModel.PROPERTY_IMAGE).observe(model))
			.in(dbc);
		ControlDecorationSupport.create(
				imageBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		//Resource Name
		Label lblName = new Label(parent, SWT.NONE);
		lblName.setText("Resource Name: ");
		lblName.setToolTipText("The name used to identify the resources that will support the deployed image.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(lblName);
		Text txtName = new Text(parent, SWT.BORDER);
//		txtName.setEditable(false);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.applyTo(txtName);
		IObservableValue nameTextObservable = 
				WidgetProperties.text(SWT.Modify).observe(txtName);
		Binding nameBinding = ValueBindingBuilder
				.bind(nameTextObservable)
				.validatingAfterConvert(new DeployImageNameValidator())
				.to(BeanProperties.value(IDeployImagePageModel.PROPERTY_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
	}
	
	private void createDockerConnectionControl(Composite parent, DataBindingContext dbc) {
		Label lblConnection = new Label(parent, SWT.NONE);
		lblConnection.setText("Docker Connection: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(lblConnection);
		
		StructuredViewer connectionViewer = new ComboViewer(parent);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(connectionViewer.getControl());
		
		connectionViewer.setContentProvider(new ObservableListContentProvider());
		connectionViewer.setLabelProvider(new ObservableTreeItemLabelProvider() {

			@Override
			public String getText(Object element) {
				if(!(element instanceof IDockerConnection)) return "";
				IDockerConnection conn = (IDockerConnection) element;
				return NLS.bind("{0} ({1})", conn.getName(), conn.getUri());
			}
			
		});
		connectionViewer.setInput(
				BeanProperties.list(IDeployImagePageModel.PROPERTY_DOCKER_CONNECTIONS).observe(model));
		
		IObservableValue selectedConnectionObservable = ViewerProperties.singleSelection().observe(connectionViewer);
		Binding selectedConnectionBinding = 
			ValueBindingBuilder.bind(selectedConnectionObservable)
			.converting(new ObservableTreeItem2ModelConverter(IDockerConnection.class))
			.validatingAfterConvert(new IValidator() {
				
				@Override
				public IStatus validate(Object value) {
					if (value instanceof IDockerConnection) {
						return ValidationStatus.ok();
					}
					return ValidationStatus.cancel("Please choose Docker connection.");
				}
			})
			.to(BeanProperties.value(IDeployImagePageModel.PROPERTY_DOCKER_CONNECTION)
			.observe(model))
			.in(dbc);
		ControlDecorationSupport.create(
			selectedConnectionBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));	}

	private class DockerImageValidator implements IValidator {

		@Override
		public IStatus validate(Object value) {
			String uri = (String) value;
			if(StringUtils.isEmpty(uri) || !model.imageExists(uri)) {
				return ValidationStatus.cancel("Please provide an existing docker image in the format of <repo>/<namespace>/<name>:<tag>. The defaults are: repo=docker.io, namespace=library, tag=latest");
			}
			return ValidationStatus.ok();
		}
	}
	
	private void createConnectionControl(Composite parent, DataBindingContext dbc) {
		Label lblConnection = new Label(parent, SWT.NONE);
		lblConnection.setText("OpenShift Connection: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(lblConnection);
		
		StructuredViewer connectionViewer = new ComboViewer(parent);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(connectionViewer.getControl());
		
		connectionViewer.setContentProvider(new ObservableListContentProvider());
		connectionViewer.setLabelProvider(new ObservableTreeItemLabelProvider());
		connectionViewer.setInput(
				BeanProperties.list(IDeployImagePageModel.PROPERTY_CONNECTIONS).observe(model));
		
		IObservableValue selectedConnectionObservable = ViewerProperties.singleSelection().observe(connectionViewer);
		Binding selectedConnectionBinding = 
			ValueBindingBuilder.bind(selectedConnectionObservable)
			.converting(new ObservableTreeItem2ModelConverter(Connection.class))
			.validatingAfterConvert(new IValidator() {
				
				@Override
				public IStatus validate(Object value) {
					if (value instanceof Connection) {
						return ValidationStatus.ok();
					}
					return ValidationStatus.cancel("Please choose an OpenShift connection.");
				}
			})
			.to(BeanProperties.value(IDeployImagePageModel.PROPERTY_CONNECTION)
			.observe(model))
			.in(dbc);
		ControlDecorationSupport.create(
			selectedConnectionBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
	}

	private void createProjectControl(Composite parent, DataBindingContext dbc) {
		Label lblProject = new Label(parent, SWT.NONE);
		lblProject.setText("OpenShift Project: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(lblProject);
		
		StructuredViewer cmboProject = new ComboViewer(parent);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.hint(SWT.DEFAULT, 30)
			.applyTo(cmboProject.getControl());
		
		final OpenShiftExplorerLabelProvider labelProvider = new OpenShiftExplorerLabelProvider();
		cmboProject.setContentProvider(new ObservableListContentProvider());
		cmboProject.setLabelProvider(labelProvider);
		cmboProject.setInput(
				BeanProperties.list(IDeployImagePageModel.PROPERTY_PROJECTS).observe(model));
		cmboProject.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return labelProvider.getText(e1).compareTo(labelProvider.getText(e2));
			}
			
		});
		IObservableValue selectedProjectObservable = ViewerProperties.singleSelection().observe(cmboProject);
		Binding selectedProjectBinding = 
			ValueBindingBuilder.bind(selectedProjectObservable)
			.converting(new ObservableTreeItem2ModelConverter(IProject.class))
			.validatingAfterConvert(new IValidator() {
				
				@Override
				public IStatus validate(Object value) {
					if (value instanceof IProject) {
						return ValidationStatus.ok();
					}
					return ValidationStatus.cancel("Please choose an OpenShift project.");
				}
			})
			.to(BeanProperties.value(IDeployImagePageModel.PROPERTY_PROJECT)
			.observe(model))
			.in(dbc);
		ControlDecorationSupport.create(
				selectedProjectBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
	}

}
