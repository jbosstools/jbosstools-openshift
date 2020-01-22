/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

public interface URL {
  enum State {
    NOT_PUSHED("Not Pushed"),
    PUSHED("Pushed"),
    LOCALLY_DELETED("Locally Deleted");

    private String label;

    private State(String label) {
      this.label = label;
    }

    public static State from(String value) {
      for(State state : State.values()) {
        if (state.label.equals(value)) {
          return state;
        }
      }
      return NOT_PUSHED;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  String getName();

  String getProtocol();

  String getHost();

  String getPort();

  State getState();

  static URL of(String name, String protocol, String host, String port, String state) {
    return new URL() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getHost() {
        return host;
      }

      @Override
      public String getProtocol() {
        return protocol;
      }

      @Override
      public String getPort() {
        return port;
      }

      @Override
      public State getState() {
        return State.from(state);
      }
    };
  }
}
