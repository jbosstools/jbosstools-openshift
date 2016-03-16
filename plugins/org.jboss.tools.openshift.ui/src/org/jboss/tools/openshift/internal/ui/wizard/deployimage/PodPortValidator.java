/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.restclient.model.IServicePort;

/**
 * Validates the input Pod port
 */
public class PodPortValidator implements IValidator {
	
	private static final int POD_PORT_MAXLENGTH = 63;
	
	private static final Pattern POD_PORT_REGEXP = Pattern.compile("[a-z0-9]([a-z0-9-]*[a-z0-9])*");

	private static final IStatus  POD_PORT_FORMAT_ERROR_STATUS = ValidationStatus.error("The Pod port must be a unique integer\nor a name matching [a-z0-9]([a-z0-9-]*[a-z0-9])*");

	private static final IStatus CANCEL_STATUS = ValidationStatus.cancel("The Pod port must be a unique integer\nor the name of a port in the backend pods.");
	
	private static final IStatus POD_PORT_UNIQUE_ERROR_STATUS = ValidationStatus.error(NLS.bind(ServicePortDialog.UNIQUE_ERROR, "pod"));
	private static final IStatus POD_PORT_LENGTH_ERROR_STATUS = ValidationStatus.error("Pod port name exceeds the maximum length " + POD_PORT_MAXLENGTH);
	private static final IStatus POD_PORT_INTERVAL_ERROR_STATUS = ValidationStatus.error("Pod port number is out of valid interval from 0 to 65535");

	private final String podPort;
	
	private final List<IServicePort> ports;
	
	public PodPortValidator(final String podPort, final List<IServicePort> ports) {
		this.podPort = podPort;
		this.ports = ports;
	}
	
	@Override
	public IStatus validate(final Object value) {
		// port cannot be empty
		if(StringUtils.isEmpty(value)) {
			return CANCEL_STATUS;
		}
		final String newPodPort = (String) value;
		if (!newPodPort.equals(podPort)) {
			try {
				final long portNumber = Long.valueOf(value.toString());
				// validate range
				if (portNumber < 0 || portNumber > 65535) {
					return POD_PORT_INTERVAL_ERROR_STATUS;
				}
			} catch (NumberFormatException e) {
				// port name must match the regular expression
				// TODO: validate against backend pod ports.
				if (newPodPort.length() > POD_PORT_MAXLENGTH) {
					return POD_PORT_LENGTH_ERROR_STATUS;
				}
				if (!POD_PORT_REGEXP.matcher(newPodPort).matches()) {
					return POD_PORT_FORMAT_ERROR_STATUS;
				}
			}
			for (IServicePort port : ports) {
				if (newPodPort.equals(port.getTargetPort())) {
					return POD_PORT_UNIQUE_ERROR_STATUS;
				}
			}
		}
		return ValidationStatus.OK_STATUS;
	}
	
}
