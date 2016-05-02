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
package org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariablePage;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceNameControl;

/**
 * Page to (mostly) edit the build config items for a page
 * 
 * @author jeff.cantrill
 */
public class BuildConfigPage extends EnvironmentVariablePage {

	public static final String PAGE_NAME = "Deployment Config Settings Page";
	private static final String PAGE_TITLE = "Build Configuration";
	private static final String PAGE_DESCRIPTION = "";

	private IBuildConfigPageModel model;

	public BuildConfigPage(IWizard wizard, IBuildConfigPageModel model) {
		super(PAGE_TITLE, PAGE_DESCRIPTION, PAGE_NAME, wizard,  model.getEnvVariablesModel());
		this.model = model;
	}

	@Override
	protected void onPageWillGetActivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		model.init();
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 2).applyTo(parent);
		
		Composite nameParent = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(nameParent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameParent);
		//basename for resources
		new ResourceNameControl("Name: ") {

			@Override
			protected void layoutLabel(Label resourceNameLabel) {
				GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.grab(false, false)
					.applyTo(resourceNameLabel);
			}

			@Override
			protected void layoutText(Text resourceNameText) {
				GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.span(2, 1)
				.applyTo(resourceNameText);
			}
			
			
		}.doCreateControl(nameParent, dbc, model);
		createSeparator(parent);
		
		//git info
		createSourceControls(parent, dbc);
		createSeparator(parent);
		
		// build triggers
		createTriggers(parent, dbc);
		createSeparator(parent);

		//Env Variables Block
		createEnvVariableControl(parent, dbc);
		setTableLabel(EnvironmentVariablePage.TABLE_LABEL + " (Build and Runtime):");
		setTableLabelToolToop("Environment variables are used to configure and pass information to running containers.  These environment variables will be available during your build and at runtime.");
	}
	
	@SuppressWarnings("unchecked")
	private void createTriggers(Composite parent, DataBindingContext dbc) {
		
		Label triggerLabel = new Label(parent, SWT.NONE);
		triggerLabel.setText("Build Triggers:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.applyTo(triggerLabel);
		
		//webhook
		Button webHookBtn = new Button(parent, SWT.CHECK);
		webHookBtn.setText("Configure a webhook build trigger");
		webHookBtn.setToolTipText("The source repository must be configured to use the webhook to trigger a build when source is committed.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(webHookBtn);
		ValueBindingBuilder.bind(WidgetProperties.selection().observe(webHookBtn))
			.to(BeanProperties.value(IBuildConfigPageModel.PROPERTY_CONFIG_WEB_HOOK)
			.observe(model))
			.in(dbc);
		
		//image change
		Button imageChangeBtn = new Button(parent, SWT.CHECK);
		imageChangeBtn.setText("Automatically build a new image when the builder image changes");
		imageChangeBtn.setToolTipText("Automatically building a new image when the builder image changes allows your code to always run on the latest updates.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(imageChangeBtn);
		ValueBindingBuilder.bind(WidgetProperties.selection().observe(imageChangeBtn))
			.to(BeanProperties.value(IBuildConfigPageModel.PROPERTY_IMAGE_CHANGE_TRIGGER)
			.observe(model))
			.in(dbc);
		
		//build config change
		Button configChangeBtn = new Button(parent, SWT.CHECK);
		configChangeBtn.setText("Automatically build a new image when the build configuration changes");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(configChangeBtn);
		ValueBindingBuilder.bind(WidgetProperties.selection().observe(configChangeBtn))
			.to(BeanProperties.value(IBuildConfigPageModel.PROPERTY_CONFIG_CHANGE_TRIGGER)
			.observe(model))
			.in(dbc);
		
	}

	private void createSeparator(Composite parent) {
		GridDataFactory
			.fillDefaults()
			.align(SWT.FILL, SWT.BEGINNING)
			.grab(true, false)
			.applyTo(new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL));
	}

	@SuppressWarnings({ "unchecked"})
	private void createSourceControls(Composite root, DataBindingContext dbc) {
		Composite parent = new Composite(root, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(parent);
		GridLayoutFactory.fillDefaults().
			numColumns(3).applyTo(parent);
		
		//url
		Label gitUrlLabel = new Label(parent, SWT.NONE);
		gitUrlLabel.setText("Git Repository URL:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(false, false)
			.applyTo(gitUrlLabel);
		
		final Text gitUrlText = new Text(parent, SWT.BORDER);
		gitUrlText.setToolTipText("The URL to the Git repository.");
		//TODO add 'try it' link here
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.span(2, 1)
			.applyTo(gitUrlText);

		Binding gitUrlBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observeDelayed(500, gitUrlText))
				.validatingAfterConvert(new IValidator() {
					
					@Override
					public IStatus validate(Object value) {
						if(UrlUtils.isValid((String) value)){
							return Status.OK_STATUS;
						}
						return ValidationStatus.error("A valid URL to a Git repository is required");
					}
				})
				.to(BeanProperties.value(IBuildConfigPageModel.PROPERTY_GIT_REPOSITORY_URL).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				gitUrlBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));		

		//reference
		Label gitReferenceLabel = new Label(parent, SWT.NONE);
		gitReferenceLabel.setText("Git Reference:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(false, false)
			.applyTo(gitReferenceLabel);
		gitReferenceLabel.setToolTipText("Optional branch, tag, or commit.");

		final Text gitReferenceText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.span(2, 1)
			.applyTo(gitReferenceText);
		ValueBindingBuilder
			.bind(WidgetProperties.text().observe(gitReferenceText))
			.to(BeanProperties.value(IBuildConfigPageModel.PROPERTY_GIT_REFERENCE).observe(model))
			.in(dbc);
		
		//context dir
		Label contextDirLabel = new Label(parent, SWT.NONE);
		contextDirLabel.setText("Context Directory:");
		contextDirLabel.setToolTipText("Optional subdirectory for the application source code, used as the context directory for the build.");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(false, false)
			.applyTo(contextDirLabel);
		
		final Text contextDirText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.span(2, 1)
			.applyTo(contextDirText);
		ValueBindingBuilder
			.bind(WidgetProperties.text().observe(contextDirText))
			.to(BeanProperties.value(IBuildConfigPageModel.PROPERTY_CONTEXT_DIR).observe(model))
			.in(dbc);
	}
	
}
