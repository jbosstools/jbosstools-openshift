/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.resource;

import java.io.File;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.SimpleUrlStringValidator;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.common.AbstractProjectPage;

import com.openshift.restclient.OpenShiftException;

/**
 * @author Jeff Maury
 *
 */
public class ResourcePayloadPage extends AbstractProjectPage<IResourcePayloadPageModel> {
    
    private static final IValidator URL_VALIDATOR =  new SimpleUrlStringValidator();

    public ResourcePayloadPage(IWizard wizard, IResourcePayloadPageModel model) {
        super(wizard, model,
              "Select resource payload",
              "Select the file or workspace file from where the resource will be created",
              "resourcePayload");
    }

    @Override
    protected void doCreateControls(Composite parent, DataBindingContext dbc) {
        super.doCreateControls(parent, dbc);
        createPayloadSourceControls(parent, dbc);
    }

    private void createPayloadSourceControls(Composite parent, DataBindingContext dbc) {
        Group sourceGroup = new Group(parent, SWT.NONE);
        sourceGroup.setText("Source");
        GridDataFactory.fillDefaults()
                .align(SWT.FILL, SWT.FILL)
                .grab(true, true)
                .span(3, 1)
                .hint(SWT.DEFAULT, SWT.DEFAULT)
                .applyTo(sourceGroup);
        GridLayoutFactory.fillDefaults()
                .numColumns(3)
                .margins(10, 6)
                .spacing(6, 6)
                .applyTo(sourceGroup);
        
        createLocalSourceControls(dbc, sourceGroup);
    }

    private void createLocalSourceControls(DataBindingContext dbc, Group sourceGroup) {
        // local template file name
        Text sourceText = new Text(sourceGroup, SWT.BORDER);
        GridDataFactory.fillDefaults()
                .align(SWT.FILL, SWT.CENTER).grab(true, false)
                .applyTo(sourceText);
        final IObservableValue source = WidgetProperties.text(SWT.Modify).observe(sourceText);
        Binding binding = ValueBindingBuilder
                .bind(source )
                //.validatingBeforeSet(value->isFile(value.toString())?
                //        ValidationStatus.ok(): 
                //            ValidationStatus.error(value +" is not a file"))
                .to(BeanProperties.value(
                        IResourcePayloadPageModel.PROPERTY_SOURCE).observe(model))
                .in(dbc);
        MultiValidator validator = new MultiValidator() {
            
            @Override
            protected IStatus validate() {
                String sourceValue = (String) source.getValue();
                return (boolean) !IResourcePayloadPageModel.URL_VALIDATOR.isValid(sourceValue) && !isFile(sourceValue)?ValidationStatus.error(sourceValue + " is not a file"):ValidationStatus.ok();
            }
        };
        dbc.addValidationStatusProvider(validator);

        // browse button
        Button btnBrowseFiles = new Button(sourceGroup, SWT.NONE);
        btnBrowseFiles.setText("File system...");
        GridDataFactory.fillDefaults()
                .align(SWT.LEFT, SWT.CENTER)
                .applyTo(btnBrowseFiles);
        UIUtils.setDefaultButtonWidth(btnBrowseFiles);

        btnBrowseFiles.addSelectionListener(onFileSystemBrowseClicked());
        
        // browse button
        Button btnBrowseWorkspaceFiles = new Button(sourceGroup, SWT.NONE);
        btnBrowseWorkspaceFiles.setText("Workspace...");
        GridDataFactory.fillDefaults()
                .align(SWT.LEFT, SWT.CENTER)
                .applyTo(btnBrowseWorkspaceFiles);
        UIUtils.setDefaultButtonWidth(btnBrowseWorkspaceFiles);

        btnBrowseWorkspaceFiles.addSelectionListener(onBrowseWorkspaceClicked());
    }

    private SelectionAdapter onFileSystemBrowseClicked() {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = createFileDialog(model.getSource());
                String file = dialog.open();
                setLocalSourceFileName(file);
            }

            private FileDialog createFileDialog(String selectedFile) {
                FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
                dialog.setText("Select an OpenShift resource");
                if(isFile(selectedFile)) {
                    File file = new File(selectedFile);
                    dialog.setFilterPath(file.getParentFile().getAbsolutePath());
                }
                return dialog;
            }
        };
    }
    
    private SelectionListener onBrowseWorkspaceClicked() {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ElementTreeSelectionDialog dialog = UIUtils.createFileDialog(model.getSource(),
                                                                             "Select an OpenShift resource",
                                                                             "Select an OpenShift resource (*.json)",
                                                                             "json",
                                                                             null);
                if (dialog.open() == IDialogConstants.OK_ID && dialog.getFirstResult() instanceof IFile) {
                    String path = ((IFile)dialog.getFirstResult()).getFullPath().toString();
                    String file = VariablesHelper.addWorkspacePrefix(path);
                    setLocalSourceFileName(file);
                }
            }


        };
    }
    
    private void setLocalSourceFileName(String file) {
        if (file == null || !isFile(file)) {
            return;
        }
        try {
            model.setSource(file);
        } catch (ClassCastException | OpenShiftException ex) {
            IStatus status = ValidationStatus.error(ex.getMessage(), ex);
            OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
            ErrorDialog.openError(getShell(), "Openshift resource error",
                    NLS.bind("The file \"{0}\" is not an OpenShift resource.", 
                            file),
                    status);
        }
    }
   

    @Override
    protected void setupWizardPageSupport(DataBindingContext dbc) {
        ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
    }


}
