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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.stream.Collectors;

import javax.servlet.DispatcherType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.beans.factory.ObjectProvider;
//import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
//import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.observability.DefaultWebMvcTagsProvider;
import org.springframework.web.servlet.mvc.observability.HandlerParser;
import org.springframework.web.servlet.mvc.observability.RecordingCustomizingHandlerInterceptor;
import org.springframework.web.servlet.mvc.observability.WebMvcObservabilityFilter;
import org.springframework.web.servlet.mvc.observability.WebMvcTagsContributor;
import org.springframework.web.servlet.mvc.observability.WebMvcTagsProvider;

@Configuration(proxyBeanMethods = false)
class ObservabilityServletInstrumentationConfiguration {

//	private final MetricsProperties properties;

//	public ObservabilityServletInstrumentationConfiguration(MetricsProperties properties) {
//		this.properties = properties;
//	}

	@Bean
	@ConditionalOnMissingBean(WebMvcTagsProvider.class)
	public DefaultWebMvcTagsProvider observabilityWebMvcTagsProvider(ObjectProvider<WebMvcTagsContributor> contributors) {
		return new DefaultWebMvcTagsProvider(true,
				contributors.orderedStream().collect(Collectors.toList()));
	}

	@Bean
	public FilterRegistrationBean<WebMvcObservabilityFilter> observabilityWebMvcMetricsFilter(MeterRegistry registry,
			WebMvcTagsProvider tagsProvider) {
		String metricName = "http.server.requests";
		WebMvcObservabilityFilter filter = new WebMvcObservabilityFilter(registry, tagsProvider, metricName);
		FilterRegistrationBean<WebMvcObservabilityFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		// TODO: Verify how we set this in Sleuth
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

	/*@Bean
	@Order(0)
	public MeterFilter metricsHttpServerUriTagFilter() {
		String metricName = this.properties.getWeb().getServer().getRequest().getMetricName();
		MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
				() -> String.format("Reached the maximum number of URI tags for '%s'.", metricName));
		// http.uri must be pushed to a constant
		return MeterFilter.maximumAllowableTags(metricName, "http.uri", this.properties.getWeb().getServer().getMaxUriTags(),
				filter);
	}*/

	@Bean
	HandlerParser observabilityHandlerParser() {
		return new HandlerParser();
	}

	@Bean
	RecordingCustomizingHandlerInterceptor observabilityRecordingCustomizingHandlerInterceptor(HandlerParser handlerParser) {
		return new RecordingCustomizingHandlerInterceptor(handlerParser);
	}

	@Bean
	ObservabilityWebMvcConfigurer observabilityWebMvcConfigurer(RecordingCustomizingHandlerInterceptor interceptor) {
		return new ObservabilityWebMvcConfigurer(interceptor);
	}

//
//	@Bean
//	RecordingCustomizingAsyncHandlerInterceptor recordingCustomizingAsyncHandlerInterceptor(HandlerParser handlerParser) {
//		return new RecordingCustomizingAsyncHandlerInterceptor(handlerParser);
//	}

	/**
	 * {@link WebMvcConfigurer} to add metrics interceptors.
	 */
	static class ObservabilityWebMvcConfigurer implements WebMvcConfigurer {

		private final RecordingCustomizingHandlerInterceptor interceptor;

		ObservabilityWebMvcConfigurer(RecordingCustomizingHandlerInterceptor interceptor) {
			this.interceptor = interceptor;
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(this.interceptor);
		}

	}
}
