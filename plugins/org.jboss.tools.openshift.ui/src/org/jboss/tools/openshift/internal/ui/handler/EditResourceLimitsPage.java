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

public class EditResourceLimitsPage extends AbstractOpenShiftWizardPage {
    public static final String[] MEMORY_SUFFIXES = {"M", "G", "Mi", "Gi", ""};
    public static final String[] MEMORY_SUFFIXES_LABELS = {"MB", "GB", "MiB", "GiB", "MiB"};
    public static final String[] CPU_SUFFIXES = {"m", "k", "M", "G", ""};
    public static final String[] CPU_SUFFIXES_LABELS = {"millicores", "kcores", "Mcores", "Gcores", "cores"};
  
    private EditResourceLimitsPageModel model;

    public EditResourceLimitsPage(EditResourceLimitsPageModel model, IWizard wizard) {
        super(OpenShiftUIMessages.EditResourceLimitsPageTitle,
              NLS.bind(OpenShiftUIMessages.EditResourceLimitsPageDescription, model.getUpdatedReplicationController().getName()),
              "EditResourceLimitsPage",
              wizard);
        this.model = model;
    }

    @Override
    protected void doCreateControls(Composite parent, DataBindingContext dbc) {
        GridLayoutFactory.fillDefaults().applyTo(parent);
        final Composite dialogArea = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(dialogArea);
        GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).spacing(20, SWT.DEFAULT).applyTo(dialogArea);

        Group group = new Group(dialogArea, SWT.NONE);
        group.setText(OpenShiftUIMessages.MemoryLabel);
        GridDataFactory.fillDefaults().span(3, 1).grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(group);
        GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).spacing(10, 4).applyTo(group);
        createResourceWidgets(dbc, group, OpenShiftUIMessages.RequestLabel, EditResourceLimitsPageModel.REQUESTS_MEMORY, MEMORY_SUFFIXES, MEMORY_SUFFIXES_LABELS);
        createResourceWidgets(dbc, group, OpenShiftUIMessages.LimitLabel, EditResourceLimitsPageModel.LIMITS_MEMORY, MEMORY_SUFFIXES, MEMORY_SUFFIXES_LABELS);
        
        group = new Group(dialogArea, SWT.NONE);
        group.setText(OpenShiftUIMessages.CPULabel);
        GridDataFactory.fillDefaults().span(3, 1).grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(group);
        GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).spacing(10, 4).applyTo(group);
        createResourceWidgets(dbc, group, OpenShiftUIMessages.RequestLabel, EditResourceLimitsPageModel.REQUESTS_CPU, CPU_SUFFIXES, CPU_SUFFIXES_LABELS);
        createResourceWidgets(dbc, group, OpenShiftUIMessages.LimitLabel, EditResourceLimitsPageModel.LIMITS_CPU, CPU_SUFFIXES, CPU_SUFFIXES_LABELS);
    }
    
    private void createResourceWidgets(DataBindingContext dbc, Group group, String label, String property, String[] suffixes, String[] labels) {
        // label
        Label labelComp = new Label(group, SWT.NONE);
        labelComp.setText(label);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(labelComp);

        // scale spinner
        Text text = new Text(group, SWT.BORDER);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(text);
        ComboViewer combo = new ComboViewer(group);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setInput(suffixes);
        combo.setLabelProvider(new LabelProvider() {

            @Override
            public String getText(Object element) {
                for(int i=0; i < suffixes.length;++i) {
                    if (element.equals(suffixes[i])) {
                        element = labels[i];
                        break;
                    }
                }
                return (String) element;
            }
            
        });

        IObservableValue textObservable = WidgetProperties.text(SWT.Modify).observe(text);
        IObservableValue comboObservable = ViewerProperties.singleSelection().observe(combo);
        IObservableValue master = BeanProperties.value(EditResourceLimitsPageModel.SELECTED_CONTAINER).observe(model);
        ValueBindingBuilder.bind(textObservable)
                .validatingAfterGet(new NumericValidator("integer", Integer::parseInt))
                .converting(new AggregatingConverter(comboObservable, true))
                .to(PojoProperties.value(property).observeDetail(master))
                .converting(new KeywordConverter(suffixes, true))
                .in(dbc);
        ValueBindingBuilder.bind(comboObservable)
        .converting(new AggregatingConverter(textObservable, false))
        .to(PojoProperties.value(property).observeDetail(master))
        .converting(new KeywordConverter(suffixes, false))
        .in(dbc);
    }
}