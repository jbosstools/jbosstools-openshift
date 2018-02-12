/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.core.condition;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.condition.WaitCondition;

/**
 * Class taking care of handling multiple wait conditions with possibility of passing 
 * method defined with Consumer functional interface 
 * @author odockal
 *
 */
public class MultipleWaitConditionHandler extends AbstractWaitCondition {
	
	private String description = "";
	
	private Map<WaitCondition, Consumer<Object>> awaitsConditions = new HashMap<WaitCondition, Consumer<Object>>();

	public MultipleWaitConditionHandler(Map<WaitCondition, Consumer<Object>> conds) {
		awaitsConditions = conds;
		StringBuilder descriptionBuilder = new StringBuilder();
		for (WaitCondition wait : conds.keySet()) {
			descriptionBuilder.append(wait.description() + " | ");
		}
		description = descriptionBuilder.toString();
	}
	
	public MultipleWaitConditionHandler(Map<WaitCondition, Consumer<Object>> conds, String desc) {
		awaitsConditions = conds;
		description = desc;
	}
	
	@Override
	public boolean test() {
		for (Entry<WaitCondition, Consumer<Object>> entry : awaitsConditions.entrySet()) {
			if (entry.getKey().test()) {
				entry.getValue().accept(entry.getKey().getResult());
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String description() {
		return description;
	}

}
