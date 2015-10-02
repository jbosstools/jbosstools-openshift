/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *     
 * based on org.eclipse.jface.databinding.wizard.WizardPageSupport
 *
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.StaleEvent;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.util.Policy;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.dialog.IValidationMessageProvider;
import org.eclipse.jface.databinding.dialog.ValidationMessageProvider;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Connects the validation result from the given data binding context to the
 * given wizard fragment, updating the page's error message accordingly.
 *
 * @since 1.3
 */
public class FormPresenterSupport {
	private IFormPresenter formPresenter;
	private DataBindingContext dbc;
	private IValidationMessageProvider messageProvider = new ValidationMessageProvider();
	private IObservableValue aggregateStatusProvider;
	private boolean uiChanged = false;
	private IChangeListener uiChangeListener = new IChangeListener() {
		@Override
		public void handleChange(ChangeEvent event) {
			handleUIChanged();
		}
	};
	private IListChangeListener validationStatusProvidersListener = new IListChangeListener() {
		@Override
		public void handleListChange(ListChangeEvent event) {
			ListDiff diff = event.diff;
			ListDiffEntry[] differences = diff.getDifferences();
			for (int i = 0; i < differences.length; i++) {
				ListDiffEntry listDiffEntry = differences[i];
				ValidationStatusProvider validationStatusProvider = (ValidationStatusProvider) listDiffEntry
						.getElement();
				IObservableList targets = validationStatusProvider.getTargets();
				if (listDiffEntry.isAddition()) {
					targets
							.addListChangeListener(validationStatusProviderTargetsListener);
					for (Iterator it = targets.iterator(); it.hasNext();) {
						((IObservable) it.next())
								.addChangeListener(uiChangeListener);
					}
				} else {
					targets
							.removeListChangeListener(validationStatusProviderTargetsListener);
					for (Iterator it = targets.iterator(); it.hasNext();) {
						((IObservable) it.next())
								.removeChangeListener(uiChangeListener);
					}
				}
			}
		}
	};
	private IListChangeListener validationStatusProviderTargetsListener = new IListChangeListener() {
		@Override
		public void handleListChange(ListChangeEvent event) {
			ListDiff diff = event.diff;
			ListDiffEntry[] differences = diff.getDifferences();
			for (int i = 0; i < differences.length; i++) {
				ListDiffEntry listDiffEntry = differences[i];
				IObservable target = (IObservable) listDiffEntry.getElement();
				if (listDiffEntry.isAddition()) {
					target.addChangeListener(uiChangeListener);
				} else {
					target.removeChangeListener(uiChangeListener);
				}
			}
		}
	};
	private ValidationStatusProvider currentStatusProvider;
	protected IStatus currentStatus;
	protected boolean currentStatusStale;

	/**
	 * @param formPresenter
	 * @param dbc
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	public FormPresenterSupport(IFormPresenter formPresenter, DataBindingContext dbc) {
		this.formPresenter = formPresenter;
		this.dbc = dbc;
		init();
	}

	/**
	 * Sets the {@link IValidationMessageProvider} to use for providing the
	 * message text and message type to display on the dialog page.
	 *
	 * @param messageProvider
	 *            The {@link IValidationMessageProvider} to use for providing
	 *            the message text and message type to display on the dialog
	 *            page.
	 *
	 * @since 1.4
	 */
	public void setValidationMessageProvider(
			IValidationMessageProvider messageProvider) {
		this.messageProvider = messageProvider;
		handleStatusChanged();
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void init() {
		ObservableTracker.setIgnore(true);
		try {
			aggregateStatusProvider = new MaxSeverityValidationStatusProvider(dbc);
		} finally {
			ObservableTracker.setIgnore(false);
		}

		aggregateStatusProvider
				.addValueChangeListener(new IValueChangeListener() {
					@Override
					public void handleValueChange(ValueChangeEvent event) {
						statusProviderChanged();
					}
				});
		formPresenter.getControl().addListener(SWT.Dispose, new Listener() {
			@Override
			public void handleEvent(Event event) {
				dispose();
			}
		});
		aggregateStatusProvider.addStaleListener(new IStaleListener() {
			@Override
			public void handleStale(StaleEvent staleEvent) {
				currentStatusStale = true;
				handleStatusChanged();
			}
		});
		statusProviderChanged();
		dbc.getValidationStatusProviders().addListChangeListener(
				validationStatusProvidersListener);
		for (Iterator it = dbc.getValidationStatusProviders().iterator(); it
				.hasNext();) {
			ValidationStatusProvider validationStatusProvider = (ValidationStatusProvider) it
					.next();
			IObservableList targets = validationStatusProvider.getTargets();
			targets
					.addListChangeListener(validationStatusProviderTargetsListener);
			for (Iterator iter = targets.iterator(); iter.hasNext();) {
				((IObservable) iter.next()).addChangeListener(uiChangeListener);
			}
		}
	}

	private void statusProviderChanged() {
		currentStatusProvider = (ValidationStatusProvider) aggregateStatusProvider
				.getValue();
		if (currentStatusProvider != null) {
			currentStatus = (IStatus) currentStatusProvider
					.getValidationStatus().getValue();
		} else {
			currentStatus = null;
		}
		currentStatusStale = aggregateStatusProvider.isStale();
		handleStatusChanged();
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void handleUIChanged() {
		uiChanged = true;
		if (currentStatus != null) {
			handleStatusChanged();
		}
		dbc.getValidationStatusProviders().removeListChangeListener(
				validationStatusProvidersListener);
		for (Iterator it = dbc.getValidationStatusProviders().iterator(); it
				.hasNext();) {
			ValidationStatusProvider validationStatusProvider = (ValidationStatusProvider) it
					.next();
			IObservableList targets = validationStatusProvider.getTargets();
			targets
					.removeListChangeListener(validationStatusProviderTargetsListener);
			for (Iterator iter = targets.iterator(); iter.hasNext();) {
				((IObservable) iter.next())
						.removeChangeListener(uiChangeListener);
			}
		}
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void handleStatusChanged() {
		handleMessage();
		handleComplete();
	}

	private void handleComplete() {
		boolean pageComplete = true;
		if (currentStatusStale) {
			pageComplete = false;
		} else if (currentStatus != null) {
			pageComplete = !currentStatus.matches(IStatus.ERROR
					| IStatus.CANCEL);
		}
		formPresenter.setComplete(pageComplete);
	}

	private void handleMessage() {
		String message = messageProvider.getMessage(currentStatusProvider);
		int type = messageProvider.getMessageType(currentStatusProvider);
		if (type == IMessageProvider.ERROR) {
			formPresenter.setMessage(uiChanged ? message : null, IMessageProvider.ERROR);
			if (currentStatus != null && currentStatusHasException()) {
				handleStatusException();
			}
		} else {
			formPresenter.setMessage(message, type);
		}
	}

	private boolean currentStatusHasException() {
		boolean hasException = false;
		if (currentStatus.getException() != null) {
			hasException = true;
		}
		if (currentStatus instanceof MultiStatus) {
			MultiStatus multiStatus = (MultiStatus) currentStatus;

			for (int i = 0; i < multiStatus.getChildren().length; i++) {
				IStatus status = multiStatus.getChildren()[i];
				if (status.getException() != null) {
					hasException = true;
					break;
				}
			}
		}
		return hasException;
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void handleStatusException() {
		if (currentStatus.getException() != null) {
			logThrowable(currentStatus.getException());
		} else if (currentStatus instanceof MultiStatus) {
			MultiStatus multiStatus = (MultiStatus) currentStatus;
			for (int i = 0; i < multiStatus.getChildren().length; i++) {
				IStatus status = multiStatus.getChildren()[i];
				if (status.getException() != null) {
					logThrowable(status.getException());
				}
			}
		}
	}

	private void logThrowable(Throwable throwable) {
		Policy
				.getLog()
				.log(
						new Status(
								IStatus.ERROR,
								Policy.JFACE_DATABINDING,
								IStatus.OK,
								"Unhandled exception: " + throwable.getMessage(), throwable)); //$NON-NLS-1$
	}

	/**
	 * Disposes of this wizard page support object, removing any listeners it
	 * may have attached.
	 */
	public void dispose() {
		if (aggregateStatusProvider != null)
			aggregateStatusProvider.dispose();
		if (dbc != null && !uiChanged) {
			for (Iterator it = dbc.getValidationStatusProviders().iterator(); it
					.hasNext();) {
				ValidationStatusProvider validationStatusProvider = (ValidationStatusProvider) it
						.next();
				IObservableList targets = validationStatusProvider.getTargets();
				targets
						.removeListChangeListener(validationStatusProviderTargetsListener);
				for (Iterator iter = targets.iterator(); iter.hasNext();) {
					((IObservable) iter.next())
							.removeChangeListener(uiChangeListener);
				}
			}
			dbc.getValidationStatusProviders().removeListChangeListener(
					validationStatusProvidersListener);
		}
		aggregateStatusProvider = null;
		dbc = null;
		uiChangeListener = null;
		validationStatusProvidersListener = null;
		validationStatusProviderTargetsListener = null;
		formPresenter = null;
	}

	public interface IFormPresenter {
		
		public void setMessage(String message, int type);
		public void setComplete(boolean complete);
		public Control getControl();
	}

	class MaxSeverityValidationStatusProvider extends ComputedValue {

		private Collection validationStatusProviders;

		public MaxSeverityValidationStatusProvider(DataBindingContext dbc) {
			super(ValidationStatusProvider.class);
			this.validationStatusProviders = dbc.getValidationStatusProviders();
		}

		@Override
		protected Object calculate() {
			int maxSeverity = IStatus.OK;
			ValidationStatusProvider maxSeverityProvider = null;
			for (Iterator it = validationStatusProviders.iterator(); it.hasNext();) {
				ValidationStatusProvider provider = (ValidationStatusProvider) it
						.next();
				IStatus status = (IStatus) provider.getValidationStatus()
						.getValue();
				if (status.getSeverity() > maxSeverity) {
					maxSeverity = status.getSeverity();
					maxSeverityProvider = provider;
				}
			}
			return maxSeverityProvider;
		}

		@Override
		public synchronized void dispose() {
			validationStatusProviders = null;
			super.dispose();
		}
	}
}
