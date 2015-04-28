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
package org.jboss.tools.openshift.internal.ui.wizard.application;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueWizardModel;
import org.jboss.tools.openshift.internal.common.ui.wizard.KeyValueWizard;
import org.jboss.tools.openshift.internal.common.ui.wizard.KeyValueWizardModelBuilder;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.ui.validator.LabelKeyValidator;
import org.jboss.tools.openshift.internal.ui.validator.LabelValueValidator;
import org.jboss.tools.openshift.internal.ui.wizard.application.IResourceLabelsPageModel.Label;

/**
 * Page to edit the labels for a resource.  Labels with certain keys can be identified
 * as readonly where it is not possible to delete or modify them
 * 
 * @author jeff.cantrill
 */
public class ResourceLabelsPage extends AbstractOpenShiftWizardPage {

	private static final String LABEL = "Label";
	private static final String RESOURCE_LABEL = "Resource Label";
	private static final String PAGE_DESCRIPTION = "Add or edit the labels to be added to each resource. " + 
			"Labels are used to organize, group, or select objects and resources, such as pods and services.  Some labels cannot be modified and therefore" +
			" cannot be edited or removed.";

	private IResourceLabelsPageModel model;
	private TableViewer viewer;

	protected ResourceLabelsPage(IWizard wizard, IResourceLabelsPageModel model) {
		super("Resource Labels", PAGE_DESCRIPTION, "Resource Labels Page", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);

		Group labelsGroup = new Group(container, SWT.NONE);
		labelsGroup.setText("Labels");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(labelsGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(labelsGroup);
		Composite tableContainer = new Composite(labelsGroup, SWT.NONE);

		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(viewer))
				.to(BeanProperties.value(IResourceLabelsPageModel.PROPERTY_SELECTED_LABEL).observe(model))
				.in(dbc);
		viewer.setContentProvider(new ObservableListContentProvider());
		viewer.setInput(BeanProperties.list(
				IResourceLabelsPageModel.PROPERTY_LABELS).observe(model));

		Button addButton = new Button(labelsGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addButton);
		addButton.setText("Add");
		addButton.addSelectionListener(onAdd());
		
		Button editExistingButton = new Button(labelsGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(editExistingButton);
		editExistingButton.setText("Edit");
		editExistingButton.addSelectionListener(onEdit());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(editExistingButton))
				.notUpdatingParticipant()
				.to(BeanProperties.value(IResourceLabelsPageModel.PROPERTY_SELECTED_LABEL).observe(model))
				.converting(new IsNotNullOrReadOnlyBooleanConverter())
				.in(dbc);
		
		Button removeButton = new Button(labelsGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove");
		removeButton.addSelectionListener(onRemove());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(removeButton))
				.notUpdatingParticipant()
				.to(BeanProperties.value(IResourceLabelsPageModel.PROPERTY_SELECTED_LABEL).observe(model))
				.converting(new IsNotNullOrReadOnlyBooleanConverter())
				.in(dbc);
	}

	private SelectionListener onRemove() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Label label = UIUtils.getFirstElement(viewer.getSelection(),Label.class);
				if(MessageDialog.openQuestion(getShell(), "Remove Label", NLS.bind("Are you sure you want to delete the label {0} ", label.getName()))) {
					model.removeLabel(label);
					viewer.refresh();
				}
			}
			
		};
	}

	private SelectionListener onEdit() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final Label label = UIUtils.getFirstElement(viewer.getSelection(),Label.class);
				IKeyValueWizardModel<Label> dialogModel = new KeyValueWizardModelBuilder<Label>(label)
						.windowTitle(RESOURCE_LABEL)
						.title("Edit Label")
						.description("Edit the resource label.")
						.keyLabel(LABEL)
						.groupLabel(LABEL)
						.keyAfterConvertValidator(new LabelKeyValidator())
						.valueAfterConvertValidator(new LabelValueValidator())
						.build();
				OkCancelButtonWizardDialog dialog =
						new OkCancelButtonWizardDialog(getShell(),
								new KeyValueWizard<Label>(label, dialogModel));
				if(OkCancelButtonWizardDialog.OK == dialog.open()) {
					model.updateLabel(label, dialogModel.getKey(), dialogModel.getValue());
				}
			}
		};
	}

	private SelectionListener onAdd() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IKeyValueWizardModel<Label> dialogModel = new KeyValueWizardModelBuilder<Label>()
						.windowTitle(RESOURCE_LABEL)
						.title("Add Label")
						.description("Add a resource label.")
						.keyLabel(LABEL)
						.groupLabel(LABEL)
						.keyAfterConvertValidator(new LabelKeyValidator())
						.valueAfterConvertValidator(new LabelValueValidator())
						.build();
				OkCancelButtonWizardDialog dialog =
						new OkCancelButtonWizardDialog(getShell(),
								new KeyValueWizard<Label>(UIUtils.getFirstElement(viewer.getSelection(),Label.class), dialogModel));
				if(OkCancelButtonWizardDialog.OK == dialog.open()) {
					model.addLabel(dialogModel.getKey(), dialogModel.getValue());
				}
			}
		};
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<Label>() {
					@Override
					public String getValue(Label label) {
						return label.getName();
					}
				})
				.name("Name").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.column(new IColumnLabelProvider<Label>() {

					@Override
					public String getValue(Label label) {
						return label.getValue();
					}
				
				})
				.name("Value").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.buildViewer();
		viewer.setComparator(new ViewerComparator() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				Label first = (Label) e1;
				Label other = (Label) e2;
				return first.getName().compareTo(other.getName());
			}
			
		});
		return viewer;
	}
	
	/*
	 * Only allow editing or removal of labels that are not
	 * deamed readonly.  Following the lead of the webconsole here
	 */
	private class IsNotNullOrReadOnlyBooleanConverter extends Converter{

		public IsNotNullOrReadOnlyBooleanConverter() {
			super(Label.class, Boolean.class);
		}

		@Override
		public Object convert(Object arg) {
			if(arg == null) return Boolean.FALSE;
			return !model.getReadOnlyLabels().contains(((Label) arg).getName());
		}
		
	}
}
