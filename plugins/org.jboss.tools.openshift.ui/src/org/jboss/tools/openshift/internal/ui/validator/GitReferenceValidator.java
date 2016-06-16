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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class GitReferenceValidator extends MultiValidator implements IValidator {
	private final static Map<Character, Integer> charactersToExclusionRule = new HashMap<>();
	private final static Map<Character, Character> twoCharacterSequences = new HashMap<>();

	static {
		//Rule 3. Reference cannot have two consecutive dots
		twoCharacterSequences.put('.', '.');

		//Rule 4. Reference cannot have ASCII control characters (i.e. bytes whose values
        //are lower than \040, or \177 DEL), space, tilde ~, caret ^, or colon : anywhere
		for (int i = 0; i < 040; i++) charactersToExclusionRule.put((char)i, 4);
		String rule4chars = "" + (char)177 + " ~^:";
		for (int i = 0; i < rule4chars.length(); i++) charactersToExclusionRule.put(rule4chars.charAt(i), 4);

		//Rule 5. Reference cannot have question-mark ?, asterisk *, or open bracket [ anywhere
		String rule5chars = "?*[";
		for (int i = 0; i < rule5chars.length(); i++) charactersToExclusionRule.put(rule5chars.charAt(i), 5);

		//Rule 6.b. Reference cannot contain multiple consecutive slashes
		twoCharacterSequences.put('/', '/'); 

		//Rule 8. Reference cannot contain a sequence @{
		twoCharacterSequences.put('@', '{');

		//Rule 10. Reference cannot contain a \
		charactersToExclusionRule.put('\\', 10);
		
	}

	private IObservableValue<String> observable;

	public GitReferenceValidator(IObservableValue<String> observable) {
		this.observable = observable;
	}

	public GitReferenceValidator() {
	}

	@Override
	public IStatus validate(Object value) {
		if(!(value instanceof String)) {
			return ValidationStatus.OK_STATUS;
		}
		String ref = (String)value;
		if(StringUtils.isBlank(ref)) {
			return ValidationStatus.OK_STATUS;
		}

		//Rule 2 (Reference must contain at least one /) is waived as one level is allowed.

		//Applying rules 3, 4, 5, 6.b, 8,10
		for (int i = 0; i < ref.length(); i++) {
			char c = ref.charAt(i);
			Integer rule = charactersToExclusionRule.get(c);
			if(rule != null) {
				return ValidationStatus.error("Reference cannot contain character '" + c + "'");
			}
			
			Character next = twoCharacterSequences.get(c);
			if(next != null && i + 1 < ref.length() && next.charValue() == ref.charAt(i + 1)) {
				return ValidationStatus.error("Reference cannot contain sequence '" + c + next + "'");
			}
		}

		//Rule 6.a. Reference cannot begin or end with a slash /
		if(ref.startsWith("/") || ref.endsWith("/")) {
			return ValidationStatus.error("Reference cannot begin or end with '/'");
		}

		//Rule 7. Reference cannot end with a dot
		if(ref.endsWith(".")) {
			return ValidationStatus.error("Reference cannot end with a dot");
		}

		//Rule 9. Reference cannot be a single character '@'
		if(ref.equals("@")) {
			return ValidationStatus.error("Reference cannot be a single character '@'");
		}

		//Rule 1. Reference or its slash-separated component cannot begin with a dot or end with the sequence .lock
		for (String part: ref.split("/")) {
			if(part.startsWith(".")) {
				return ValidationStatus.error("Reference or its slash-separated component cannot start with a dot");
			} else if(part.endsWith(".lock")) {
				return ValidationStatus.error("Reference or its slash-separated component cannot end with '.lock'");
			}
		}
		
		return ValidationStatus.OK_STATUS;
	}

	@Override
	protected IStatus validate() {
		if(observable == null) {
			return ValidationStatus.OK_STATUS;
		}
		return validate(observable.getValue());
	}

}
