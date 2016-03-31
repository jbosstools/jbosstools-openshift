/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.Iterator;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.map.ObservableMap;
import org.eclipse.core.databinding.observable.map.WritableMap;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerCellDecorationManager;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.TemplateParameterViewerUtils.ParameterNameViewerComparator;

import com.openshift.restclient.model.template.IParameter;

/**
 * The template parameter page that allows viewing
 * and editing of a template's input parameters
 * 
 * @author jeff.cantrill
 * @author Andre Dietisheim
 *
 */
public class TemplateParametersPage extends AbstractOpenShiftWizardPage {

	private ITemplateParametersPageModel model;
	private TableViewer viewer;

	public TemplateParametersPage(IWizard wizard, ITemplateParametersPageModel model) {
		super("Template Parameters", 
				"Edit the parameter values to be substituted into the template.", 
				"Template Parameter Page", 
				wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(10, 10)
			.applyTo(container);
		
		// parameters table
		Composite tableContainer = new Composite(container, SWT.NONE);
		IObservableList<IParameter> parametersObservable = 
				BeanProperties.list(ITemplateParametersPageModel.PROPERTY_PARAMETERS).observe(model);
		this.viewer = createTable(tableContainer, parametersObservable, dbc);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).hint(500, 300).applyTo(tableContainer);
		IObservableValue<IParameter> selectedParameter = 
				BeanProperties.value(ITemplateParametersPageModel.PROPERTY_SELECTED_PARAMETER).observe(model);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(viewer))
				.to(selectedParameter)
				.in(dbc);
		viewer.setInput(parametersObservable);
		viewer.addDoubleClickListener(onDoubleClick());

		// edit button
		Button editExistingButton = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(editExistingButton);
		editExistingButton.setText("Edit...");
		UIUtils.setDefaultButtonWidth(editExistingButton);
		editExistingButton.addSelectionListener(onEdit());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(editExistingButton))
				.notUpdatingParticipant()
				.to(selectedParameter)
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);
		
		// reset button
		Button resetButton = new Button(container, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(resetButton);
		resetButton.setText("Reset");
		UIUtils.setDefaultButtonWidth(resetButton);
		resetButton.addSelectionListener(onReset());

		// required explanation
		Label requiredExplanationLabel = new Label(container, SWT.None);
		requiredExplanationLabel.setText("* = value required, click the 'Edit...' button or double-click on a value to edit it.");
		GridDataFactory.fillDefaults()
			.grab(true, false).align(SWT.FILL, SWT.FILL).span(2,1).applyTo(requiredExplanationLabel);

		// selected parameter details
		final Group detailsContainer = new Group(container, SWT.NONE);
		detailsContainer.setText("Details");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).span(2,1).hint(SWT.DEFAULT, 106)
				.applyTo(detailsContainer);
		new TemplateParameterDetailViews(selectedParameter, detailsContainer, dbc)
				.createControls();

	}

	private IListChangeListener<IParameter> onParametersChanged(final TableViewerCellDecorationManager cellDecorations, final ObservableMap<String, IStatus> validationStatusByParameter) {
		return new IListChangeListener<IParameter>() {

			@Override
			public void handleListChange(ListChangeEvent<? extends IParameter> event) {
				// new list of parameters, clear current validation status
				validationStatusByParameter.clear();
				cellDecorations.hideAll();
			}
		};
	}

	private IDoubleClickListener onDoubleClick() {
		return new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				IParameter param = (IParameter) selection.getFirstElement();
				openEditDialog(param);
			}
		};
	}
	
	public TableViewer createTable(final Composite tableContainer, IObservableList<IParameter> parametersObservable, DataBindingContext dbc) {
		final Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		Image decorationImage = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
		final ObservableMap<String, IStatus> cellsValidationStatusObservable = new WritableMap<String, IStatus>(String.class, IStatus.class);
		final TableViewerCellDecorationManager decorations = new TableViewerCellDecorationManager(decorationImage, table);
		parametersObservable.addListChangeListener(onParametersChanged(decorations, cellsValidationStatusObservable));
		TableViewer viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new CellLabelProvider() {
							
							@Override
							public void update(ViewerCell cell) {
								Assert.isLegal(cell.getElement() instanceof IParameter, "cell element is not a IParameter");

								IParameter parameter = (IParameter) cell.getElement();
								String label = parameter.getName();
								if (parameter.isRequired()) {
									label = markRequired(label);
								}
								cell.setText(label);
							}

							private String markRequired(String label) {
								return label += " *";
							}

							@Override
							public String getToolTipText(Object object) {
								Assert.isLegal(object instanceof IParameter, "cell element is not a IParameter");

								return ((IParameter) object).getDescription();
							}

							@Override
							public int getToolTipDisplayDelayTime(Object object) {
								return 0;
							}
					})
					.name("Name")
					.align(SWT.LEFT)
					.weight(1)
					.minWidth(180)
					.buildColumn()
				.column(new CellLabelProvider() {

							@Override
							public void update(ViewerCell cell) {
								Assert.isLegal(cell.getElement() instanceof IParameter, "cell element is not a IParameter");

								final IParameter parameter = (IParameter) cell.getElement();
								String label = TemplateParameterViewerUtils.getValueLabel(parameter);
								cell.setText(label);

								IStatus validationStatus = validate(parameter);
								cellsValidationStatusObservable.put(parameter.getName(), validationStatus);
								decorations.toggle(!validationStatus.isOK(), cell);
							}

							private IStatus validate(IParameter parameter) {
								if (parameter.isRequired()) {
									if (StringUtils.isEmpty(parameter.getValue())
													&& StringUtils.isEmpty(parameter.getGeneratorName())) {
										return ValidationStatus.error(
												NLS.bind("Parameter {0} is required, please provide a value.", parameter.getName()));
									};
								} 
								IStatus status = gitSourceValidator.validate(parameter);
								if(!status.isOK()) {
									return status;
								}
								return ValidationStatus.ok();
							}

							@Override
							public String getToolTipText(Object object) {
								Assert.isLegal(object instanceof IParameter, "cell element is not a IParameter");

								return ((IParameter) object).getDescription();
							}

							@Override
							public int getToolTipDisplayDelayTime(Object object) {
								return 0;
							}

						})
						.name("Value")
						.align(SWT.LEFT)
						.weight(1)
						.buildColumn()
			.buildViewer();

		viewer.setComparator(new ParameterNameViewerComparator());
		viewer.setContentProvider(new ObservableListContentProvider());

		// cells validity
		dbc.addValidationStatusProvider(new MultiValidator() {
			
			@Override
			protected IStatus validate() {
				for (Iterator<IStatus> iterator = 
						(Iterator<IStatus>) cellsValidationStatusObservable.values().iterator(); iterator.hasNext(); ) {
					IStatus cellValidationStatus = iterator.next();
					if (cellValidationStatus != null
							&& !cellValidationStatus.isOK()) {
						return cellValidationStatus;
					}
				}
				return ValidationStatus.ok();
			}
		});
		return viewer;
	}

	private static boolean isGitSourceParameterName(String name) {
		return ITemplateParametersPageModel.PARAMETER_SOURCE_REPOSITORY_URL.equals(name)
				|| ITemplateParametersPageModel.PARAMETER_GIT_URI.equals(name);
	}

	GitSourceValidator gitSourceValidator = new GitSourceValidator();

	/**
	 * Validates only non-empty value and if it is not valid, then returns 
	 * error status for a required parameter and warning status for an optional parameter.
	 */
	class GitSourceValidator implements IValidator {
		private IParameter parameter;

		public GitSourceValidator() {
		}
	
		public void setParameter(IParameter parameter) {
			this.parameter = parameter;
		}

		@Override
		public IStatus validate(Object value) {
			IParameter parameter = null;
			String paramaterValue = null;
			if(value instanceof IParameter) {
				//The case in cell editor
				parameter = (IParameter)value;
				paramaterValue = parameter.getValue();
			} else if(value instanceof String) {
				//The case in edit dialog
				parameter = this.parameter;
				paramaterValue = (String)value;
			}
			if(parameter != null && !StringUtils.isEmpty(paramaterValue)) {
				String message = validateGitSource(parameter.getName(), paramaterValue);
				if(message != null) {
					if(parameter.isRequired()) {
						return ValidationStatus.error(message);
					} else {
						//Normally, git source url parameter should be marked as required,
						//but if by some reason it is not, let's return a warning.
						return ValidationStatus.warning(message);
					}
				}
			}
			return ValidationStatus.ok();
		}
		
		private String validateGitSource(String name, String value) {
			if(StringUtils.isEmpty(value)) {
				return null;
			}
			if(isGitSourceParameterName(name)) {
				if(!UrlUtils.isValid(value)) {
					return NLS.bind("Parameter {0} is not a valid URL.", name);
				}
				if(!value.startsWith("http://")
						&& !value.startsWith("https://")) {
					return NLS.bind("Parameter {0} should be an http(s) connection.", name);
				}
			}
			return null;
		}

	}
	
	private SelectionListener onEdit() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openEditDialog(getSelectedParameter());
			}
		};
	}

	private void openEditDialog(final IParameter parameter) {
		EditValueDialog dialog = new EditValueDialog(getShell(), 
				"Edit Template Parameter",
				NLS.bind("Please enter a value for {0}.\n{1}", parameter.getName(), parameter.getDescription()), 
				parameter.getName(), 
				parameter.getValue(), 
				parameter.isRequired());
		if(isGitSourceParameterName(parameter.getName())) {
			GitSourceValidator valueValidator = new GitSourceValidator();
			valueValidator.setParameter(parameter);
			dialog.setValueValidator(valueValidator);
		}
		if (InputDialog.OK == dialog.open()) {
			model.updateParameterValue(parameter, dialog.getValue());
			viewer.refresh();
		}
	}

	private SelectionListener onReset() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.resetParameter(getSelectedParameter());
				viewer.refresh();
			}
		};
	}
	
	private IParameter getSelectedParameter() {
		return (IParameter) viewer.getStructuredSelection().getFirstElement();
	}

}
