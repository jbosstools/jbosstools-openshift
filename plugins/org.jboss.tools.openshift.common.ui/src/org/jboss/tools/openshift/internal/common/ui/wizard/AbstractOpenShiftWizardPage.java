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
package org.jboss.tools.openshift.internal.common.ui.wizard;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;

/**
 * @author André Dietisheim
 */
public abstract class AbstractOpenShiftWizardPage extends WizardPage {

	public enum Direction {
		FORWARDS {
			@Override
			public IWizardPage getFollowingPage(IWizardPage page) {
				return page.getNextPage();
			}
		}, BACKWARDS {
			@Override
			public IWizardPage getFollowingPage(IWizardPage page) {
				return page.getPreviousPage();
			}
		};
		
		public abstract IWizardPage getFollowingPage(IWizardPage currentPage);
	}
	
	private DataBindingContext dbc;
	
	protected AbstractOpenShiftWizardPage(String title, String description, String pageName, IWizard wizard) {
		super(pageName);
		setWizard(wizard);
		setTitle(title);
		setDescription(description);
		setImageDescriptor(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM);
	}

	@Override
	public void createControl(Composite parent) {
		this.dbc = new DataBindingContext();
		setupWizardPageSupport(dbc);
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(6,6).applyTo(container);
		Composite child = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(child);
		setControl(container);
		initPageChangeListeners();
		doCreateControls(child, dbc);
		parent.getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				DataBindingUtils.dispose(dbc);
				dbc = null;
			}
		});
	}

	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.INFO | IStatus.WARNING | IStatus.CANCEL, this,
				dbc);
	}

	/**
	 * Sets this page to get informed of page changes before they occur and
	 * changes that occurred (if its running in WizardDialog).
	 * 
	 * @see WizardDialog#addPageChangedListener(IPageChangedListener)
	 * @see WizardDialog#addPageChangingListener(IPageChangingListener)
	 */
	protected void initPageChangeListeners() {
		IWizardContainer container = getContainer();
		if (container instanceof WizardDialog) {
			((WizardDialog) container).addPageChangedListener(onPageChanged());
			((WizardDialog) container).addPageChangingListener(onPageChanging());
		}
	}

	private IPageChangingListener onPageChanging() {
		return new IPageChangingListener() {

			@Override
			public void handlePageChanging(PageChangingEvent event) {
				if (isChangingToThisPage(event)) {
					if (event.getCurrentPage() == null
							|| equals((IWizardPage) event.getCurrentPage(), getPreviousPage())) {
						onPageWillGetActivated(Direction.FORWARDS, event, dbc);
					} else {
						onPageWillGetActivated(Direction.BACKWARDS, event, dbc);
					}
				} else if (isChangingFromThisPage(event)) {
					if (event.getTargetPage() != null
							&& equals((IWizardPage) event.getTargetPage(), getPreviousPage())) {
						onPageWillGetDeactivated(Direction.BACKWARDS, event, dbc);
					} else if (equals((IWizardPage) event.getTargetPage(), getNextPage())) {
						onPageWillGetDeactivated(Direction.FORWARDS, event, dbc);
					}
				}
			}

			private boolean isChangingToThisPage(PageChangingEvent event) {
				return equals((IWizardPage) event.getTargetPage(), AbstractOpenShiftWizardPage.this);
			}
			
			private boolean isChangingFromThisPage(PageChangingEvent event) {
				return equals((IWizardPage) event.getCurrentPage(), AbstractOpenShiftWizardPage.this);
			}

			private boolean equals(Object thisPage, Object thatPage) {
				if (!(thisPage instanceof IWizardPage)) {
					return thatPage == null;
				}
				
				if (!(thatPage instanceof IWizardPage)) {
					return false;
				}
				
				return thisPage == thatPage 
						|| ((IWizardPage) thisPage).getControl() == ((IWizardPage) thatPage).getControl();
			}

		};
	}

	private IPageChangedListener onPageChanged() {
		return new IPageChangedListener() {

			@Override
			public void pageChanged(PageChangedEvent event) {
				if (event.getSelectedPage() == AbstractOpenShiftWizardPage.this) {
					onPageActivated(dbc);
				} else {
					onPageDeactivated(dbc);
				}
			}
		};
	}

	protected DataBindingContext getDatabindingContext() {
		return dbc;
	}

	protected void onPageActivated(DataBindingContext dbc) {
	}
	
	protected void onPageDeactivated(DataBindingContext dbc) {
	}

	/**
	 * Callback that gets called before the page changes. 
	 * <p>
	 * Attention: this is not called when the very first wizard page gets shown for the first time.
	 * 
	 * @param direction
	 * @param event
	 * @param dbc
	 */
	protected void onPageWillGetActivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
	}

	/**
	 * Callback that gets called when this page is going to be deactivated.
	 *  
	 * @param progress the direction that the wizard is moving: backwards/forwards
	 * @param event the page changing event that may be use to veto the change 
	 * @param dbc the current data binding context
	 */
	protected void onPageWillGetDeactivated(Direction progress, PageChangingEvent event, DataBindingContext dbc) {
	}

	protected abstract void doCreateControls(Composite parent, DataBindingContext dbc);

	protected DataBindingContext getDataBindingContext() {
		return dbc;
	}

}
