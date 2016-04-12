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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.ui.wizards.ImageSearch;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemLabelProvider;
import org.jboss.tools.openshift.internal.ui.validator.DockerImageValidator;
import org.jboss.tools.openshift.internal.ui.validator.ServiceNameValidator;
import org.jboss.tools.openshift.internal.ui.wizard.project.ManageProjectsWizard;

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
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR |  IStatus.CANCEL, this, dbc);
	}

	/**
	 * Callback that gets called when this page is going to be deactivated. This
	 * is the chance to do a one-time remote call to the selected Docker daemon
	 * to check if an image with the given name exists in the local cache or in
	 * the remote registry (Docker Hub). This kind of validation is a
	 * long-running process, so it should not be performed during the field
	 * validation.
	 * 
	 * @param progress
	 *            the direction that the wizard is moving: backwards/forwards
	 * @param event
	 *            the page changing event that may be use to veto the change
	 * @param dbc
	 *            the current data binding context
	 */
	protected void onPageWillGetDeactivated(final Direction progress, final PageChangingEvent event, final DataBindingContext dbc) {
		
		/**
		 * Inner class to perform the image search in the selected Docker daemon cache or on the remote registry. 
		 */
		class ImageValidatorJob extends Job {

			public ImageValidatorJob(String name) {
				super(name);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if(model.initializeContainerInfo()) {
					return Status.OK_STATUS;
				}
				return Status.CANCEL_STATUS;
			}
		}
		
		final ImageValidatorJob imageValidator = new ImageValidatorJob("Looking-up the selected Docker image...");
		try {
			final IStatus validatorJobStatus = WizardUtils.runInWizard(imageValidator, getContainer());
			if (!validatorJobStatus.isOK()) {
				MessageDialog.openError(getShell(), "Error",
						"No Docker image named '" + model.getImageName() + "' could be found.");
				event.doit = false;
			}
		} catch (InvocationTargetException | InterruptedException e) {
			final String message = "Failed to look-up metadata for a Docker image named '" + model.getImageName() + "'.";
			MessageDialog.openError(getShell(), "Error", message);
			OpenShiftUIActivator.getDefault().getLogger().logError(message, e);
		}
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);
		if (!model.originatedFromDockerExplorer()) {
			createDockerConnectionControl(parent, dbc);
		}
		createProjectControl(parent, dbc);
		createImageNameControls(parent, dbc);
		createResourceNameControls(parent, dbc);
	}

	private SelectionAdapter onSearch(Text txtImage) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (model.getDockerConnection() == null) {
					MessageDialog.openError(getShell(), "A Docker connection must be selected", "You must first select a Docker connection.");
					return;
				}
				ImageSearch wizard = new ImageSearch(model.getDockerConnection(), txtImage.getText());
				if(Window.OK == new OkCancelButtonWizardDialog(getShell(), wizard).open()){
					//this bypasses validation
					model.setImageName(wizard.getSelectedImage());
				}
			}
		};
	}
	
	private void createDockerConnectionControl(Composite parent, DataBindingContext dbc) {
		Label lblConnection = new Label(parent, SWT.NONE);
		lblConnection.setText("Docker Connection: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(lblConnection);
		
		StructuredViewer connectionViewer = new ComboViewer(parent);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.span(2, 1)
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
		
		StyledText manageProjectsLink = StyledTextUtils.emulateLinkWidget("<a>Manage Projects</a>", new StyledText(parent, SWT.WRAP));
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).indent(8, 0)
			.applyTo(manageProjectsLink);
		StyledTextUtils.emulateLinkAction(manageProjectsLink, r->onManageProjectsClicked());
	}

	private void createImageNameControls(final Composite parent, final DataBindingContext dbc) {
		//Image
		final Label imageNameLabel = new Label(parent, SWT.NONE);
		imageNameLabel.setText("Image Name: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(imageNameLabel);
		final Text imageNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.applyTo(imageNameText);
		final IObservableValue imageNameTextObservable = 
				WidgetProperties.text(SWT.Modify).observeDelayed(500, imageNameText);
		final IObservableValue imageNameObservable = BeanProperties.value(IDeployImagePageModel.PROPERTY_IMAGE_NAME).observe(model);
		Binding imageBinding = ValueBindingBuilder.bind(imageNameTextObservable)
				.validatingAfterConvert(new DockerImageValidator())
				.to(imageNameObservable).in(dbc);
		ControlDecorationSupport.create(
				imageBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		new ContentProposalAdapter(imageNameText,
				// override the text value before content assist was invoked and
				// move the cursor to the end of the selected value
				new TextContentAdapter() {
					@Override
					public void insertControlContents(Control control,
							String text, int cursorPosition) {
						final Text imageNameText = (Text) control;
						final Point selection = imageNameText.getSelection();
						imageNameText.setText(text);
						selection.x = text.length();
						selection.y = selection.x;
						imageNameText.setSelection(selection);
					}
				}, getImageNameContentProposalProvider(imageNameText),
				null, null);
		//browse
		Button btnDockerSearch = new Button(parent, SWT.NONE);
		btnDockerSearch.setText("Search...");
		btnDockerSearch.setToolTipText("Look-up an image by browsing the docker daemon");
		btnDockerSearch.addSelectionListener(onSearch(imageNameText));
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(btnDockerSearch);
	
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(btnDockerSearch))
				.notUpdatingParticipant()
				.to(BeanProperties.value(IDeployImagePageModel.PROPERTY_DOCKER_CONNECTION).observe(model))
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);
	}

	/**
	 * Creates an {@link IContentProposalProvider} to propose
	 * {@link IDockerImage} names based on the current text.
	 * 
	 * @param items
	 * @return
	 */
	private IContentProposalProvider getImageNameContentProposalProvider(
			final Text imageNameText) {
		return new IContentProposalProvider() {

			@Override
			public IContentProposal[] getProposals(final String input,
					final int position) {
				return model.getImageNames().stream().filter(name -> name.contains(input))
						.map(n -> new ContentProposal(n, n, null, position))
						.toArray(size -> new IContentProposal[size]);
			}
		};
	}
	private void createResourceNameControls(final Composite parent, final DataBindingContext dbc) {
		//Resource Name
		final Label resourceNameLabel = new Label(parent, SWT.NONE);
		resourceNameLabel.setText("Resource Name: ");
		resourceNameLabel.setToolTipText("The name used to identify the resources that will support the deployed image.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(resourceNameLabel);
		final Text resourceNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.span(2, 1)
			.applyTo(resourceNameText);
		final IObservableValue resourceNameTextObservable = 
				WidgetProperties.text(SWT.Modify).observe(resourceNameText);
		final Binding nameBinding = ValueBindingBuilder
				.bind(resourceNameTextObservable)
				.to(BeanProperties.value(IDeployImagePageModel.PROPERTY_RESOURCE_NAME).observe(model))
				.in(dbc);
		dbc.addValidationStatusProvider(new DeployImageNameValidator(resourceNameTextObservable));
		ControlDecorationSupport.create(
				nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
	}
	
	private static class DeployImageNameValidator extends MultiValidator{
		
		private ServiceNameValidator validator = new ServiceNameValidator();
		final private IObservableValue observable;
		DeployImageNameValidator(IObservableValue observable){
			this.observable = observable;
		}
		@Override
		protected IStatus validate() {
			return validator.validate(observable.getValue());
		}
		
	}

	private void onManageProjectsClicked() {
		try {
			// run in job to enforce busy cursor which doesnt work otherwise
			WizardUtils.runInWizard(new UIUpdatingJob("Opening projects wizard...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					return Status.OK_STATUS;
				}

				@Override
				protected IStatus updateUI(IProgressMonitor monitor) {
					ManageProjectsWizard manageProjectsWizard = new ManageProjectsWizard(model.getConnection());
					int result = new OkCancelButtonWizardDialog(getShell(), manageProjectsWizard).open();
					// reload projects to reflect changes that happened in
					// projects wizard
					if (manageProjectsWizard.hasChanged()) {
						model.setProjects(manageProjectsWizard.getProjects());
					}
					if (Dialog.OK == result) {
						IProject selectedProject = manageProjectsWizard.getSelectedProject();
						if (selectedProject != null) {
							model.setProject(selectedProject);
						}
					}
					return Status.OK_STATUS;
				}
			}, getContainer());
		} catch (InvocationTargetException | InterruptedException e) {
			// swallow intentionnally
		}
	}
	
}
