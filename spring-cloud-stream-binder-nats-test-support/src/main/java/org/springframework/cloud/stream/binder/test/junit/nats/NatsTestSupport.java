/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.stream.binder.test.junit.nats;

import org.springframework.cloud.stream.test.junit.AbstractExternalResourceTestSupport;

/**
 * JUnit {@link org.junit.Rule} ensure that NATS is available on
 * localhost.
 *
 * @author The NATS Authors
 */
public class NatsTestSupport
		extends AbstractExternalResourceTestSupport {

	private final boolean management;

	public NatsTestSupport() {
		this(false);
	}

	public NatsTestSupport(boolean management) {
		super("NATS");
		this.management = management;
	}

	//
	// TODO:  Add code to start/stop the NATS server.
	//

	@Override
	protected void obtainResource() throws Exception {
		// Launc the nats server?
	}

	@Override
	protected void cleanupResource() throws Exception {
		// kill the NATS server?
	}
}