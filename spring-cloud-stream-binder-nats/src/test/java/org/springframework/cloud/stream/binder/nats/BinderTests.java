/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.stream.binder.nats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.nats.NatsAutoConfiguration;
import org.springframework.boot.autoconfigure.nats.NatsProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;

public class BinderTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NatsAutoConfiguration.class));

    @Test
    public void testMessageProducer() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues("spring.nats.server:" + ts.getURI()).run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());

                NatsProperties props = new NatsProperties();

                props.setServer(ts.getURI());

                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsChannelBinder binder = config.natsBinder(provisioner, props);

                String theMessage = "hello world";
                String in = "in";

                ConsumerProperties cprops = new ConsumerProperties();
                ConsumerDestination from = provisioner.provisionConsumerDestination(in, "", cprops);
                NatsMessageProducer producer = (NatsMessageProducer) binder.createConsumerEndpoint(from, "", cprops);

                CompletableFuture<String> received = new CompletableFuture<>();
                DirectChannel output = new DirectChannel();
                output.subscribe(msg -> {
                    Object payload = msg.getPayload();

                    if (payload instanceof byte[]) {
                        received.complete(new String((byte[]) payload, StandardCharsets.UTF_8));
                    } else {
                        received.complete(payload.toString());
                    }
                });
                producer.setOutputChannel(output);

                assertTrue(producer.getOutputChannel() == output);

                assertFalse(producer.isRunning());
                producer.start();
                assertTrue(producer.isRunning());

                conn.publish(in, theMessage.getBytes(StandardCharsets.UTF_8));
                conn.flush(Duration.ofSeconds(5));

                String result = received.get(5, TimeUnit.SECONDS);

                assertEquals(theMessage, result);

                producer.stop();
            });
        }
    }

    @Test
    public void testMessageSource() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues("spring.nats.server:" + ts.getURI()).run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());

                NatsProperties props = new NatsProperties();

                props.setServer(ts.getURI());

                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsChannelBinder binder = config.natsBinder(provisioner, props);

                String theMessage = "hello world";
                String in = "in";

                ConsumerProperties cprops = new ConsumerProperties();
                NatsConsumerDestination from = (NatsConsumerDestination) provisioner.provisionConsumerDestination(in, "", cprops);
                NatsMessageSource src = new NatsMessageSource(from, binder.getConnection());

                assertFalse(src.isRunning());
                src.start();
                assertTrue(src.isRunning());
                
                CompletableFuture<String> received = new CompletableFuture<>();

                Thread t = new Thread(() -> {
                    org.springframework.messaging.Message<Object> msg = src.receive();
                    Object payload = msg.getPayload();

                    if (payload instanceof byte[]) {
                        received.complete(new String((byte[]) payload, StandardCharsets.UTF_8));
                    } else {
                        received.complete(payload.toString());
                    }
                });
                t.start();

                conn.publish(in, theMessage.getBytes(StandardCharsets.UTF_8));
                conn.flush(Duration.ofSeconds(5));

                String result = received.get(5, TimeUnit.SECONDS);

                assertEquals(theMessage, result);

                src.stop();
            });
        }
    }

    @Test
    public void testMessageHandler() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer()) {
            this.contextRunner.withPropertyValues("spring.nats.server:" + ts.getURI()).run((context) -> {
                Connection conn = (Connection) context.getBean(Connection.class);
                assertNotNull(conn);
                assertTrue("Connected Status", Connection.Status.CONNECTED == conn.getStatus());

                NatsProperties props = new NatsProperties();

                props.setServer(ts.getURI());

                NatsChannelBinderConfiguration config = new NatsChannelBinderConfiguration();
                NatsChannelProvisioner provisioner = config.natsChannelProvisioner();
                NatsChannelBinder binder = config.natsBinder(provisioner, props);

                String theMessage = "hello world";
                String out = "out";
                ProducerProperties pprops = new ProducerProperties();
                ProducerDestination to = provisioner.provisionProducerDestination(out, pprops);
                MessageHandler mh = binder.createProducerMessageHandler(to, pprops, null);

                Subscription sub = conn.subscribe(out);
                conn.flush(Duration.ofSeconds(5));

                // send with the binder code
                mh.handleMessage(new GenericMessage<byte[]>(theMessage.getBytes(StandardCharsets.UTF_8)));

                Message msg = sub.nextMessage(Duration.ofSeconds(5));
                String result = (msg != null) ? new String((byte[]) msg.getData(), StandardCharsets.UTF_8) : null;

                assertEquals(theMessage, result);
            });
        }
    }
}