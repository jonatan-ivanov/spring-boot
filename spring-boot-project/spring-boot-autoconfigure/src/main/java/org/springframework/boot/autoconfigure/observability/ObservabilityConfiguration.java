/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.observability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.observability.event.Recorder;
import org.springframework.core.observability.event.SimpleRecorder;
import org.springframework.core.observability.event.listener.RecordingListener;
import org.springframework.core.observability.event.listener.composite.AllMatchingCompositeRecordingListener;
import org.springframework.core.observability.event.listener.composite.CompositeRecordingListener;
import org.springframework.core.observability.event.listener.composite.FirstMatchingCompositeRecordingListener;
import org.springframework.core.observability.listener.metrics.MetricsRecordingListener;
import org.springframework.core.observability.listener.metrics.MicrometerRecordingListener;
import org.springframework.core.observability.listener.tracing.DefaultTracingRecordingListener;
import org.springframework.core.observability.listener.tracing.HttpClientTracingRecordingListener;
import org.springframework.core.observability.listener.tracing.HttpServerTracingRecordingListener;
import org.springframework.core.observability.listener.tracing.TracingRecordingListener;
import org.springframework.core.observability.time.Clock;
import org.springframework.core.observability.tracing.Tracer;
import org.springframework.core.observability.tracing.http.HttpClientHandler;
import org.springframework.core.observability.tracing.http.HttpServerHandler;

@Configuration(proxyBeanMethods = false)
class ObservabilityConfiguration {

	@Bean
	Recorder<?> simpleRecorder(CompositeRecordingListener compositeRecordingListener) {
		return new SimpleRecorder<>(compositeRecordingListener, Clock.SYSTEM);
	}

	@Bean
	CompositeRecordingListener compositeRecordingListener(List<RecordingListener<?>> listeners) {
		return new AllMatchingCompositeRecordingListener(listenersWithoutDuplicates(listeners));
	}

	private List<RecordingListener<?>> listenersWithoutDuplicates(List<RecordingListener<?>> listeners) {
		Set<RecordingListener<?>> recordingListeners = new HashSet<>();
		listeners.forEach(recordingListener -> {
			if (recordingListener instanceof CompositeRecordingListener) {
				List<? extends RecordingListener<?>> compositeListeners = ((CompositeRecordingListener) recordingListener).getListeners();
				compositeListeners.forEach(recordingListeners::remove);
			}
			recordingListeners.add(recordingListener);
		});
		return new ArrayList<>(recordingListeners);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(Tracer.class)
	static class TracingConfig {

		@Bean
		@Order
		DefaultTracingRecordingListener defaultTracingRecordingListener(Tracer tracer) {
			return new DefaultTracingRecordingListener(tracer);
		}

		@Bean
		@Order(value = Ordered.LOWEST_PRECEDENCE - 10)
		HttpClientTracingRecordingListener httpClientTracingRecordingListener(Tracer tracer, HttpClientHandler httpClientHandler) {
			return new HttpClientTracingRecordingListener(tracer, httpClientHandler);
		}

		@Bean
		@Order(value = Ordered.LOWEST_PRECEDENCE - 10)
		HttpServerTracingRecordingListener httpServerTracingRecordingListener(Tracer tracer, HttpServerHandler httpServerHandler) {
			return new HttpServerTracingRecordingListener(tracer, httpServerHandler);
		}

		@Bean
		FirstMatchingCompositeRecordingListener tracingFirstMatchingRecordingListeners(List<TracingRecordingListener> tracingRecordingListeners) {
			return new FirstMatchingCompositeRecordingListener(tracingRecordingListeners);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class MetricsConfig {

		@Bean
		FirstMatchingCompositeRecordingListener metricsFirstMatchingRecordingListeners(List<MetricsRecordingListener<?>> metricsRecordingListeners) {
			return new FirstMatchingCompositeRecordingListener(metricsRecordingListeners);
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(MeterRegistry.class)
		static class MicrometerConfig {

			@Bean
			@Order
			MicrometerRecordingListener micrometerRecordingListener(MeterRegistry meterRegistry) {
				return new MicrometerRecordingListener(meterRegistry);
			}
		}
	}

}

