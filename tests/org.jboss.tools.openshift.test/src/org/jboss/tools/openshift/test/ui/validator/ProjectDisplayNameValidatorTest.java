package org.jboss.tools.openshift.test.ui.validator;

import org.jboss.tools.openshift.internal.ui.validator.ProjectDisplayNameValidator;
import org.junit.Test;

public class ProjectDisplayNameValidatorTest extends AbstractValidatorTest {

	public ProjectDisplayNameValidatorTest() {
		super(new ProjectDisplayNameValidator());
	}
	
	@Test
	public void validDisplayNameShouldBeValid() {
		assertPass("jboss tools dream team");
	}
	
	@Test
	public void emptyDisplayNameShouldBeValid() {
		assertPass("");
		assertPass(null);
	}
	
	@Test
	public void notStringShouldBeCanceled() {
		assertCancel(new Object());
	}
	
	@Test
	public void tabsShouldBeInvalid() {
		assertFailure("jboss\tools");
	}
	
	@Test
	public void newLinesShouldBeInvalid() {
		assertFailure("happy\new year");
	}
	
	@Test
	public void veryLongDisplayNameShouldBeInvalid() {
		assertFailure(new String(new char[ProjectDisplayNameValidator.DISPLAY_NAME_LENGTH_LIMIT + 1]));
	}

}
