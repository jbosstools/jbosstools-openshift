/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author André Dietisheim
 */
public class EditDomainWizardPage extends AbstractOpenShiftWizardPage {

	private EditDomainWizardModel pageModel;

	public EditDomainWizardPage(String title, String description, EditDomainWizardModel model, IWizard wizard) {
		super(title, description, "", wizard);
		this.pageModel = model;
	}

	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(parent);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(2).applyTo(parent);

		// domain name
		Label namespaceLabel = new Label(parent, SWT.NONE);
		namespaceLabel.setText(OpenshiftUIMessages.DomainName);
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).applyTo(namespaceLabel);
		Text namespaceText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(namespaceText);
		ISWTObservableValue namespaceTextObservable = 
				WidgetProperties.text(SWT.Modify).observe(namespaceText);
		NamespaceValidator namespaceValidator = new NamespaceValidator(namespaceTextObservable);
		dbc.addValidationStatusProvider(namespaceValidator);
		ControlDecorationSupport.create(namespaceValidator, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		IObservableValue namespaceModelObservable = 
				BeanProperties.value(EditDomainWizardModel.PROPERTY_DOMAIN_ID).observe(pageModel);
		ValueBindingBuilder
			.bind(namespaceTextObservable)
			.to(namespaceModelObservable)
			.in(dbc);
		
		// edit domain members
		Link editMembersLink = new Link(parent, SWT.NONE);
		editMembersLink.setText("Checking web console availability...");
		editMembersLink.setEnabled(false);
		GridDataFactory.fillDefaults()
				.span(2,1).align(SWT.LEFT, SWT.CENTER).applyTo(editMembersLink);
		new EditDomainMembersEnablement(editMembersLink).schedule();
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}
	
	class NamespaceValidator extends MultiValidator {

		private final ISWTObservableValue domainNameObservable;

		public NamespaceValidator(ISWTObservableValue domainNameObservable) {
			this.domainNameObservable = domainNameObservable;
		}

		@Override
		protected IStatus validate() {
			final String domainName = (String) domainNameObservable.getValue();
			if (pageModel.isCurrentDomainId(domainName)) {
				return ValidationStatus.cancel(getDescription());
			}
			if (domainName.isEmpty()) {
				return ValidationStatus.cancel(
						OpenshiftUIMessages.EnterDomainName);
			}
			if (!StringUtils.isAlphaNumeric(domainName)) {
				return ValidationStatus.error(
						OpenshiftUIMessages.DomainNameMayHaveLettersAndDigits);
			}
			if (domainName.length() > 16) {
				return ValidationStatus.error(
						OpenshiftUIMessages.DomainNameMaximumLength);
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(domainNameObservable);
			return targets;
		}
	}

	private class EditDomainMembersEnablement extends Job {

		private Link editDomainMembersLink;

		public EditDomainMembersEnablement(Link editDomainMembersLink) {
			super("Enabling/disabling edit members link");
			this.editDomainMembersLink = editDomainMembersLink;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final String webUIDomainPageUrl = pageModel.getWebUIDomainPageUrl();
			setEditDomainMembersLink(webUIDomainPageUrl);
			return Status.OK_STATUS;
		}

		private void setEditDomainMembersLink(final String webUIDomainPage) {
			getShell().getDisplay().syncExec(new Runnable() {
				
				@Override
				public void run() {
					if (webUIDomainPage != null) {
						editDomainMembersLink.setText("<a>Edit domain members</a>");
						editDomainMembersLink.addSelectionListener(onEditMembers(webUIDomainPage));
					} else {
						editDomainMembersLink.setText("Could not find the OpenShift web console");
					}
					editDomainMembersLink.setEnabled(webUIDomainPage != null);
					editDomainMembersLink.getParent().layout(true, true);
				}
			});
		}
		
		private SelectionListener onEditMembers(final String webUIDomainPage) {
			return new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					new BrowserUtility().checkedCreateExternalBrowser(webUIDomainPage, OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
				}
			};
		}
	}
	
}
