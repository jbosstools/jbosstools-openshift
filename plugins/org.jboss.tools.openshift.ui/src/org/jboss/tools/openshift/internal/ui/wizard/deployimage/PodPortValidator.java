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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

import com.openshift.restclient.model.IServicePort;

/**
 * Validates the a given Pod port
 * 
 * @author Viacheslav Kabanovich
 * @contributor Andre Dietisheim
 */
public class PodPortValidator implements IValidator {

	private static final int PORT_MIN = 0;
	private static final int PORT_MAX = 65535;
	private static final int PORT_MAXLENGTH = 63;

	private static final Pattern PORT_REGEXP = Pattern.compile("[a-z0-9]([a-z0-9-]*[a-z0-9])*");

	private static final IStatus PORT_FORMAT_ERROR_STATUS = ValidationStatus
			.error("The Pod port must be a unique integer\nor a name matching [a-z0-9]([a-z0-9-]*[a-z0-9])*");

	private static final IStatus CANCEL_STATUS = ValidationStatus
			.cancel("The Pod port must be a unique integer\nor the name of a port in the backend pods.");

	private static final IStatus PORT_UNIQUE_ERROR_STATUS = ValidationStatus
			.error(NLS.bind(ServicePortDialog.UNIQUE_ERROR, "pod"));
	private static final IStatus PORT_LENGTH_ERROR_STATUS = ValidationStatus
			.error("Pod port name exceeds the maximum length " + PORT_MAXLENGTH);
	private static final IStatus PORT_INTERVAL_ERROR_STATUS = ValidationStatus
			.error("Pod port number is out of valid interval from 0 to 65535");

	private final String podPort;
	private final List<ServicePortAdapter> ports;

	public PodPortValidator(final String podPort, final List<ServicePortAdapter> ports) {
		this.podPort = podPort;
		this.ports = ports;
	}

	@Override
	public IStatus validate(final Object value) {
		if (!(value instanceof String)
				|| StringUtils.isEmpty(value)) {
			return CANCEL_STATUS;
		}

		IStatus status = Status.OK_STATUS;
		final String port = (String) value;
		if (!port.equals(podPort)) {
			status = validateValue(port);
			if (status.isOK()) {
				status = validateUniqueness(port);
			}
		}
		return status;
	}

	private IStatus validateUniqueness(final String port) {
		for (IServicePort existing : ports) {
			if (port.equals(existing.getTargetPort())) {
				return PORT_UNIQUE_ERROR_STATUS;
			}
		}
		return Status.OK_STATUS;
	}

	private IStatus validateValue(final String port) {
		try {
			final long portNumber = Long.parseLong(port);
			// validate range
			if (portNumber < PORT_MIN 
					|| portNumber > PORT_MAX) {
				return PORT_INTERVAL_ERROR_STATUS;
			}
		} catch (NumberFormatException e) {
			// port name must match the regular expression
			// TODO: validate against backend pod ports.
			if (port.length() > PORT_MAXLENGTH) {
				return PORT_LENGTH_ERROR_STATUS;
			}
			if (!PORT_REGEXP.matcher(port).matches()) {
				return PORT_FORMAT_ERROR_STATUS;
			}
		}
		return Status.OK_STATUS;
	}

}
