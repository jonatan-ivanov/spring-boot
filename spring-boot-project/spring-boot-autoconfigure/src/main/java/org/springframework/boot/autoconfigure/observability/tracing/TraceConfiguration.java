/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.observability.tracing;

import io.micrometer.core.instrument.tracing.SpanNamer;
import io.micrometer.core.instrument.tracing.exporter.SpanFilter;
import io.micrometer.core.instrument.tracing.exporter.SpanIgnoringSpanFilter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.observability.tracing.internal.DefaultSpanNamer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to enable tracing via Spring Cloud Sleuth.
 *
 * @author Spencer Gibb
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.observability.tracing.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ ObservabilityTracingSpanFilterProperties.class, ObservabilityTracingBaggageProperties.class,
		ObservabilityTracingTracerProperties.class })
public class TraceConfiguration {

	@Bean
	@ConditionalOnMissingBean
	SpanNamer defaultSpanNamer() {
		return new DefaultSpanNamer();
	}

	@Bean
	@ConditionalOnProperty(value = "spring.observability.tracing.span-filter.enabled", matchIfMissing = true)
	SpanFilter spanIgnoringSpanExporter(ObservabilityTracingSpanFilterProperties observabilityTracingSpanFilterProperties) {
		return new SpanIgnoringSpanFilter(observabilityTracingSpanFilterProperties.getSpanNamePatternsToSkip(),
				observabilityTracingSpanFilterProperties.getAdditionalSpanNamePatternsToIgnore());
	}

}
