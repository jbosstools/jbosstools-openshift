/**
 * Copyright 2013-2015 Docker, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.tools.openshift.internal.ui.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * Validator for Docker image names.
 * <p>
 * Regular expressions used to validate a Docker image name where copied from <a href="https://github.com/docker/docker/blob/6964f4235035c5e94f6a0c35149783e910fed313/vendor/src/github.com/docker/distribution/reference/regexp.go
 * ">Docker's regexp.go</a>
 * </p>
 * @author Fred Bricon
 *
 */
public class DockerImageValidator implements IValidator {

	private String hostnameComponentRegexp = "(?:[a-z0-9]|[a-z0-9][a-z0-9-]*[a-z0-9])";

	// hostnameComponentRegexp restricts the registry hostname component of a
	// repository name to
	// start with a component as defined by hostnameRegexp and followed by an
	// optional port.
	private String hostnameRegexp = "(?:" + hostnameComponentRegexp + "\\.)*" + hostnameComponentRegexp + "(?::[0-9]+)?";

	// TagRegexp matches valid tag names. From docker/docker:graph/tags.go.
	private String tagRegexp = "[\\w][\\w.-]{0,127}";

	// nameSubComponentRegexp defines the part of the name which must be
	// begin and end with an alphanumeric character. These characters can
	// be separated by any number of dashes.
	private String nameSubComponentRegexp = "[a-z0-9]+(?:[-]+[a-z0-9]+)*";

	// nameComponentRegexp restricts registry path component names to
	// start with at least one letter or number, with following parts able to
	// be separated by one period, underscore or double underscore.
	private String nameComponentRegexp = nameSubComponentRegexp + "(?:(?:[._]|__)" + nameSubComponentRegexp + ")*";

	private String nameRegexp = "(?:" + nameComponentRegexp + "/)*" + nameComponentRegexp;

	// digestRegexp matches valid digests.
	private String digestRegexp = "[A-Za-z][A-Za-z0-9]*(?:[-_+.][A-Za-z][A-Za-z0-9]*)*[:][[:xdigit:]]{32,}";

	// referenceRegexp is the full supported format of a reference. The
	// regexp has capturing groups for name, tag, and digest components.
	private String referenceRegexp = "^((?:" + hostnameRegexp + "/)?" + nameRegexp + ")(?:[:](" + tagRegexp +"))?(?:[@](" + digestRegexp + "))?$";

	private Pattern IMAGE_PATTERN = Pattern.compile(referenceRegexp);

	private static String ERROR_MSG = "Please provide an existing docker image in the format of [[<repo>/]<namespace>/]<name>[:<tag>].\n"
			+ " The defaults are: repo=docker.io, namespace=library, tag=latest";
	
	@Override
	public IStatus validate(Object value) {
		if (!(value instanceof String)) {
			return ValidationStatus.cancel(ERROR_MSG);
		}
		String name = (String) value; 
		IStatus status = validateImageName(name);
		if (status.isOK()) {
			status = additionalValidation(name);
		}
		return status;
	}

	public IStatus validateImageName(String repoName) {
		String uri = repoName;
		if (StringUtils.isEmpty(uri) || uri.contains("://")) {
			return ValidationStatus.cancel(ERROR_MSG);
		}
		Matcher matcher = IMAGE_PATTERN.matcher(repoName);
		if (!matcher.matches()) {
			return ValidationStatus.cancel(ERROR_MSG);
		}
		return ValidationStatus.ok();
	}

	public IStatus additionalValidation(String repoName) {
		return ValidationStatus.ok();
	}
}