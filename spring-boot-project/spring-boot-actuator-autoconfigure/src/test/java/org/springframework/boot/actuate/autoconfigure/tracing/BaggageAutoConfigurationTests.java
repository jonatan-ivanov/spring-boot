/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.function.Supplier;

import brave.propagation.CurrentTraceContext;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.MDC;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BraveAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class BaggageAutoConfigurationTests {

	static final String COUNTRY_CODE = "country-code";
	static final String BUSINESS_PROCESS = "bp";

	Span span;

	@BeforeEach
	@AfterEach
	void setup() {
		MDC.clear();
	}

	@ParameterizedTest
	@EnumSource(AutoConfig.class)
	void shouldSetEntriesToMdcFromSpan(AutoConfig autoConfig) {
		autoConfig.get().run(context -> {
			startSpan(context);
			try (io.micrometer.tracing.Tracer.SpanInScope scope = tracer(context).withSpan(this.span)) {
				assertThat(MDC.get("traceId")).isEqualTo(this.span.context().traceId());
			}

			assertThat(MDC.get("traceId")).isNullOrEmpty();
		});
	}

	private void startSpan(ApplicationContext context) {
		this.span = tracer(context).nextSpan().name("span").start();
	}

	private io.micrometer.tracing.Tracer tracer(ApplicationContext context) {
		return context.getBean(io.micrometer.tracing.Tracer.class);
	}

	@ParameterizedTest
	@EnumSource(AutoConfig.class)
	void shouldSetEntriesToMdcFromSpanWithBaggage(AutoConfig autoConfig) {
		autoConfig.get().run(context -> {
			startSpan(context);

			try (BaggageInScope fo = context.getBean(BaggageManager.class).createBaggage(COUNTRY_CODE)
					.set(this.span.context(), "FO"); BaggageInScope bp = context.getBean(BaggageManager.class).createBaggage(BUSINESS_PROCESS)
					.set(this.span.context(), "ALM"); io.micrometer.tracing.Tracer.SpanInScope scope = tracer(context).withSpan(this.span)) {
				assertThat(MDC.get(COUNTRY_CODE)).isEqualTo("FO");
				assertThat(MDC.get(BUSINESS_PROCESS)).isEqualTo("ALM");
			}

			assertThat(MDC.get(COUNTRY_CODE)).isNull();
			assertThat(MDC.get(BUSINESS_PROCESS)).isNull();
		});
	}

	@ParameterizedTest
	@EnumSource(AutoConfig.class)
	void shouldRemoveEntriesFromMdcForNullSpan(AutoConfig autoConfig) {
		autoConfig.get().run(context -> {
			startSpan(context);

			try (BaggageInScope fo = context.getBean(BaggageManager.class).createBaggage(COUNTRY_CODE)
					.set(this.span.context(), "FO"); io.micrometer.tracing.Tracer.SpanInScope scope1 = tracer(context).withSpan(this.span)) {
				assertThat(MDC.get(COUNTRY_CODE)).isEqualTo("FO");

				try (io.micrometer.tracing.Tracer.SpanInScope scope2 = tracer(context).withSpan(null)) {
					assertThat(MDC.get(COUNTRY_CODE)).isNullOrEmpty();
				}
			}
		});
	}

	@ParameterizedTest
	@EnumSource(AutoConfig.class)
	void shouldRemoveEntriesFromMdcForNullSpanAndMdcFieldsSetDirectly(AutoConfig autoConfig) {
		autoConfig.get().run(context -> {
			startSpan(context);
			MDC.put(COUNTRY_CODE, "FO");

			// the span is holding no baggage so it clears the preceding values
			try (io.micrometer.tracing.Tracer.SpanInScope scope = tracer(context).withSpan(this.span)) {
				assertThat(MDC.get(COUNTRY_CODE)).isNullOrEmpty();
			}

			assertThat(MDC.get(COUNTRY_CODE)).isEqualTo("FO");

			try (io.micrometer.tracing.Tracer.SpanInScope scope = tracer(context).withSpan(this.span)) {
				assertThat(MDC.get(COUNTRY_CODE)).isNullOrEmpty();
			}

			assertThat(MDC.get(COUNTRY_CODE)).isEqualTo("FO");
		});
	}

	@ParameterizedTest
	@EnumSource(AutoConfig.class)
	void shouldRemoveEntriesFromMdcFromNullSpan(AutoConfig autoConfig) {
		autoConfig.get().run(context -> {
			startSpan(context);
			MDC.put("traceId", "A");

			// can't use NOOP as it is special cased
			try (io.micrometer.tracing.Tracer.SpanInScope scope = tracer(context).withSpan(null)) {
				assertThat(MDC.get("traceId")).isNullOrEmpty();
			}

			assertThat(MDC.get("traceId")).isEqualTo("A");
		});

	}

	@ParameterizedTest
	@EnumSource(AutoConfig.class)
	void shouldPickPreviousMdcEntriesWhenTheirKeysAreWhitelisted(AutoConfig autoConfig) {
		autoConfig.get().run(context -> {
			startSpan(context);
			MDC.put(COUNTRY_CODE, "FO");

			try (io.micrometer.tracing.Tracer.SpanInScope scope = tracer(context).withSpan(this.span)) {
				MDC.put(COUNTRY_CODE, "BV");

				assertThat(MDC.get(COUNTRY_CODE)).isEqualTo("BV");
			}

			assertThat(MDC.get(COUNTRY_CODE)).isEqualTo("FO");

		});
	}

	enum AutoConfig implements Supplier<ApplicationContextRunner> {


//		BRAVE {
//			@Override
//			public ApplicationContextRunner get() {
//				return new ApplicationContextRunner()
//						.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class))
//						.withPropertyValues("management.tracing.baggage.remote-fields=x-vcap-request-id,country-code",
//								"management.tracing.baggage.local-fields=bp", "management.tracing.baggage.correlation-fields=country-code,bp");
//			}
//		},

		OTEL {
			@Override
			public ApplicationContextRunner get() {
				return new ApplicationContextRunner()
						.withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class))
						.withPropertyValues("management.tracing.baggage.remote-fields=x-vcap-request-id,country-code",
								"management.tracing.baggage.local-fields=bp", "management.tracing.baggage.correlation-fields=country-code,bp");
			}
		}
	}

}

