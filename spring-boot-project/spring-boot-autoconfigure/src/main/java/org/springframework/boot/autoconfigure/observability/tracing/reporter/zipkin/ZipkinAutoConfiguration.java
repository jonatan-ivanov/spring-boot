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

package org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import zipkin2.CheckResult;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.InMemoryReporterMetrics;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.ReporterMetrics;
import zipkin2.reporter.Sender;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.observability.tracing.brave.BraveAutoConfiguration;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.DefaultZipkinRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.EndpointLocator;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.ZipkinRestTemplateCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables reporting to Zipkin via HTTP.
 *
 * The {@link ZipkinRestTemplateCustomizer} allows you to customize the
 * {@link RestTemplate} that is used to send Spans to Zipkin. Its default implementation -
 * {@link DefaultZipkinRestTemplateCustomizer} adds the GZip compression.
 *
 * @author Spencer Gibb
 * @author Tim Ysewyn
 * @since 1.0.0
 * @see ZipkinRestTemplateCustomizer
 * @see DefaultZipkinRestTemplateCustomizer
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ZipkinProperties.class)
@ConditionalOnClass({ Sender.class, EndpointLocator.class })
@ConditionalOnProperty(value = { "spring.observability.tracing.enabled", "spring.observability.tracing.zipkin.enabled" }, matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.cloud.autoconfigure.RefreshAutoConfiguration")
@AutoConfigureBefore(BraveAutoConfiguration.class)
@Import({ org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.ZipkinSenderConfigurationImportSelector.class, org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.ZipkinBraveConfiguration.class })
public class ZipkinAutoConfiguration {

	/**
	 * Zipkin reporter bean name. Name of the bean matters for supporting multiple tracing
	 * systems.
	 */
	public static final String REPORTER_BEAN_NAME = "zipkinReporter";

	/**
	 * Zipkin sender bean name. Name of the bean matters for supporting multiple tracing
	 * systems.
	 */
	public static final String SENDER_BEAN_NAME = "zipkinSender";

	private static final Log log = LogFactory.getLog(ZipkinAutoConfiguration.class);

	/** Limits {@link Sender#check()} to {@code deadlineMillis}. */
	static CheckResult checkResult(Sender sender, long deadlineMillis) {
		CheckResult[] outcome = new CheckResult[1];
		Thread thread = new Thread(sender + " check()") {
			@Override
			public void run() {
				try {
					outcome[0] = sender.check();
				}
				catch (Throwable e) {
					outcome[0] = CheckResult.failed(e);
				}
			}
		};
		thread.start();
		try {
			thread.join(deadlineMillis);
			if (outcome[0] != null) {
				return outcome[0];
			}
			thread.interrupt();
			return CheckResult
					.failed(new TimeoutException(thread.getName() + " timed out after " + deadlineMillis + "ms"));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return CheckResult.failed(e);
		}
	}

	@Bean(REPORTER_BEAN_NAME)
	@ConditionalOnMissingBean(name = REPORTER_BEAN_NAME)
	Reporter<Span> reporter(ReporterMetrics reporterMetrics, ZipkinProperties zipkin,
			@Qualifier(SENDER_BEAN_NAME) Sender sender) {
		CheckResult checkResult = checkResult(sender, 1_000L);
		logCheckResult(sender, checkResult);

		// historical constraint. Note: AsyncReporter supports memory bounds
		AsyncReporter<Span> asyncReporter = AsyncReporter.builder(sender).queuedMaxSpans(1000)
				.messageTimeout(zipkin.getMessageTimeout(), TimeUnit.SECONDS).metrics(reporterMetrics)
				.build(zipkin.getEncoder());

		return asyncReporter;
	}

	private void logCheckResult(Sender sender, CheckResult checkResult) {
		if (log.isDebugEnabled() && checkResult != null && checkResult.ok()) {
			log.debug("Check result of the [" + sender.toString() + "] is [" + checkResult + "]");
		}
		else if (checkResult != null && !checkResult.ok()) {
			log.warn("Check result of the [" + sender.toString() + "] contains an error [" + checkResult + "]");
		}
	}

	@Bean
	@ConditionalOnMissingBean
	ZipkinRestTemplateCustomizer zipkinRestTemplateCustomizer(ZipkinProperties zipkinProperties) {
		return new DefaultZipkinRestTemplateCustomizer(() -> zipkinProperties.getCompression().isEnabled());
	}

	@Bean
	@ConditionalOnMissingBean
	ReporterMetrics sleuthReporterMetrics() {
		return new InMemoryReporterMetrics();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
	static class TraceMetricsInMemoryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ReporterMetrics sleuthReporterMetrics() {
			return new InMemoryReporterMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	static class TraceMetricsMicrometerConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnMissingBean(ReporterMetrics.class)
		static class NoReporterMetricsBeanConfiguration {

			/*@Bean
			@ConditionalOnBean(MeterRegistry.class)
			@ConditionalOnClass(name = "zipkin2.reporter.metrics.micrometer.MicrometerReporterMetrics")
			ReporterMetrics sleuthMicrometerReporterMetrics(MeterRegistry meterRegistry) {
				return MicrometerReporterMetrics.create(meterRegistry);
			}*/

			@Bean
//			@ConditionalOnMissingClass("zipkin2.reporter.metrics.micrometer.MicrometerReporterMetrics")
			ReporterMetrics sleuthReporterMetrics() {
				return new InMemoryReporterMetrics();
			}

		}

	}

}
