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

import brave.sampler.CountingSampler;
import brave.sampler.Sampler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.observability.tracing.brave.sampler.ProbabilityBasedSampler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * {@linkplain Configuration configuration} for {@link Sampler}.
 *
 * @author Marcin Grzejszczak
 * @see SamplerCondition
 * @since 2.1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ObservabilityTracingSamplerProperties.class)
class BraveSamplerConfiguration {

	@Bean
	@ConditionalOnMissingBean
	Sampler sleuthTraceSampler() {
		return Sampler.NEVER_SAMPLE;
	}

	// NOTE: Brave's default samplers return Sampler.NEVER_SAMPLE if the config implies
	// that
	static Sampler samplerFromProps(ObservabilityTracingSamplerProperties config) {
		if (config.getProbability() != null) {
			return CountingSampler.create(config.getProbability());
		}
		return brave.sampler.RateLimitingSampler.create(config.getRate());
	}

/*	@Configuration(proxyBeanMethods = false)
	@Conditional(SamplerCondition.class)
	@ConditionalOnBean(type = "org.springframework.cloud.context.scope.refresh.RefreshScope")
	protected static class RefreshScopedSamplerConfiguration {


		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(value = "spring.observability.tracing.sampler.refresh.enabled", havingValue = "false")
		public Sampler defaultNonRefreshScopeTraceSampler(ObservabilityTracingSamplerProperties config) {
			return sampler(config);
		}

		private Sampler sampler(ObservabilityTracingSamplerProperties config) {
			// TODO: Rewrite: refresh should replace the sampler, not change its state
			// internally
			if (config.getProbability() != null) {
				return new ProbabilityBasedSampler(config::getProbability);
			}
			return new RateLimitingSampler(config::getRate);
		}

	}*/

	@Configuration(proxyBeanMethods = false)
	@Conditional(SamplerCondition.class)
//	@ConditionalOnMissingBean(type = "org.springframework.cloud.context.scope.refresh.RefreshScope")
	protected static class NonRefreshScopeSamplerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public Sampler defaultTraceSampler(ObservabilityTracingSamplerProperties config) {
			return samplerFromProps(config);
		}

	}

}
