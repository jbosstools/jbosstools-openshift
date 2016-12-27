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
package org.jboss.tools.openshift.test.handler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.ui.handler.PodLogsHandler;
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IPod;

@RunWith(MockitoJUnitRunner.class)
public class PodLogsHandlerTest {
	
	@Mock private IBuild build;
	@Mock private IResourceWrapper<IBuild, ?> uiModel;
	@Mock private IPod pod;
	@Mock private IContainer container;
	
	private TestPodLogsHandler handler;
	@SuppressWarnings("rawtypes")
	private Map parameters = new HashMap();
	private ExecutionEvent event;
	
	@Before
	public void setUp() throws Exception {
		handler = spy(new TestPodLogsHandler());
		event = new ExecutionEvent(null, parameters, null, null);
	}

	@Test
	public void testGetLogsFromPodWhenRunning() throws Exception{
		givenAPodIsSelected("Running");
		handler.execute(event);
		thenTheLogsShouldBeShown();
	}

    @Test
    public void testGetLogsFromPodWhenSucceeded() throws Exception{
        givenAPodIsSelected("Succeeded");
        handler.execute(event);
        thenTheLogsShouldBeShown();
    }

    @Test
    public void testGetLogsFromPodWhenFailed() throws Exception{
        givenAPodIsSelected("Failed");
        handler.execute(event);
        thenTheLogsShouldBeShown();
    }

    @Test
    public void testGetLogsFromPodWhenCompleted() throws Exception{
        givenAPodIsSelected("Completed");
        handler.execute(event);
        thenTheLogsShouldBeShown();
    }

    @Test
    public void testGetLogsFromPodWhenPending() throws Exception{
        givenAPodIsSelected("Pending");
        handler.execute(event);
        thenInvalidStateShouldBeShown("Pending");
    }

    @Test
    public void testGetLogsFromPodWhenUnknown() throws Exception{
        givenAPodIsSelected("Unknown");
        handler.execute(event);
        thenInvalidStateShouldBeShown("Unknown");
    }

    private void givenAPodIsSelected(String status) {
	    doReturn(status).when(pod).getStatus();
	    doReturn(Arrays.asList(container)).when(pod).getContainers();
	    doReturn(pod).when(handler).getSelectedElement(any(ExecutionEvent.class), eq(IPod.class));
	}

	private void thenTheLogsShouldBeShown() {
		verify(handler, times(1)).showLogs(any(), any());
		verify(handler, times(0)).showDialog(any(ExecutionEvent.class), anyString(), anyString());
	}

    private void thenInvalidStateShouldBeShown(String status) {
        verify(handler, times(1)).showLogs(any(), any());
        verify(handler, times(1)).showDialog(any(ExecutionEvent.class), anyString(), eq(NLS.bind(PodLogsHandler.INVALID_POD_STATUS_MESSAGE,  status)));
    }

    public static class TestPodLogsHandler extends PodLogsHandler {

        @Override
        public <T> T getSelectedElement(ExecutionEvent event, Class<T> klass) {
            return super.getSelectedElement(event, klass);
        }

        @Override
        protected void showLogs(IPod pod, ExecutionEvent event) {
            super.showLogs(pod, event);
        }

        @Override
        protected void showDialog(ExecutionEvent event, String title, String message) {
        }
    }

}
