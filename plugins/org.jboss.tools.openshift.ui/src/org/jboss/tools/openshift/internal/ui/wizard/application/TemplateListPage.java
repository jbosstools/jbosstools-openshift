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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.restclient.model.template.ITemplate;

/**
 * A page that offers a list templates to a user
 * @author jeff.cantrill
 *
 */
public class TemplateListPage  extends AbstractOpenShiftWizardPage  {

	public ITemplateListPageModel model;
	
	public TemplateListPage(IWizard wizard, ITemplateListPageModel model) {
		super("Select template", "Templates choices may be reduced to a smaller list by typing the name of a tag in the text field.", "templateList", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.numColumns(1)
			.margins(10, 6)
			.spacing(2, 2)
			.applyTo(parent);
		
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
		
		// the list of templates
		final TreeViewer viewer = createTemplatesViewer(treeComposite, txtTemplateFilter);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.hint(400, 180)
				.applyTo(viewer.getControl());
		final IObservableValue viewObservable = ViewerProperties.singleSelection().observe(viewer);
		final IObservableValue modelObservable = BeanProperties.value(
				ITemplateListPageModel.PROPERTY_TEMPLATE).observe(model);
		ValueBindingBuilder
			.bind(viewObservable)
			.to(modelObservable)
			.in(dbc);
		dbc.addValidationStatusProvider(new MultiValidator() {
			@Override
			protected IStatus validate() {
				if(modelObservable.getValue() ==null) {
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
		
		new TemplateDetailViews(modelObservable, null, detailsContainer, dbc).createControls();;
	}

	private TreeViewer createTemplatesViewer(Composite parent, final Text txtFilter) {
		TreeViewer viewer = 	new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if(element instanceof ITemplate) {
					ITemplate template = (ITemplate) element;
					StyledString text = new StyledString(template.getName());
					if(template.isAnnotatedWith("tags")) {
						String [] tags = template.getAnnotation("tags").split(",");
						text.append(NLS.bind(" ({0})", StringUtils.join(tags, ", ")), StyledString.DECORATIONS_STYLER);
					}
					cell.setText(text.toString());
					cell.setStyleRanges(text.getStyleRanges());
				}
				super.update(cell);
			}
		});
		viewer.setContentProvider(new TemplateListPageTreeContentProvider());
		viewer.addFilter(new AnnotationTagViewerFilter( new ITextControl() {
			@Override
			public String getText() {
				return txtFilter.getText();
			}
		}));
		viewer.setInput(model.getTemplates());
		return viewer;
	}
	
	private class TemplateListPageTreeContentProvider implements ITreeContentProvider{
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
		@Override
		public void dispose() {
		}
		
		@Override
		public boolean hasChildren(Object node) {
			return false;
		}
		
		@Override
		public Object getParent(Object node) {
			return null;
		}
		
		@Override
		public Object[] getElements(Object node) {
			return model.getTemplates().toArray();
		}
		
		@Override
		public Object[] getChildren(Object node) {
			return null;
		}
	}
	
	public interface ITextControl {
		String getText();
	}
}
