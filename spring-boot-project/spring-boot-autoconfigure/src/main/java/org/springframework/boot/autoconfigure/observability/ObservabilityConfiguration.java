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

import io.micrometer.core.event.listener.RecordingListener;
import io.micrometer.core.event.listener.composite.AllMatchingCompositeRecordingListener;
import io.micrometer.core.event.listener.composite.CompositeRecordingListener;
import io.micrometer.core.event.listener.composite.FirstMatchingCompositeRecordingListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.listener.tracing.DefaultTracingRecordingListener;
import io.micrometer.core.instrument.listener.tracing.HttpClientTracingRecordingListener;
import io.micrometer.core.instrument.listener.tracing.HttpServerTracingRecordingListener;
import io.micrometer.core.instrument.listener.tracing.TracingRecordingListener;
import io.micrometer.core.instrument.tracing.Tracer;
import io.micrometer.core.instrument.tracing.http.HttpClientHandler;
import io.micrometer.core.instrument.tracing.http.HttpServerHandler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
class ObservabilityConfiguration {


	@Bean
	BeanPostProcessor listenerProvidingMeterRegistryCustomizer(BeanFactory beanFactory) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof MeterRegistry) {
					((MeterRegistry) bean).config().recordingListener(beanFactory.getBean(CompositeRecordingListener.class));
				}
				return bean;
			}
		};
	}


	@Bean
	@Primary
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

}

