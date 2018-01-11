/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;


import static org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIConstants.IMPORT_APPLICATION_DIALOG_SETTINGS_KEY;
import static org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIConstants.REPO_PATH_KEY;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.GitCloningWizardPage;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizardModel;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;

public class ImportApplicationDetailsPage extends GitCloningWizardPage {
    
    private NewApplicationWizardModel applicationSourceModel;
    private ImportApplicationWizardModel importApplicationWizardModel;
    private BuildConfigSelectedValidator buildConfigSelectedValidator;
    
    private ResourceDetailsLabelProvider buildConfigTreeLabelProvider;
    
    public static final String PAGE_NAME = "Import application details";

    protected ImportApplicationDetailsPage(IWizard wizard, NewApplicationWizardModel applicationSourceModel) {
        super("Import application", "Configure the project settings", PAGE_NAME, wizard, new ImportApplicationWizardModel());
        this.applicationSourceModel = applicationSourceModel;
        this.importApplicationWizardModel = (ImportApplicationWizardModel)model;
        
        this.importApplicationWizardModel.setConnection(applicationSourceModel.getConnection());
        String repoPath = loadRepoPath();
        if (StringUtils.isNotBlank(repoPath)) {
            importApplicationWizardModel.setCloneDestination(repoPath);
            importApplicationWizardModel.setUseDefaultCloneDestination(false);
        }
    }
    
    private String loadRepoPath() {
        if (getDialogSettings() == null || getDialogSettings().get(REPO_PATH_KEY) == null) {
            IDialogSettings settings = DialogSettings.getOrCreateSection(
                    OpenShiftUIActivator.getDefault().getDialogSettings(), IMPORT_APPLICATION_DIALOG_SETTINGS_KEY);
            return settings.get(REPO_PATH_KEY);
        }
        return getDialogSettings().get(REPO_PATH_KEY);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doCreateControls(Composite parent, DataBindingContext dbc) {
        GridLayoutFactory.fillDefaults().applyTo(parent);
        Button isImportApplicationButton = new Button(parent, SWT.CHECK);
        isImportApplicationButton.setText("Import application into the workspace");
        isImportApplicationButton.setToolTipText("Uncheck if you don't want to import application into the workspace");
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(isImportApplicationButton);
        
        Composite gitCloneComposite = new Composite(parent, SWT.NONE);
        TreeViewer buildConfigsTreeViewer = createTable(gitCloneComposite, dbc);

        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(gitCloneComposite);
        super.doCreateControls(gitCloneComposite, dbc);
        
        isImportApplicationButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                UIUtils.enableAllChildren(((Button)event.getSource()).getSelection(), gitCloneComposite);
            }
            
        });
        
        //set buildConfigsTreeViewer content based on selected template
        IObservableValue<IApplicationSource> selectedAppSourceObservable = 
                BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_SELECTED_APP_SOURCE).observe(applicationSourceModel);
        selectedAppSourceObservable.addValueChangeListener(new IValueChangeListener<IApplicationSource>() {

            @Override
            public void handleValueChange(ValueChangeEvent<? extends IApplicationSource> event) {
                IApplicationSource appSource = event.getObservableValue().getValue();
                Collection<IResource> buildConfigs = getBuildConfigsFromApplicationSource(appSource);
                buildConfigsTreeViewer.setInput(buildConfigs);
                UIUtils.enableAllChildren(!buildConfigs.isEmpty(), parent);
                isImportApplicationButton.setSelection(!buildConfigs.isEmpty());
                if (buildConfigs.isEmpty()) {
                    setMessage("No build configs were found in your template, so you can't import application right now.", IMessageProvider.WARNING);
                }
                
                Map<String, IParameter> templateParameters = ((ITemplate)appSource.getSource()).getParameters();
                importApplicationWizardModel.setTemplateParameters(templateParameters);
                
                buildConfigTreeLabelProvider = new ResourceDetailsLabelProvider(templateParameters);
                buildConfigsTreeViewer.setLabelProvider(buildConfigTreeLabelProvider);
            }

        });
    }
    
    private Collection<IResource> getBuildConfigsFromApplicationSource(IApplicationSource appSource) {
        Collection<IResource> buildConfigs = Collections.emptyList();
        if (appSource != null && appSource.getSource() instanceof ITemplate) {
            ITemplate template = appSource.getSource();
            buildConfigs = template.getObjects().stream()
                    .filter(resource -> ResourceKind.BUILD_CONFIG.equals(resource.getKind())).collect(Collectors.toList());
        }
        return buildConfigs;
    }
    
    private TreeViewer createTable(Composite parent, DataBindingContext dbc) {
        Composite tableContainer = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.FILL)
            .grab(true, false)
            .hint(400, 150).applyTo(tableContainer);

        TreeColumnLayout treeLayout = new TreeColumnLayout();
        tableContainer.setLayout(treeLayout);
        TreeViewer viewer = new TreeViewer(tableContainer, SWT.BORDER  | SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.setContentProvider(new ResourceDetailsContentProvider());
        
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getStructuredSelection() != null 
                        && ((ITreeSelection)event.getStructuredSelection()).getPaths() != null
                        && ((ITreeSelection)event.getStructuredSelection()).getPaths().length > 0) {
                    importApplicationWizardModel.setSelectedItem(
                            ((ITreeSelection)event.getStructuredSelection()).getPaths()[0].getFirstSegment());
                }
                buildConfigSelectedValidator.forceRevalidate();
            }
        });
        
        dbc.addValidationStatusProvider(this.buildConfigSelectedValidator = new BuildConfigSelectedValidator());
        
        return viewer;
    }
    
    private class BuildConfigSelectedValidator extends MultiValidator {

        @Override
        protected IStatus validate() {
            if (importApplicationWizardModel.getSelectedItem() == null) {
                return ValidationStatus.error("Please, select a build config");
            }
            return ValidationStatus.ok();
        }
        
        public void forceRevalidate() {
            revalidate();
        }
        
    }

}
