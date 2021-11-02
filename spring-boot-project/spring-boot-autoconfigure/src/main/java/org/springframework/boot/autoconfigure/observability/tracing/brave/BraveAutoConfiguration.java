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

package org.springframework.boot.autoconfigure.observability.tracing.brave;

import java.util.Collections;
import java.util.List;

import brave.CurrentSpanCustomizer;
import brave.Tracer;
import brave.Tracing;
import brave.TracingCustomizer;
import brave.handler.SpanHandler;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContextCustomizer;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.observability.ObservabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.observability.tracing.ObservabilityTracingBaggageProperties;
import org.springframework.boot.autoconfigure.observability.tracing.ObservabilityTracingSpanFilterProperties;
import org.springframework.boot.autoconfigure.observability.tracing.ObservabilityTracingTracerProperties;
import org.springframework.boot.autoconfigure.observability.tracing.TraceConfiguration;
import org.springframework.boot.autoconfigure.observability.tracing.internal.DefaultSpanNamer;
import org.springframework.boot.autoconfigure.observability.tracing.internal.ObservabilityTracingContextListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import io.micrometer.core.instrument.tracing.SpanNamer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to enable tracing via Spring Cloud Sleuth with Brave.
 *
 * @author Spencer Gibb
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @since 3.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBraveEnabled
@ConditionalOnProperty(value = "spring.observability.tracing.enabled", matchIfMissing = true)
@AutoConfigureBefore(ObservabilityAutoConfiguration.class)
@ConditionalOnMissingBean(Tracer.class)
@ConditionalOnClass({ Tracer.class, ObservabilityTracingProperties.class })
@EnableConfigurationProperties({ ObservabilityTracingProperties.class, ObservabilityTracingSpanFilterProperties.class,
		ObservabilityTracingBaggageProperties.class, ObservabilityTracingTracerProperties.class, ObservabilityTracingBaggageProperties.class })
@Import({ BraveBridgeConfiguration.class, BraveBaggageConfiguration.class, BraveSamplerConfiguration.class,
		BraveHttpConfiguration.class, TraceConfiguration.class })
public class BraveAutoConfiguration {

	/**
	 * Tracing bean name. Name of the bean matters for some instrumentations.
	 */
	public static final String TRACING_BEAN_NAME = "tracing";

	/**
	 * Tracer bean name. Name of the bean matters for some instrumentations.
	 */
	public static final String TRACER_BEAN_NAME = "tracer";

	/**
	 * Default value used for service name if none provided.
	 */
	private static final String DEFAULT_SERVICE_NAME = "default";

	@Bean(name = TRACING_BEAN_NAME)
	@ConditionalOnMissingBean
	// NOTE: stable bean name as might be used outside sleuth
	Tracing tracing(@LocalServiceName String serviceName, Propagation.Factory factory,
			CurrentTraceContext currentTraceContext, Sampler sampler, ObservabilityTracingProperties observabilityTracingProperties,
			@Nullable List<SpanHandler> spanHandlers, @Nullable List<TracingCustomizer> tracingCustomizers) {
		Tracing.Builder builder = Tracing.newBuilder().sampler(sampler)
				.localServiceName(!StringUtils.hasText(serviceName) ? DEFAULT_SERVICE_NAME : serviceName)
				.propagationFactory(factory).currentTraceContext(currentTraceContext)
				.traceId128Bit(observabilityTracingProperties.isTraceId128()).supportsJoin(observabilityTracingProperties.isSupportsJoin());
		if (spanHandlers != null) {
			for (SpanHandler spanHandlerFactory : spanHandlers) {
				builder.addSpanHandler(spanHandlerFactory);
			}
		}
		if (tracingCustomizers != null) {
			for (TracingCustomizer customizer : tracingCustomizers) {
				customizer.customize(builder);
			}
		}

		return builder.build();
	}

	@Bean(name = TRACER_BEAN_NAME)
	@ConditionalOnMissingBean
	Tracer tracer(Tracing tracing) {
		return tracing.tracer();
	}

	@Bean
	@ConditionalOnMissingBean
	SpanNamer sleuthSpanNamer() {
		return new DefaultSpanNamer();
	}

	@Bean
	CurrentTraceContext sleuthCurrentTraceContext(CurrentTraceContext.Builder builder,
			@Nullable List<CurrentTraceContext.ScopeDecorator> scopeDecorators,
			@Nullable List<CurrentTraceContextCustomizer> currentTraceContextCustomizers) {
		if (scopeDecorators == null) {
			scopeDecorators = Collections.emptyList();
		}
		if (currentTraceContextCustomizers == null) {
			currentTraceContextCustomizers = Collections.emptyList();
		}

		for (CurrentTraceContext.ScopeDecorator scopeDecorator : scopeDecorators) {
			builder.addScopeDecorator(scopeDecorator);
		}
		for (CurrentTraceContextCustomizer customizer : currentTraceContextCustomizers) {
			customizer.customize(builder);
		}
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	CurrentTraceContext.Builder sleuthCurrentTraceContextBuilder() {
		return ThreadLocalCurrentTraceContext.newBuilder();
	}

	@Bean
	@ConditionalOnMissingBean
	// NOTE: stable bean name as might be used outside sleuth
	CurrentSpanCustomizer spanCustomizer(Tracing tracing) {
		return CurrentSpanCustomizer.create(tracing);
	}

	@Bean
	ObservabilityTracingContextListener sleuthContextListener() {
		return new ObservabilityTracingContextListener();
	}

}
