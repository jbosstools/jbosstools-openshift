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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.internal.databinding.conversion.ObjectToStringConverter;
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
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.linuxtools.docker.core.AbstractRegistry;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.ui.wizards.ImageSearch;
import org.eclipse.linuxtools.internal.docker.core.RegistryInfo;
import org.eclipse.linuxtools.internal.docker.ui.wizards.NewDockerConnection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
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
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionColumLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.comparators.ProjectViewerComparator;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemLabelProvider;
import org.jboss.tools.openshift.internal.ui.validator.DockerImageValidator;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceNameControl;
import org.jboss.tools.openshift.internal.ui.wizard.project.NewProjectWizard;

import com.openshift.restclient.model.IProject;

/**
 * Page to (mostly) edit the config items for a page
 * 
 * @author jeff.cantrill
 */
public class DeployImagePage extends AbstractOpenShiftWizardPage {
	private static final String MISSING_DOCKER_CONNECTION_MSG = "You must select a Docker connection.";

	static String DEPLOY_IMAGE_PAGE_NAME = "Deployment Config Settings Page";

	private static final String PAGE_DESCRIPTION = "This page allows you to choose an image and the name to be used for the deployed resources.";

	private IDeployImagePageModel model;

	ContentProposalAdapter imageNameProposalAdapter;

	protected DeployImagePage(IWizard wizard, IDeployImagePageModel model) {
		super("Deploy an Image", PAGE_DESCRIPTION, DEPLOY_IMAGE_PAGE_NAME, wizard);
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
	@Override
	protected void onPageWillGetDeactivated(final Direction progress, final PageChangingEvent event, final DataBindingContext dbc) {
		if(imageNameProposalAdapter != null) {
			imageNameProposalAdapter.setEnabled(false);
		}
		if(progress == Direction.BACKWARDS) {
			//Do not block return to change connection.
			return;
		}
		
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
			final IStatus validatorJobStatus = WizardUtils.runInWizard(imageValidator, getContainer(), getDataBindingContext());
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
	protected void onPageWillGetActivated(final Direction progress, final PageChangingEvent event, final DataBindingContext dbc) {
		if(imageNameProposalAdapter != null) {
			imageNameProposalAdapter.setEnabled(true);
		}
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);
		createOpenShiftConnectionControl(parent, dbc);
		createProjectControl(parent, dbc);
		createSeparator(parent);
		if (!model.originatedFromDockerExplorer()) {
			createDockerConnectionControl(parent, dbc);
		} else {
			createDockerConnectionInfoControl(parent, dbc);
		}
		createImageNameControls(parent, dbc);
		new ResourceNameControl().doCreateControl(parent, dbc, model);
	}

	private void createSeparator(Composite parent) {
		GridDataFactory
			.fillDefaults()
			.align(SWT.FILL, SWT.BEGINNING)
			.grab(true, false)
			.span(3, 1)
			.applyTo(new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL));
	}

	private SelectionAdapter onSearch(Text txtImage) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (model.getDockerConnection() == null) {
					MessageDialog.openError(getShell(), "A Docker connection must be selected", MISSING_DOCKER_CONNECTION_MSG);
					return;
				}
				// FIXME: may need to revisit the call to the constructor once https://bugs.eclipse.org/bugs/show_bug.cgi?id=495285 is addressed
				// there may be no need to to specify the registry info if we want to search on Docker Hub.
				ImageSearch wizard = new ImageSearch(model.getDockerConnection(), txtImage.getText(), new RegistryInfo(AbstractRegistry.DOCKERHUB_REGISTRY));
				if(Window.OK == new OkCancelButtonWizardDialog(getShell(), wizard).open()){
					//this bypasses validation
					model.setImageName(wizard.getSelectedImage());
				}
			}
		};
	}
	
	private void createDockerConnectionControl(Composite parent, DataBindingContext dbc) {
		createDockerConnectionLabel(parent);
		
		StructuredViewer connectionViewer = new ComboViewer(parent);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.span(1, 1)
			.applyTo(connectionViewer.getControl());
		
		connectionViewer.setContentProvider(new ObservableListContentProvider());
		connectionViewer.setLabelProvider(new ObservableTreeItemLabelProvider() {

			@Override
			public String getText(Object element) {
				return (element instanceof IDockerConnection) ? dockerConnectionToString((IDockerConnection) element) : "";
			}
			
		});
		connectionViewer.setInput(
				BeanProperties.list(IDeployImagePageModel.PROPERTY_DOCKER_CONNECTIONS).observe(model));
		
		IObservableValue dockerConnectionObservable = BeanProperties.value(IDeployImagePageModel.PROPERTY_DOCKER_CONNECTION)
				.observe(model);
		DockerConnectionStatusProvider validator = new DockerConnectionStatusProvider(dockerConnectionObservable);
		IObservableValue selectedConnectionObservable = ViewerProperties.singleSelection().observe(connectionViewer);
		Binding selectedConnectionBinding = 
			ValueBindingBuilder.bind(selectedConnectionObservable)
			.converting(new ObservableTreeItem2ModelConverter(IDockerConnection.class))
			.validatingAfterConvert(validator)
			.to(BeanProperties.value(IDeployImagePageModel.PROPERTY_DOCKER_CONNECTION)
			.observe(model))
			.in(dbc);
		ControlDecorationSupport.create(
			selectedConnectionBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
		Button newDockerConnectionButton = new Button(parent, SWT.PUSH);
		newDockerConnectionButton.setText("New...");
        GridDataFactory.fillDefaults()
            .align(SWT.LEFT, SWT.CENTER)
            .applyTo(newDockerConnectionButton);
        UIUtils.setDefaultButtonWidth(newDockerConnectionButton);
        newDockerConnectionButton.addSelectionListener(onNewDockerConnectionClicked());

        dbc.addValidationStatusProvider(validator);
	}

	private String dockerConnectionToString(IDockerConnection conn) {
		return NLS.bind("{0} ({1})", conn.getName(), conn.getUri());
	}

	private Label createDockerConnectionLabel(Composite parent) {
		Label lblConnection = new Label(parent, SWT.NONE);
		lblConnection.setText("Docker Connection: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(lblConnection);
		return lblConnection;
	}

	@SuppressWarnings("rawtypes")
	class DockerConnectionStatusProvider extends MultiValidator implements IValidator {
		IObservableValue dockerConnectionObservable;

		DockerConnectionStatusProvider(IObservableValue dockerConnectionObservable) {
			this.dockerConnectionObservable = dockerConnectionObservable;
		}

		@Override
		public IStatus validate(Object value) {
			if (value instanceof IDockerConnection) {
				return ValidationStatus.ok();
			}
			return ValidationStatus.cancel(MISSING_DOCKER_CONNECTION_MSG);
		}

		@Override
		protected IStatus validate() {
			return validate(dockerConnectionObservable.getValue());
		}
	}

	private void createDockerConnectionInfoControl(Composite parent, DataBindingContext dbc) {
		Label lblConnection = createDockerConnectionLabel(parent);
		final Text connectionText = new Text(parent, SWT.READ_ONLY | SWT.NO_FOCUS);
		connectionText.setBackground(lblConnection.getBackground());
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.span(2, 1)
			.grab(true, false)
			.applyTo(connectionText);
		final IObservableValue connnectionTextObservable = WidgetProperties.text(SWT.None).observe(connectionText);
		final IObservableValue connnectionObservable = BeanProperties.value(IDeployImagePageModel.PROPERTY_DOCKER_CONNECTION).observe(model);
		ValueBindingBuilder.bind(connnectionTextObservable)
			.notUpdatingParticipant()
			.to(connnectionObservable)
			.converting(new ObjectToStringConverter(IDockerConnection.class) {
				ConnectionColumLabelProvider labelProvider = new ConnectionColumLabelProvider();
				@Override
				public Object convert(Object source) {
					return (source instanceof IDockerConnection) ? dockerConnectionToString((IDockerConnection) source) : "";
				}
			})
			.in(dbc);
	}

	private void createOpenShiftConnectionControl(Composite parent, DataBindingContext dbc) {
		Label lblConnection = new Label(parent, SWT.NONE);
		lblConnection.setText("OpenShift Connection: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(lblConnection);
		final Text connectionText = new Text(parent, SWT.READ_ONLY | SWT.NO_FOCUS);
		connectionText.setBackground(lblConnection.getBackground());
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.span(2, 1)
			.grab(true, false)
			.applyTo(connectionText);
		final IObservableValue connnectionTextObservable = WidgetProperties.text(SWT.None).observe(connectionText);
		final IObservableValue connnectionObservable = BeanProperties.value(IDeployImagePageModel.PROPERTY_CONNECTION).observe(model);
		ValueBindingBuilder.bind(connnectionTextObservable)
			.notUpdatingParticipant()
			.to(connnectionObservable)
			.converting(new ObjectToStringConverter(Connection.class) {
				ConnectionColumLabelProvider labelProvider = new ConnectionColumLabelProvider();
				@Override
				public Object convert(Object source) {
					return source == null ? "" : labelProvider.getText(source);
				}
			})
			.in(dbc);
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
		cmboProject.setComparator(new ProjectViewerComparator(labelProvider));

		IObservableValue projectObservable = BeanProperties.value(IDeployImagePageModel.PROPERTY_PROJECT)
				.observe(model);
		ProjectStatusProvider validator = new ProjectStatusProvider(projectObservable);
		IObservableValue selectedProjectObservable = ViewerProperties.singleSelection().observe(cmboProject);
		Binding selectedProjectBinding = 
			ValueBindingBuilder.bind(selectedProjectObservable)
			.converting(new ObservableTreeItem2ModelConverter(IProject.class))
			.validatingAfterConvert(validator)
			.to(projectObservable)
			.in(dbc);
		ControlDecorationSupport.create(
				selectedProjectBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
		Button newProjectButton = new Button(parent, SWT.PUSH);
		newProjectButton.setText("New...");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER)
			.applyTo(newProjectButton);
		UIUtils.setDefaultButtonWidth(newProjectButton);
		newProjectButton.addSelectionListener(onNewProjectClicked());

        dbc.addValidationStatusProvider(validator);

        cmboProject.getControl().forceFocus();
	}

	@SuppressWarnings("rawtypes")
	class ProjectStatusProvider extends MultiValidator implements IValidator {
		IObservableValue projectObservable;

		ProjectStatusProvider(IObservableValue projectObservable) {
			this.projectObservable = projectObservable;
		}

		@Override
		public IStatus validate(Object value) {
			if (value instanceof IProject) {
				return ValidationStatus.ok();
			}
			return ValidationStatus.cancel("Please choose an OpenShift project.");
		}

		@Override
		protected IStatus validate() {
			return validate(projectObservable.getValue());
		}
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

		imageNameProposalAdapter = new ContentProposalAdapter(imageNameText,
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
	
	private SelectionAdapter onNewProjectClicked() {
	    return new SelectionAdapter() {

            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    // run in job to enforce busy cursor which doesnt work otherwise
                    WizardUtils.runInWizard(new UIUpdatingJob("Opening projects wizard...") {

                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            return Status.OK_STATUS;
                        }

                        @Override
                        protected IStatus updateUI(IProgressMonitor monitor) {
                            NewProjectWizard newProjectWizard = new NewProjectWizard(model.getConnection(), (List<IProject>) model.getProjects());
                            int result = new OkCancelButtonWizardDialog(getShell(), newProjectWizard).open();
                            // reload projects to reflect changes that happened in
                            // projects wizard
                            if (newProjectWizard.getProject() != null) {
                                List<IProject> projects = new ArrayList<>(model.getProjects());
                                projects.add(newProjectWizard.getProject());
                                model.setProjects(projects);
                            }
                            if (Dialog.OK == result) {
                                IProject selectedProject = newProjectWizard.getProject();
                                if (selectedProject != null) {
                                    model.setProject(selectedProject);
                                }
                            }
                            return Status.OK_STATUS;
                        }
                    }, getContainer(), getDataBindingContext());
                } catch (InvocationTargetException | InterruptedException ex) {
                    // swallow intentionnally
                }
            }
	    };
	}
	
    private SelectionAdapter onNewDockerConnectionClicked() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    // run in job to enforce busy cursor which doesnt work otherwise
                    WizardUtils.runInWizard(new UIUpdatingJob("Opening new Docker connection wizard...") {

                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            return Status.OK_STATUS;
                        }

                        @Override
                        protected IStatus updateUI(IProgressMonitor monitor) {
                            NewDockerConnection newDockerConnectionWizard = new NewDockerConnection();
                            int result = new OkCancelButtonWizardDialog(getShell(), newDockerConnectionWizard).open();
                            // set docker connection to reflect changes that happened in
                            // docker connection wizard
                            if (Dialog.OK == result) {
                                IDockerConnection dockerConnection = newDockerConnectionWizard.getDockerConnection();
                                if(dockerConnection != null) {
                                    model.setDockerConnection(dockerConnection);
                                }
                            }
                            return Status.OK_STATUS;
                        }
                    }, getContainer(), getDatabindingContext());
                } catch (InvocationTargetException | InterruptedException ex) {
                    OpenShiftUIActivator.getDefault().getLogger().logError(ex);
                }
            }
        };
    }
}
