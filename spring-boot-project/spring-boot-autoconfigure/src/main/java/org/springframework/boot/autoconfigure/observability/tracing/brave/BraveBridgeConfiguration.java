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

import java.util.ArrayList;
import java.util.List;

import brave.Tracing;
import brave.handler.SpanHandler;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.observability.tracing.ObservabilityTracingBaggageProperties;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.BraveBaggageManager;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.BraveCurrentTraceContext;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.BravePropagator;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.BraveSpanCustomizer;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.BraveTracer;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.CompositePropagationFactorySupplier;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.CompositeSpanHandler;
import org.springframework.boot.autoconfigure.observability.tracing.brave.propagation.PropagationFactorySupplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.observability.tracing.CurrentTraceContext;
import org.springframework.core.observability.tracing.SpanCustomizer;
import org.springframework.core.observability.tracing.exporter.SpanFilter;
import org.springframework.core.observability.tracing.exporter.SpanReporter;
import org.springframework.core.observability.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.observability.tracing.Tracer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ObservabilityTracingPropagationProperties.class)
class BraveBridgeConfiguration {

	@Bean
	BraveBaggageManager braveBaggageManager() {
		return new BraveBaggageManager();
	}

	@Bean
	Tracer braveTracer(brave.Tracer tracer, brave.propagation.CurrentTraceContext currentTraceContext, BraveBaggageManager braveBaggageManager) {
		return new BraveTracer(tracer, currentTraceContext, braveBaggageManager);
	}

	@Bean
	CurrentTraceContext braveCurrentTraceContext(
			brave.propagation.CurrentTraceContext currentTraceContext) {
		return new BraveCurrentTraceContext(currentTraceContext);
	}

	@Bean
	SpanCustomizer braveSpanCustomizer(brave.SpanCustomizer spanCustomizer) {
		return new BraveSpanCustomizer(spanCustomizer);
	}

	@Bean
	Propagator bravePropagator(Tracing tracing) {
		return new BravePropagator(tracing);
	}

	@Bean
	@ConditionalOnMissingBean
	PropagationFactorySupplier compositePropagationFactorySupplier(BeanFactory beanFactory,
			ObservabilityTracingBaggageProperties baggageProperties, ObservabilityTracingPropagationProperties properties) {
		return new CompositePropagationFactorySupplier(beanFactory, baggageProperties.getLocalFields(),
				properties.getType());
	}

	// Name is important for sampling conditions
	@Bean(name = "traceCompositeSpanHandler")
	SpanHandler compositeSpanHandler(ObjectProvider<List<SpanFilter>> exporters,
			ObjectProvider<List<SpanReporter>> reporters) {
		return new CompositeSpanHandler(exporters.getIfAvailable(ArrayList::new),
				reporters.getIfAvailable(ArrayList::new));
	}

/*	@Bean
	@ConditionalOnClass(name = "reactor.util.context.Context")
	static BraveReactorContextBeanDefinitionRegistryPostProcessor braveReactorContextBeanDefinitionRegistryPostProcessor() {
		return new BraveReactorContextBeanDefinitionRegistryPostProcessor();
	}

	static class BraveReactorContextBeanDefinitionRegistryPostProcessor
			implements BeanDefinitionRegistryPostProcessor, Closeable {

		private static final Log log = LogFactory.getLog(BraveReactorContextBeanDefinitionRegistryPostProcessor.class);

		@Override
		public void close() throws IOException {
			ReactorSleuth.contextWrappingFunction = Function.identity();
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			ReactorSleuth.contextWrappingFunction = new BraveContextWrappingFunction();
			if (log.isDebugEnabled()) {
				log.debug("Wrapped Reactor's context into a Brave representation");
			}
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		}

	}*/

}
