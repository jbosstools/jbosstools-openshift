/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.AggregatingConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.KeywordConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.NumericValidator;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.wizard.common.EditResourceLimitsPageModel;

import com.openshift.restclient.model.IContainer;

public class EditResourceLimitsPage extends AbstractOpenShiftWizardPage {
	public static final String[] MEMORY_SUFFIXES = 
		{ "M", "G", "Mi", "Gi", "" };
	public static final String[] MEMORY_SUFFIXES_LABELS = 
		{ "MB (1000 KB)", "GB (1000 MB)", "MiB (1024 KB)", "GiB (1024 MB)", "bytes" };
	public static final String[] CPU_SUFFIXES = 
		{ "m", "k", "M", "G", "" };
	public static final String[] CPU_SUFFIXES_LABELS = 
		{ "millicores", "kcores", "Mcores", "Gcores", "cores" };

	private EditResourceLimitsPageModel model;

	public EditResourceLimitsPage(EditResourceLimitsPageModel model, IWizard wizard) {
		super(OpenShiftUIMessages.EditResourceLimitsPageTitle,
				NLS.bind(OpenShiftUIMessages.EditResourceLimitsPageDescription,
						model.getUpdatedReplicationController().getName()),
				"EditResourceLimitsPage", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		final Composite dialogArea = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(dialogArea);
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 10).spacing(20, SWT.DEFAULT)
			.applyTo(dialogArea);

		Group group = new Group(dialogArea, SWT.NONE);
		group.setText(OpenShiftUIMessages.MemoryLabel);
		GridDataFactory.fillDefaults().span(3, 1).grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(group);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).spacing(10, 4).applyTo(group);
		createResourceWidgets(OpenShiftUIMessages.RequestLabel, EditResourceLimitsPageModel.REQUESTS_MEMORY, 
				MEMORY_SUFFIXES, MEMORY_SUFFIXES_LABELS, group, dbc);
		createResourceWidgets(OpenShiftUIMessages.LimitLabel, EditResourceLimitsPageModel.LIMITS_MEMORY,
				MEMORY_SUFFIXES, MEMORY_SUFFIXES_LABELS, group, dbc);

		group = new Group(dialogArea, SWT.NONE);
		group.setText(OpenShiftUIMessages.CPULabel);
		GridDataFactory.fillDefaults().span(3, 1).grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(group);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).spacing(10, 4).applyTo(group);
		createResourceWidgets(OpenShiftUIMessages.RequestLabel, EditResourceLimitsPageModel.REQUESTS_CPU,
				CPU_SUFFIXES, CPU_SUFFIXES_LABELS, group, dbc);
		createResourceWidgets(OpenShiftUIMessages.LimitLabel, EditResourceLimitsPageModel.LIMITS_CPU,
				CPU_SUFFIXES, CPU_SUFFIXES_LABELS, group, dbc);
	}

	private void createResourceWidgets(String label, String property, String[] suffixes, String[] labels, 
			Group parent, DataBindingContext dbc) {
		// label
		Label labelComp = new Label(parent, SWT.NONE);
		labelComp.setText(label);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(labelComp);

		// value text
		Text text = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(text);

		// unit combo
		ComboViewer combo = new ComboViewer(parent);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).hint(140, SWT.DEFAULT)
			.applyTo(combo.getControl());
		combo.setContentProvider(ArrayContentProvider.getInstance());
		combo.setInput(suffixes);
		combo.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				return getLabelForSuffix(element, suffixes, labels);
			}

			private String getLabelForSuffix(Object element, String[] suffixes, String[] labels) {
				String label = (String) element;
				for (int i = 0; i < suffixes.length; ++i) {
					if (element.equals(suffixes[i])) {
						label = labels[i];
						break;
					}
				}
				return label;
			}
		});

		IObservableValue<String> valueObservable = WidgetProperties.text(SWT.Modify).observe(text);
		IObservableValue<String> selectedUnitObservable = ViewerProperties.singleSelection().observe(combo);
		IObservableValue<IContainer> master = BeanProperties.value(EditResourceLimitsPageModel.SELECTED_CONTAINER).observe(model);
		ValueBindingBuilder
			.bind(valueObservable)
			.validatingAfterGet(new NumericValidator("integer", Integer::parseInt))
			.converting(new AggregatingConverter(selectedUnitObservable, true))
			.to(PojoProperties.value(property).observeDetail(master))
			.converting(new KeywordConverter(suffixes, true))
			.in(dbc);
		ValueBindingBuilder
			.bind(selectedUnitObservable)
			.converting(new AggregatingConverter(valueObservable, false))
			.to(PojoProperties.value(property).observeDetail(master))
			.converting(new KeywordConverter(suffixes, false))
			.in(dbc);
	}
}