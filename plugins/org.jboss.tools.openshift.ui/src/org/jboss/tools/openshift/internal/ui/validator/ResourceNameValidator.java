/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.validator;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.runtime.IStatus;

/**
 * Common validator for validating a resource name when generating
 * multiple resources (e.g. new-app). ServiceName is the limiting resource
 * 
 * @author jeff.cantrill
 *
 */
public class ResourceNameValidator extends MultiValidator{
	
	private final ServiceNameValidator validator = new ServiceNameValidator();
	private final IObservableValue<String> observable;
	
	public ResourceNameValidator(IObservableValue<String> observable){
		this.observable = observable;
	}
	@Override
	protected IStatus validate() {
		return validator.validate(observable.getValue());
	}
	
}