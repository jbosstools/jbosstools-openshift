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
package org.jboss.tools.openshift.internal.ui.models;

/**
 * The state of an entity with delayed loading. Such entities are initially
 * empty. When loading is started, they are filled with results fetched from
 * Openshift. Once loading has finished, a change notification will be sent for
 * the element.
 * 
 * @author Thomas Mäder
 *
 */
public enum LoadingState {
	/** The no loading attempt has been made yet **/
	INIT,
	/** the element is currently being loaded **/
	LOADING,
	/** loading has been stopped, either cancelled or with exception **/
	LOAD_STOPPED,
	/** Loading has finished, the element can be used **/
	LOADED
}
