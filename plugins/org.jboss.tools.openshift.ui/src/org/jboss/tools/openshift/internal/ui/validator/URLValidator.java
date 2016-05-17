/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.validator;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * Validator for validating URLs in the UI
 * @author jeff.cantrill
 *
 */
public class URLValidator implements IValidator{
	
	private static final String [] SCHEMES = new String [] {"http", "https"};
	private final String invalidURLMessage;
	private boolean allowEmpty = false;
	private UrlValidator validator = new UrlValidator(SCHEMES, UrlValidator.ALLOW_LOCAL_URLS);
	
	/**
	 * @param urlType  The value to plug into error message of 'Please provide a valid TYPE URL'
	 * @param allowEmpty  Empty/Blank values will return OK
	 */
	public URLValidator(String urlType, boolean allowEmpty) {
		invalidURLMessage = NLS.bind("Please provide a valid {0} (HTTP/S) URL.", urlType);
		this.allowEmpty  = allowEmpty;
	}
	
	

	@Override
	public IStatus validate(Object in) {
		String value = (String) in;
		if(allowEmpty && StringUtils.isBlank(value)) {
			return ValidationStatus.ok();
		}
		if (!validator.isValid(value)) {
			return ValidationStatus.error(invalidURLMessage);
		}
		return ValidationStatus.ok();
	}
	
}
