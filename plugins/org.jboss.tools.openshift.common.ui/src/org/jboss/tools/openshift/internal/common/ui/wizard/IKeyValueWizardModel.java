/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.wizard;

import org.eclipse.core.databinding.validation.IValidator;

/**
 * 
 * @author jeff.cantrill
 *
 * @param <T> an IKeyValueItem type
 */
public interface IKeyValueWizardModel<T extends IKeyValueItem> {
	String PROPERTY_KEY = "key";
	String PROPERTY_VALUE = "value";
	
	String getKey();
	void setKey(String key);
	
	String getValue();
	void setValue(String value);
	
	String getTitle();
	String getDescription();
	String getWindowTitle();

	/**
	 * Used as tooltip at key input label.
	 * @return
	 */
	String getKeyDescription();
	/**
	 * Used as tooltip at value input label.
	 * @return
	 */
	String getValueDescription();
	
	/**
	 * Retrieve the validator to be applied after the key is converted
	 * @return
	 */
	IValidator getKeyAfterConvertValidator();

	/**
	 * Retrieve the validator to be applied after the value is converted
	 * @return
	 */
	IValidator getValueAfterConvertValidator();
	
	/**
	 * The text to be used for the group text (e.g. Environment Variable)
	 * @return
	 */
	String getGroupLabel();
	
	/**
	 * The text to identify the key (e.g. name)
	 * @return
	 */
	String getKeyLabel();
	
	/**
	 * The text to identify the value (e.g. value)
	 * @return
	 */
	String getValueLabel();
	
	/**
	 * Is the key editable
	 * @return
	 */
	boolean isKeyEditable();
}
