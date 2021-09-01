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

package org.springframework.boot.autoconfigure.observability.tracing.reporter.wavefront;

import brave.Tracer;
import brave.TracingCustomizer;
import brave.handler.SpanHandler;
import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;
import com.wavefront.spring.autoconfigure.WavefrontProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.wavefront.WavefrontConfig;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.observability.ObservabilityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ MeterRegistry.class, WavefrontConfig.class, WavefrontSender.class })
@AutoConfigureBefore(ObservabilityAutoConfiguration.class)
public class ObservabilityWavefrontAutoConfiguration {

	static final String BEAN_NAME = "observabilityWavefrontTracingCustomizer";

	@Bean
	@ConditionalOnBean({ MeterRegistry.class, WavefrontConfig.class, WavefrontSender.class })
	WavefrontSpringObservabilitySpanHandler wavefrontSpringObservabilitySpanHandler(MeterRegistry meterRegistry,
			WavefrontSender wavefrontSender,
			ApplicationTags applicationTags,
			WavefrontConfig wavefrontConfig,
			WavefrontProperties wavefrontProperties) {
		return new WavefrontSpringObservabilitySpanHandler(
				// https://github.com/wavefrontHQ/wavefront-opentracing-sdk-java/blob/f1f08d8daf7b692b9b61dcd5bc24ca6befa8e710/src/main/java/com/wavefront/opentracing/reporting/WavefrontSpanReporter.java#L54
				50000, // TODO: maxQueueSize should be a property, ya?
				wavefrontSender,
				meterRegistry,
				wavefrontConfig.source(),
				applicationTags,
				wavefrontProperties.getTracing().getRedMetricsCustomTagKeys());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({Tracer.class, TracingCustomizer.class, SpanHandler.class })
	static class WavefrontBraveCustomizerConfiguration {
		@Bean(BEAN_NAME)
		@ConditionalOnMissingBean(name = BEAN_NAME)
		@ConditionalOnBean({ MeterRegistry.class, WavefrontConfig.class, WavefrontSender.class })
		TracingCustomizer wavefrontTracingCustomizer(WavefrontSpringObservabilitySpanHandler spanHandler) {
			return t -> t.traceId128Bit(true).supportsJoin(false).addSpanHandler(new WavefrontBraveSpanHandler(spanHandler));
		}
	}
}
