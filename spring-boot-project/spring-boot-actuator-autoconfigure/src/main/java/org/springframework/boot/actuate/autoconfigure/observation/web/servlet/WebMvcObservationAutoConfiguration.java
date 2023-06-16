/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.web.servlet;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ServerHttpObservationPredicate;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.DispatcherServlet;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring Web
 * MVC servlet-based request mappings.
 *
 * @author Brian Clozel
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @since 3.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class, ObservationAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ DispatcherServlet.class, Observation.class })
@ConditionalOnBean(ObservationRegistry.class)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
@Import({ ObservationFilterConfigurations.TracingHeaderObservation.class,
		ObservationFilterConfigurations.DefaultObservation.class })
public class WebMvcObservationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	// TODO: Only create this bean if ignored path is not empty
	// observationProperties.getHttp().getServer().getIgnoredPaths()
	public ServerHttpObservationPredicate serverHttpObservationPredicate(ObservationProperties observationProperties) {
		return new ServerHttpObservationPredicate(observationProperties, WebMvcObservationAutoConfiguration::getPath,
				WebMvcObservationAutoConfiguration::getServerHttpObservation);
	}

	@Nullable
	private static String getPath(Context context) {
		if (context instanceof ServerRequestObservationContext serverContext) {
			return serverContext.getCarrier().getRequestURI();
		}
		return null;
	}

	@Nullable
	private static Observation getServerHttpObservation(Context ignored) {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes) {
			// TODO: can we make ServerHttpObservationFilter.CURRENT_OBSERVATION_ATTRIBUTE
			// public?
			return (Observation) requestAttributes
				.getAttribute(ServerHttpObservationFilter.class.getName() + ".observation", SCOPE_REQUEST);
		}
		return null;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(MeterRegistry.class)
	@ConditionalOnBean(MeterRegistry.class)
	static class MeterFilterConfiguration {

		@Bean
		@Order(0)
		MeterFilter metricsHttpServerUriTagFilter(ObservationProperties observationProperties,
				MetricsProperties metricsProperties) {
			String name = observationProperties.getHttp().getServer().getRequests().getName();
			MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
					() -> String.format("Reached the maximum number of URI tags for '%s'.", name));
			return MeterFilter.maximumAllowableTags(name, "uri", metricsProperties.getWeb().getServer().getMaxUriTags(),
					filter);
		}

	}

}
