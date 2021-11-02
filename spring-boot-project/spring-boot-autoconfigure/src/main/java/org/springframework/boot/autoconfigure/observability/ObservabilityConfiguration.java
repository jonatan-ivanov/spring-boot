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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimerRecordingListener;
import io.micrometer.core.instrument.tracing.Tracer;
import io.micrometer.core.instrument.tracing.http.HttpClientHandler;
import io.micrometer.core.instrument.tracing.http.HttpServerHandler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.observability.tracing.listener.DefaultTracingRecordingListener;
import org.springframework.boot.autoconfigure.observability.tracing.listener.HttpClientTracingRecordingListener;
import org.springframework.boot.autoconfigure.observability.tracing.listener.HttpServerTracingRecordingListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
@SuppressWarnings({ "rawtypes", "unchecked" })
class ObservabilityConfiguration {

	@Bean
	BeanPostProcessor listenerProvidingMeterRegistryCustomizer(ListableBeanFactory beanFactory) {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName)
					throws BeansException {
				if (bean instanceof MeterRegistry) {
					beanFactory.getBeansOfType(TimerRecordingListener.class).values().stream()
							.forEach(listener -> ((MeterRegistry) bean).config().timerRecordingListener(listener));
				}
				return bean;
			}
		};
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
		HttpClientTracingRecordingListener httpClientTracingRecordingListener(
				Tracer tracer, HttpClientHandler httpClientHandler) {
			return new HttpClientTracingRecordingListener(tracer, httpClientHandler);
		}

		@Bean
		@Order(value = Ordered.LOWEST_PRECEDENCE - 10)
		HttpServerTracingRecordingListener httpServerTracingRecordingListener(
				Tracer tracer, HttpServerHandler httpServerHandler) {
			return new HttpServerTracingRecordingListener(tracer, httpServerHandler);
		}
	}

}
