/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.application;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.restclient.model.template.IParameter;

/**
 * The template parameter page that allows viewing
 * and editing of a template's input parameters
 * 
 * @author jeff.cantrill
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
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);

		Group templateParametersGroup = new Group(container, SWT.NONE);
		templateParametersGroup.setText("Template Parameters");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(templateParametersGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(templateParametersGroup);
		Composite tableContainer = new Composite(templateParametersGroup, SWT.NONE);
		
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(viewer))
				.to(BeanProperties.value(ITemplateParametersPageModel.PROPERTY_SELECTED_PARAMETER).observe(model))
				.in(dbc);
		viewer.setContentProvider(new ObservableListContentProvider());
		viewer.setInput(BeanProperties.list(
				ITemplateParametersPageModel.PROPERTY_PARAMETERS).observe(model));
		
		Button editExistingButton = new Button(templateParametersGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(editExistingButton);
		editExistingButton.setText("Edit");
		editExistingButton.addSelectionListener(onEdit());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(editExistingButton))
				.notUpdatingParticipant()
				.to(BeanProperties.value(ITemplateParametersPageModel.PROPERTY_SELECTED_PARAMETER).observe(model))
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);
		
		Button resetButton = new Button(templateParametersGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(resetButton);
		resetButton.setText("Reset");
		resetButton.addSelectionListener(onReset());
	}
	
	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IParameter>() {
					@Override
					public String getValue(IParameter variable) {
						return variable.getName();
					}
				})
					.name("Name").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.column(new TemplateParameterColumnLabelProvider())
					.name("Value").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.column(new IColumnLabelProvider<IParameter>() {
						@Override
						public String getValue(IParameter variable) {
							return StringUtils.defaultIfEmpty("", variable.getDescription());
						}
					})
					.name("Description").align(SWT.LEFT).minWidth(100).buildColumn()
				.buildViewer();
		viewer.setComparator(new ViewerComparator() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				IParameter first = (IParameter) e1;
				IParameter other = (IParameter) e2;
				return first.getName().compareTo(other.getName());
			}
			
		});
		return viewer;
	}

	private SelectionListener onEdit() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final IParameter param = getSelectedParameter();
				InputDialog dialog = new InputDialog(getShell(), "Edit Template Parameter", NLS.bind("Enter a value for {0}.\n{1}", param.getName(), param.getDescription()), param.getValue(), null) ;
				if(InputDialog.OK == dialog.open()){
					model.updateParameterValue(param, dialog.getValue());
					viewer.refresh();
				}
			}
		};
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
