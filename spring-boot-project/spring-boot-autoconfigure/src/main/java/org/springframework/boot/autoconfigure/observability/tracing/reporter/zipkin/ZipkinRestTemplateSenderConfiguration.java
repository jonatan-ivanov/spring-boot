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

import zipkin2.reporter.Sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.CachingZipkinUrlExtractor;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.RestTemplateSender;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.StaticInstanceZipkinLoadBalancer;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.ZipkinLoadBalancer;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.ZipkinRestTemplateCustomizer;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.ZipkinRestTemplateProvider;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.ZipkinRestTemplateWrapper;
import org.springframework.boot.autoconfigure.observability.tracing.reporter.zipkin.core.ZipkinUrlExtractor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(name = ZipkinAutoConfiguration.SENDER_BEAN_NAME)
@Conditional(ZipkinSenderCondition.class)
@EnableConfigurationProperties(ZipkinSenderProperties.class)
class ZipkinRestTemplateSenderConfiguration {

	@Bean(ZipkinAutoConfiguration.SENDER_BEAN_NAME)
	Sender restTemplateSender(ZipkinProperties zipkin, ZipkinRestTemplateCustomizer zipkinRestTemplateCustomizer,
			ZipkinRestTemplateProvider zipkinRestTemplateProvider) {
		RestTemplate restTemplate = zipkinRestTemplateProvider.zipkinRestTemplate();
		restTemplate = zipkinRestTemplateCustomizer.customizeTemplate(restTemplate);
		return new RestTemplateSender(restTemplate, zipkin.getBaseUrl(), zipkin.getApiPath(), zipkin.getEncoder());
	}

	@Bean
	@ConditionalOnMissingBean
	ZipkinRestTemplateProvider zipkinRestTemplateProvider(ZipkinProperties zipkin, ZipkinUrlExtractor extractor) {
		return () -> new ZipkinRestTemplateWrapper(() -> zipkin.getBaseUrl(), extractor);
	}

	@Bean
	ZipkinUrlExtractor defaultZipkinUrlExtractor(final ZipkinLoadBalancer zipkinLoadBalancer) {
		return new CachingZipkinUrlExtractor(zipkinLoadBalancer);
	}

	@Bean
	@ConditionalOnMissingBean
	ZipkinLoadBalancer staticInstanceLoadBalancer(final ZipkinProperties zipkinProperties) {
		return new StaticInstanceZipkinLoadBalancer(zipkinProperties::getBaseUrl);
	}
/*
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.springframework.cloud.client.loadbalancer.LoadBalancerClient")
	static class DefaultZipkinUrlExtractorConfiguration {

		@Autowired(required = false)
		LoadBalancerClient client;

		@Bean
		@ConditionalOnMissingBean
		ZipkinLoadBalancer staticInstanceLoadBalancer(final ZipkinProperties zipkinProperties) {
			return new StaticInstanceZipkinLoadBalancer(zipkinProperties);
		}

	}*/

	/*@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(LoadBalancerClient.class)
	static class DiscoveryClientZipkinUrlExtractorConfiguration {

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(value = "spring.observability.tracing.zipkin.discovery-client-enabled", havingValue = "true",
				matchIfMissing = true)
		static class ZipkinClientLoadBalancedConfiguration {

			@Autowired(required = false)
			LoadBalancerClient client;

			@Bean
			@ConditionalOnMissingBean
			ZipkinLoadBalancer loadBalancerClientZipkinLoadBalancer(ZipkinProperties zipkinProperties) {
				return new LoadBalancerClientZipkinLoadBalancer(this.client, zipkinProperties);
			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(value = "spring.observability.tracing.zipkin.discovery-client-enabled", havingValue = "false")
		static class ZipkinClientNoOpConfiguration {

			@Bean
			@ConditionalOnMissingBean
			ZipkinLoadBalancer staticInstanceLoadBalancer(final ZipkinProperties zipkinProperties) {
				return new StaticInstanceZipkinLoadBalancer(zipkinProperties);
			}

		}

	}*/

}
