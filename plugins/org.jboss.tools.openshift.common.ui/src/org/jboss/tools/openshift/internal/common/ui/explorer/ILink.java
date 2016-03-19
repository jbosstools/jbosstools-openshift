/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.explorer;

/**
 * If element data implements this interface, view and label provider
 * may support link style for the tree node:
 * - Custom implementation of view sets hand style cursor and on click 
 *   calls method execute().
 * - Custom label provider underlines text and sets blue color to it.
 *  
 * @author Viacheslav Kabanovich
 *
 */
public interface ILink {

	public void execute();
}
