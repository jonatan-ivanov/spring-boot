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

import org.springframework.aop.observability.TracingMethodInvocationProcessor;
import org.springframework.aop.observability.TracingNewSpanParser;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.observability.tracing.annotation.DefaultSpanCreator;
import org.springframework.boot.autoconfigure.observability.tracing.annotation.NonReactorObservabilityTracingMethodInvocationProcessor;
import org.springframework.boot.autoconfigure.observability.tracing.annotation.ObservabilityTracingAdvisorConfig;
import org.springframework.boot.autoconfigure.observability.tracing.annotation.ReactorObservabilityTracingMethodInvocationProcessor;
import org.springframework.boot.autoconfigure.observability.tracing.annotation.SpelTagValueExpressionResolver;
import org.springframework.core.observability.tracing.annotation.NewSpan;
import org.springframework.core.observability.tracing.annotation.NoOpTagValueResolver;
import org.springframework.core.observability.tracing.annotation.TagValueExpressionResolver;
import org.springframework.core.observability.tracing.annotation.TagValueResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} that allows creating spans by means of a {@link NewSpan}
 * annotation. You can annotate classes or just methods. You can also apply this
 * annotation to an interface.
 *
 * @author Christian Schwerdtfeger
 * @author Marcin Grzejszczak
 * @since 1.2.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(name = { "spring.observability.tracing.enabled", "spring.observability.tracing.annotation.enabled" }, matchIfMissing = true)
public class ObservabilityTracingAnnotationConfiguration {

	@Bean
	@ConditionalOnMissingBean
	TracingNewSpanParser newSpanParser() {
		return new DefaultSpanCreator();
	}

	@Bean
	@ConditionalOnMissingBean
	TagValueExpressionResolver spelTagValueExpressionResolver() {
		return new SpelTagValueExpressionResolver();
	}

	@Bean
	@ConditionalOnMissingBean
	TagValueResolver noOpTagValueResolver() {
		return new NoOpTagValueResolver();
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	ObservabilityTracingAdvisorConfig observabilityTracingAdvisorConfig() {
		return new ObservabilityTracingAdvisorConfig();
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@ConditionalOnClass(name = "reactor.core.publisher.Flux")
	TracingMethodInvocationProcessor reactorObservabilityTracingMethodInvocationProcessor() {
		return new ReactorObservabilityTracingMethodInvocationProcessor();
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@ConditionalOnMissingClass("reactor.core.publisher.Flux")
	TracingMethodInvocationProcessor nonReactorObservabilityTracingMethodInvocationProcessor() {
		return new NonReactorObservabilityTracingMethodInvocationProcessor();
	}

}
