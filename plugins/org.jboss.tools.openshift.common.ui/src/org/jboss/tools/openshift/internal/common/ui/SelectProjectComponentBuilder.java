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
package org.jboss.tools.openshift.internal.common.ui;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;

public class SelectProjectComponentBuilder {
	String textLabel = "Use existing workspace project:";
	String browseLabel = "Browse...";
	String errorText = "Select an existing project";
	int hSpan = 1;

	IObservableValue eclipseProjectObservable;
	SelectionListener selectionListener;

	ISWTObservableValue projectNameTextObservable;

	public SelectProjectComponentBuilder() {}

	public void build(Composite container, DataBindingContext dbc) {
		Label existingProjectLabel = new Label(container, SWT.NONE);
		existingProjectLabel.setText("Eclipse Project: ");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(existingProjectLabel);
		
		final Text existingProjectNameText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(existingProjectNameText);
		
		projectNameTextObservable = WidgetProperties.text(SWT.Modify).observe(existingProjectNameText);

		Binding eclipseProjectBinding = ValueBindingBuilder
			.bind(projectNameTextObservable)
			.validatingAfterConvert(new IValidator() {
				@Override
				public IStatus validate(Object value) {
					if(value instanceof String) {
						return ValidationStatus.ok();
					} else if(value == null) {
						return ValidationStatus.error("Select an existing project");
					}
					return ValidationStatus.ok();
				}
			})
			.converting(new Converter(String.class, IProject.class) {
				@Override
				public Object convert(Object fromObject) {
					String name = (String)fromObject;
					return ProjectUtils.getProject(name);
				}
			})
			.to(eclipseProjectObservable)
			.converting(new Converter(IProject.class, String.class) {

				@Override
				public Object convert(Object fromObject) {
					return fromObject == null ? "" : ((IProject)fromObject).getName();
				}
			})
			.in(dbc);
		ControlDecorationSupport.create(
				eclipseProjectBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		// project name content assist
		ControlDecoration dec = new ControlDecoration(existingProjectNameText, SWT.TOP | SWT.RIGHT);

		FieldDecoration contentProposalFieldIndicator =
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(contentProposalFieldIndicator.getImage());
		dec.setDescriptionText("Auto-completion is enabled when you start typing a project name.");
		dec.setShowOnlyOnFocus(true);

		new AutoCompleteField(existingProjectNameText, new TextContentAdapter(), ProjectUtils.getAllAccessibleProjectNames());

		// browse projects
		Button browseProjectsButton = new Button(container, SWT.NONE);
		browseProjectsButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER)
				.hint(100, SWT.DEFAULT)
				.grab(false, false)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(selectionListener);
	}

	/**
	 * Set value of label at text input. Has only effect if called before build().
	 *  
	 * @param label
	 * @return
	 */
	public SelectProjectComponentBuilder setTextLabel(String label) {
		textLabel = label;
		return this;
	}

	/**
	 * Set value of model observable. Has only effect if called before build().
	 *  
	 * @param label
	 * @return
	 */
	public SelectProjectComponentBuilder setEclipseProjectObservable(IObservableValue eclipseProjectObservable) {
		this.eclipseProjectObservable = eclipseProjectObservable;
		return this;
	}

	/**
	 * Set number of columns in the grid to take by text input. 
	 * Grid needs one column for label, hSpan columns for text input, and one column for browse button.
	 * Has only effect if called before build().
	 *  
	 * @param label
	 * @return
	 */
	public SelectProjectComponentBuilder setHorisontalSpan(int hSpan) {
		this.hSpan = hSpan;
		return this;
	}

	/**
	 * Set selection listener that will run a customized dialog and consume selected project.
	 * Has only effect if called before build().
	 *  
	 * @param label
	 * @return
	 */
	public SelectProjectComponentBuilder setSelectionListener(SelectionListener listener) {
		selectionListener = listener;
		return this;
	}

	/**
	 * Returns swt observable for text input. Has only effect if called after build().
	 *  
	 * @param label
	 * @return
	 */
	public ISWTObservableValue getProjectNameTextObservable() {
		return projectNameTextObservable;
	}
	
}
