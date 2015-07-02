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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;

import com.openshift.restclient.ResourceFactoryException;
import com.openshift.restclient.UnsupportedVersionException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.ICapability;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.template.IParameter;
import com.openshift.restclient.model.template.ITemplate;

/**
 * A page that offers a list templates to a user
 * @author jeff.cantrill
 *
 */
public class TemplateListPage  extends AbstractOpenShiftWizardPage  {

	public ITemplateListPageModel model;
	private Text txtUploadedFileName;
	
	public TemplateListPage(IWizard wizard, ITemplateListPageModel model) {
		super("Select template", "Templates choices may be reduced to a smaller list by typing the name of a tag in the text field.", "templateList", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.numColumns(2)
			.margins(10, 6)
			.spacing(2, 2)
			.applyTo(parent);
		
		// upload template
		SelectObservableValue uploadTemplate = new SelectObservableValue();
		ValueBindingBuilder
			.bind(uploadTemplate)
			.to(BeanProperties.value(
					ITemplateListPageModel.PROPERTY_USE_UPLOAD_TEMPLATE).observe(model))
			.in(dbc);
		
		createUploadControls(parent, uploadTemplate, dbc);
		
		createServerTemplateControls(parent, uploadTemplate, dbc);
		model.setUseUploadTemplate(false);
	}

	private void createUploadControls(Composite parent, SelectObservableValue uploadTemplate, DataBindingContext dbc) {
		// upload  app radio
		Button btnUploadTemplate = new Button(parent, SWT.RADIO);
		btnUploadTemplate.setText("Use a template from my local file system:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.span(2, 1)
				.grab(true, false)
				.applyTo(btnUploadTemplate);

		uploadTemplate.addOption(Boolean.TRUE, WidgetProperties.selection().observe(btnUploadTemplate));

		// uploaded file name
		this.txtUploadedFileName = new Text(parent, SWT.BORDER);
		txtUploadedFileName.setEnabled(false);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(txtUploadedFileName);
		IObservableValue uploadedFilenameTextObservable = WidgetProperties.text(SWT.Modify).observe(txtUploadedFileName);
		ValueBindingBuilder
				.bind(uploadedFilenameTextObservable)
				.to(BeanProperties.value(
						ITemplateListPageModel.PROPERTY_TEMPLATE_FILENAME).observe(model))
				.in(dbc);

		// browse button
		Button btnBrowseFiles = new Button(parent, SWT.NONE);
		btnBrowseFiles.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).grab(false, false)
				.applyTo(btnBrowseFiles);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(btnBrowseFiles))
				.notUpdatingParticipant()
				.to(uploadTemplate)
				.in(dbc);
		btnBrowseFiles.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				if(StringUtils.isNotBlank(model.getTemplateFileName())) {
					File file = new File(model.getTemplateFileName());
					dialog.setFilterPath(file.getParentFile().getAbsolutePath());
				}
				String file = null;
				do {
					file = dialog.open();
					if (file != null) {
						try {
							model.setTemplateFileName(file);
							return;
						} catch (ClassCastException ex) {
							IStatus status = ValidationStatus.error(ex.getMessage(), ex);
							OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
							ErrorDialog.openError(getShell(), "Template Error",
									NLS.bind("The file \"{0}\" is not an OpenShift template.", 
											file),
									status);
						} catch (UnsupportedVersionException ex) {
							IStatus status = ValidationStatus.error(ex.getMessage(), ex);
							OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
							ErrorDialog.openError(getShell(), "Template Error", 
									NLS.bind("The file \"{0}\" is a template in a version that we do not support.", 
											file),
									status);
						} catch (ResourceFactoryException ex) {
							IStatus status = ValidationStatus.error(ex.getMessage(), ex);
							OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
							ErrorDialog.openError(getShell(), "Template Error",
									NLS.bind("Unable to read and/or parse the file \"{0}\" as a template.",
											file),
									status);
						}
					}
				} while (file != null);
			}
		});
	}
	
	private void createServerTemplateControls(Composite parent, SelectObservableValue uploadTemplate, DataBindingContext dbc) {
		// existing app radio
		Button btnServerTemplate = new Button(parent, SWT.RADIO);
		btnServerTemplate.setText("Use a template from the server:");
		GridDataFactory.fillDefaults()
				.span(2, 1)
				.indent(0,8)
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(btnServerTemplate);

		uploadTemplate.addOption(Boolean.FALSE, WidgetProperties.selection().observe(btnServerTemplate));
		
		Composite treeComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1)
				.align(SWT.FILL, SWT.FILL)
				.grab(true, true)
				.applyTo(treeComposite);
		GridLayoutFactory.fillDefaults().spacing(2, 2).applyTo(treeComposite);

		// filter text
		Text txtTemplateFilter = UIUtils.createSearchText(treeComposite);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(txtTemplateFilter);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(txtTemplateFilter))
			.notUpdatingParticipant()
			.to(uploadTemplate)
			.converting(new InvertingBooleanConverter())
			.in(dbc);		
		
		// the list of templates
		final TreeViewer viewer = createTemplatesViewer(treeComposite, txtTemplateFilter);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.hint(400, 180)
				.applyTo(viewer.getControl());
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(viewer.getControl()))
			.notUpdatingParticipant()
			.to(uploadTemplate)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		
		IObservableValue viewObservable = ViewerProperties.singleSelection().observe(viewer);
		final IObservableValue modelObservable = BeanProperties.value(
				ITemplateListPageModel.PROPERTY_TEMPLATE).observe(model);
		ValueBindingBuilder
			.bind(viewObservable)
			.to(modelObservable)
			.in(dbc);
		dbc.addValidationStatusProvider(new MultiValidator() {
			@Override
			protected IStatus validate() {
				Object value = modelObservable.getValue();
				if(value == null || value instanceof TemplateNode) {
					return ValidationStatus.cancel("Please select a template to create your application.");
				}
				return ValidationStatus.ok();
			}
		});
		
		//bind filter to tree
		txtTemplateFilter.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				viewer.refresh();
				viewer.expandAll();
			}
		});
		
		//details
		final Group detailsContainer = new Group(treeComposite, SWT.NONE);
		detailsContainer.setText("Details");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL)
				.hint(SWT.DEFAULT, 106)
				.applyTo(detailsContainer);
		
		new TemplateDetailViews(modelObservable, null, detailsContainer, dbc).createControls();
		
		// details button
		Button btnDetails = new Button(parent, SWT.NONE);
		btnDetails.setText("Details...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).grab(false, false)
				.applyTo(btnDetails);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(btnDetails))
				.notUpdatingParticipant()
				.to(modelObservable)
				.converting(new Converter(ITemplate.class, Boolean.class) {
					@Override
					public Object convert(Object value) {
						if(value == null || value instanceof TemplateNode) {
							return Boolean.FALSE;
						}
						return Boolean.TRUE;
					}
				})
				.in(dbc);
		btnDetails.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final String message = "The following resources will be created by using this template:";
				new ResourceSummaryDialog(getShell(), 
						model.getTemplate().getItems(), 
						"Template Details",
						message, 
						new ResourceDetailsLabelProvider(), new ResourceDetailsContentProvider()).open();
			}
			
		});
	}
	private TreeViewer createTemplatesViewer(Composite parent, final Text txtFilter) {
		TreeViewer viewer = 	new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setLabelProvider(new TemplateListPageLabelProvier());
		viewer.setContentProvider(new TemplateListPageContentProvider());
		viewer.addFilter(new TemplateViewerFilter( new ITextControl() {
			@Override
			public String getText() {
				return txtFilter.getText();
			}
		}));
		viewer.setInput(model.getTemplates());
		return viewer;
	}
	
	private static class TemplateListPageContentProvider implements ITreeContentProvider{
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
		@Override
		public void dispose() {
		}
		
		@Override
		public boolean hasChildren(Object node) {
			return (node instanceof TemplateNode);
		}
		
		@Override
		public Object getParent(Object node) {
			return null;
		}
		
		@Override
		public Object[] getElements(Object node) {
			if(!(node instanceof Collection)) return null;
			@SuppressWarnings("unchecked")
			Collection<ITemplate> templates = (Collection<ITemplate>)node;
			Map<String, TemplateNode> namespaces = new HashMap<String, TemplateNode>();
			for (ITemplate template : templates) {
				if(!namespaces.containsKey(template.getNamespace())) {
					namespaces.put(template.getNamespace(), new TemplateNode(template.getNamespace()));
				}
				namespaces.get(template.getNamespace()).getItems().add(template);
			}
			return namespaces.values().toArray();
		}
		
		@Override
		public Object[] getChildren(Object node) {
			if(!(node instanceof TemplateNode)) return null;
			return ((TemplateNode) node).getItems().toArray();
		}
	}
	
	public static class TemplateNode implements ITemplate {
		
		private String name;
		private Collection<IResource> resources = new ArrayList<IResource>();
		
		public TemplateNode(String name) {
			this.name = name;
		}
		
		public Collection<IResource> getItems(){
			return this.resources;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public Set<Class<? extends ICapability>> getCapabilities() {
			return null;
		}

		@Override
		public String getKind() {
			return null;
		}

		@Override
		public String getApiVersion() {
			return null;
		}

		@Override
		public String getCreationTimeStamp() {
			return null;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getNamespace() {
			return null;
		}

		@Override
		public Map<String, String> getLabels() {
			return new HashMap<String, String>();
		}

		@Override
		public void addLabel(String key, String value) {
		}

		@Override
		public boolean isAnnotatedWith(String key) {
			return false;
		}

		@Override
		public String getAnnotation(String key) {
			return null;
		}

		@Override
		public Map<String, String> getAnnotations() {
			return new HashMap<String, String>();
		}

		@Override
		public <T extends ICapability> T getCapability(Class<T> capability) {
			return null;
		}

		@Override
		public boolean supports(Class<? extends ICapability> capability) {
			return false;
		}

		@Override
		public <T extends ICapability, R> R accept(
				CapabilityVisitor<T, R> visitor, R unsupportedCapabililityValue) {
			return unsupportedCapabililityValue;
		}

		@Override
		public Map<String, IParameter> getParameters() {
			return new HashMap<String, IParameter>();
		}

		@Override
		public void updateParameterValues(Collection<IParameter> parameters) {
		}

		@Override
		public Map<String, String> getObjectLabels() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addObjectLabel(String key, String value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public IProject getProject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setAnnoation(String key, String value) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public interface ITextControl {
		String getText();
	}
}
