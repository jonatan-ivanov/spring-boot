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

import brave.Tracing;
import brave.handler.SpanHandler;
import brave.http.HttpTracing;
import brave.sampler.Sampler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.observability.bridge.brave.bridge.BraveBaggageManager;
import org.springframework.boot.autoconfigure.observability.bridge.brave.bridge.BraveCurrentTraceContext;
import org.springframework.boot.autoconfigure.observability.bridge.brave.bridge.BraveHttpClientHandler;
import org.springframework.boot.autoconfigure.observability.bridge.brave.bridge.BraveHttpServerHandler;
import org.springframework.boot.autoconfigure.observability.bridge.brave.bridge.BraveTracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.observability.tracing.CurrentTraceContext;
import org.springframework.core.observability.tracing.Tracer;
import org.springframework.core.observability.tracing.http.HttpClientHandler;
import org.springframework.core.observability.tracing.http.HttpServerHandler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Tracing.class)
class BraveConfiguration {

	@Bean
	Sampler braveSampler() {
		return Sampler.ALWAYS_SAMPLE;
	}

	@Bean
	Tracing braveTracing(ObjectProvider<SpanHandler> spanHandlers, Sampler sampler) {
		Tracing.Builder builder = Tracing.newBuilder();
		spanHandlers.forEach(builder::addSpanHandler);
		return builder
				.sampler(sampler)
				.build();
	}

	@Bean
	BraveBaggageManager braveBaggageManager() {
		return new BraveBaggageManager();
	}

	@Bean
	brave.Tracer braveTracer(Tracing tracing) {
		return tracing.tracer();
	}

	@Bean
	Tracer braveBridgeTracer(Tracing tracing, BraveBaggageManager baggageManager) {
		return new BraveTracer(tracing.tracer(), tracing.currentTraceContext(), baggageManager);
	}

	@Bean
	CurrentTraceContext currentTraceContext(Tracing tracing) {
		return new BraveCurrentTraceContext(tracing.currentTraceContext());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HttpTracing.class)
	static class HttpConfig {

		@Bean
		HttpTracing httpTracing(Tracing tracing) {
			return HttpTracing.newBuilder(tracing)
					.clientRequestParser((request, context, span) -> {

					})
					.clientResponseParser((request, context, span) -> {

					})
					.serverRequestParser((request, context, span) -> {

					})
					.serverResponseParser((request, context, span) -> {

					})
					.build();
		}

		@Bean
		HttpClientHandler traceHttpClientHandler(HttpTracing httpTracing) {
			return new BraveHttpClientHandler(brave.http.HttpClientHandler.create(httpTracing));
		}

		@Bean
		HttpServerHandler traceHttpServerHandler(HttpTracing httpTracing) {
			return new BraveHttpServerHandler(brave.http.HttpServerHandler.create(httpTracing));
		}
	}

}
