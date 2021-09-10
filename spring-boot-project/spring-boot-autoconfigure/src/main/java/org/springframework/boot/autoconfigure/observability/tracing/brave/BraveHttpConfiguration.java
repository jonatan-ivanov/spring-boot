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

import java.util.List;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import brave.Tracer;
import brave.Tracing;
import brave.http.HttpRequest;
import brave.http.HttpTracing;
import brave.http.HttpTracingCustomizer;
import brave.propagation.CurrentTraceContext;
import brave.sampler.SamplerFunction;
import brave.sampler.SamplerFunctions;
import reactor.util.context.Context;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.observability.tracing.ObservabilityTracingWebProperties;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.BraveHttpRequestParser;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.BraveHttpResponseParser;
import org.springframework.boot.autoconfigure.observability.tracing.brave.bridge.BraveSamplerFunction;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.http.HttpClientRequestParser;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.http.HttpClientResponseParser;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.http.HttpClientSampler;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.http.HttpServerRequestParser;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.http.HttpServerResponseParser;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.http.HttpServerSampler;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.http.SkipPatternProvider;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.http.SpanFromContextRetriever;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.web.BraveSpanFromContextRetriever;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.web.CompositeHttpSampler;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.web.SkipPatternHttpClientSampler;
import org.springframework.boot.autoconfigure.observability.tracing.instrumentation.web.SkipPatternHttpServerSampler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import io.micrometer.core.instrument.tracing.http.HttpRequestParser;
import io.micrometer.core.instrument.tracing.http.HttpResponseParser;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} related to HTTP based communication.
 *
 * @author Marcin Grzejszczak
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HttpTracing.class)
@EnableConfigurationProperties(ObservabilityTracingWebProperties.class)
@Import(BraveHttpBridgeConfiguration.class)
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class BraveHttpConfiguration {

	@Bean(HttpClientRequestParser.NAME)
	@ConditionalOnMissingBean(name = HttpClientRequestParser.NAME)
	HttpRequestParser defaultHttpClientRequestParser() {
		return (httpRequest, traceContext, spanCustomizer) -> {

		};
	}

	@Bean(HttpClientResponseParser.NAME)
	@ConditionalOnMissingBean(name = HttpClientResponseParser.NAME)
	HttpResponseParser defaultHttpClientResponseParser() {
		return (httpRequest, traceContext, spanCustomizer) -> {

		};
	}

	@Bean(HttpServerRequestParser.NAME)
	@ConditionalOnMissingBean(name = HttpServerRequestParser.NAME)
	HttpRequestParser defaultHttpServerRequestParser() {
		return (httpRequest, traceContext, spanCustomizer) -> {

		};
	}

	@Bean(HttpServerResponseParser.NAME)
	@ConditionalOnMissingBean(name = HttpServerResponseParser.NAME)
	HttpResponseParser defaultHttpServerResponseParser() {
		return (httpRequest, traceContext, spanCustomizer) -> {

		};
	}

	@Bean
	@ConditionalOnMissingBean
	// NOTE: stable bean name as might be used outside sleuth
	HttpTracing httpTracing(Tracing tracing, @Nullable SkipPatternProvider provider,
			@Nullable brave.http.HttpClientParser clientParser, @Nullable brave.http.HttpServerParser serverParser,
			BeanFactory beanFactory, @Nullable List<HttpTracingCustomizer> httpTracingCustomizers) {
		HttpTracing.Builder builder = httpTracingBuilder(tracing, provider, beanFactory);
		brave.http.HttpRequestParser httpClientRequestParser = httpRequestParser(beanFactory,
				HttpClientRequestParser.NAME);
		brave.http.HttpResponseParser httpClientResponseParser = httpResponseParser(beanFactory,
				HttpClientResponseParser.NAME);
		brave.http.HttpRequestParser httpServerRequestParser = httpRequestParser(beanFactory,
				HttpServerRequestParser.NAME);
		brave.http.HttpResponseParser httpServerResponseParser = httpResponseParser(beanFactory,
				HttpServerResponseParser.NAME);

		if (httpClientRequestParser != null || httpClientResponseParser != null) {
			if (httpClientRequestParser != null) {
				builder.clientRequestParser(httpClientRequestParser);
			}
			if (httpClientResponseParser != null) {
				builder.clientResponseParser(httpClientResponseParser);
			}
		}
		else if (clientParser != null) { // consider deprecated last
			builder.clientParser(clientParser);
		}

		if (httpServerRequestParser != null || httpServerResponseParser != null) {
			if (httpServerRequestParser != null) {
				builder.serverRequestParser(httpServerRequestParser);
			}
			if (httpServerResponseParser != null) {
				builder.serverResponseParser(httpServerResponseParser);
			}
		}
		else if (serverParser != null) { // consider deprecated last
			builder.serverParser(serverParser);
		}

		if (httpTracingCustomizers != null) {
			for (HttpTracingCustomizer customizer : httpTracingCustomizers) {
				customizer.customize(builder);
			}
		}
		return builder.build();
	}

	private brave.http.HttpRequestParser httpRequestParser(BeanFactory beanFactory, String name) {
		return beanFactory.containsBean(name) ? toBraveHttpRequestParser(beanFactory, name) : null;
	}

	private brave.http.HttpResponseParser httpResponseParser(BeanFactory beanFactory, String name) {
		return beanFactory.containsBean(name) ? toBraveHttpResponseParser(beanFactory, name) : null;
	}

	@NotNull
	private HttpTracing.Builder httpTracingBuilder(Tracing tracing, @Nullable SkipPatternProvider provider,
			BeanFactory beanFactory) {
		SamplerFunction<HttpRequest> httpClientSampler = toBraveSampler(beanFactory, HttpClientSampler.NAME);
		SamplerFunction<HttpRequest> httpServerSampler = httpServerSampler(beanFactory);
		SamplerFunction<HttpRequest> combinedSampler = combineUserProvidedSamplerWithSkipPatternSampler(
				httpServerSampler, provider);
		return HttpTracing.newBuilder(tracing).clientSampler(httpClientSampler).serverSampler(combinedSampler);
	}

	@Nullable
	private SamplerFunction<HttpRequest> httpServerSampler(BeanFactory beanFactory) {
		return beanFactory.containsBean(HttpServerSampler.NAME) ? toBraveSampler(beanFactory, HttpServerSampler.NAME)
				: null;
	}

	private brave.http.HttpRequestParser toBraveHttpRequestParser(BeanFactory beanFactory, String beanName) {
		Object bean = beanFactory.getBean(beanName);
		brave.http.HttpRequestParser parser = bean instanceof brave.http.HttpRequestParser
				? (brave.http.HttpRequestParser) bean
				: bean instanceof HttpRequestParser ? BraveHttpRequestParser.toBrave((HttpRequestParser) bean) : null;
		return returnOrThrow(bean, parser, beanName, brave.http.HttpRequestParser.class, HttpRequestParser.class);
	}

	private brave.http.HttpResponseParser toBraveHttpResponseParser(BeanFactory beanFactory, String beanName) {
		Object bean = beanFactory.getBean(beanName);
		brave.http.HttpResponseParser parser = bean instanceof brave.http.HttpResponseParser
				? (brave.http.HttpResponseParser) bean : bean instanceof HttpResponseParser
						? BraveHttpResponseParser.toBrave((HttpResponseParser) bean) : null;
		return returnOrThrow(bean, parser, beanName, brave.http.HttpResponseParser.class, HttpResponseParser.class);
	}

	private SamplerFunction<HttpRequest> toBraveSampler(BeanFactory beanFactory, String beanName) {
		Object bean = beanFactory.getBean(beanName);
		SamplerFunction<HttpRequest> braveSampler = bean instanceof SamplerFunction
				? (SamplerFunction<HttpRequest>) bean
				: bean instanceof io.micrometer.core.instrument.tracing.SamplerFunction ? BraveSamplerFunction.toHttpBrave(
						(io.micrometer.core.instrument.tracing.SamplerFunction<io.micrometer.core.instrument.transport.http.HttpRequest>) bean)
						: null;
		return returnOrThrow(bean, braveSampler, beanName, SamplerFunction.class,
				io.micrometer.core.instrument.tracing.SamplerFunction.class);
	}

	@NotNull
	private <T> T returnOrThrow(Object bean, T convertedBean, String name, Class brave, Class sleuth) {
		if (convertedBean == null) {
			throw new IllegalStateException(
					"Bean with name [" + name + "] is of type [" + bean.getClass() + "] and only ["
							+ brave.getCanonicalName() + "] and [" + sleuth.getCanonicalName() + "] are supported");
		}
		return convertedBean;
	}

	private SamplerFunction<HttpRequest> combineUserProvidedSamplerWithSkipPatternSampler(
			@Nullable SamplerFunction<HttpRequest> serverSampler, @Nullable SkipPatternProvider provider) {
		SamplerFunction<HttpRequest> skipPatternSampler = provider != null
				? new SkipPatternHttpServerSampler(provider) : null;
		if (serverSampler == null && skipPatternSampler == null) {
			return SamplerFunctions.deferDecision();
		}
		else if (serverSampler == null) {
			return skipPatternSampler;
		}
		else if (skipPatternSampler == null) {
			return serverSampler;
		}
		return new CompositeHttpSampler(skipPatternSampler, serverSampler);
	}

	@Bean
	@ConditionalOnMissingBean(name = HttpClientSampler.NAME)
	SamplerFunction<HttpRequest> sleuthHttpClientSampler(ObservabilityTracingWebProperties observabilityTracingWebProperties) {
		String skipPattern = observabilityTracingWebProperties.getClient().getSkipPattern();
		if (skipPattern == null) {
			return SamplerFunctions.deferDecision();
		}

		return new SkipPatternHttpClientSampler(Pattern.compile(skipPattern));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Context.class)
	static class BraveWebFilterConfiguration {

		@Bean
		SpanFromContextRetriever braveSpanFromContextRetriever(CurrentTraceContext currentTraceContext, Tracer tracer) {
			return new BraveSpanFromContextRetriever(currentTraceContext, tracer);
		}

	}

}
