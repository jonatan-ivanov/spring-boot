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

package org.springframework.boot.autoconfigure.observability.tracing.annotation;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.observability.TracingNewSpanParser;
import org.springframework.core.observability.tracing.Span;
import org.springframework.core.observability.tracing.annotation.NewSpan;
import org.springframework.core.observability.tracing.internal.SpanNameUtil;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link NewSpanParser} that parses only the span name.
 *
 * @author Christian Schwerdtfeger
 * @since 1.2.0
 */
public class DefaultSpanCreator implements TracingNewSpanParser {

	private static final Log log = LogFactory.getLog(DefaultSpanCreator.class);

	@Override
	public void parse(MethodInvocation pjp, NewSpan newSpan, Span span) {
		String name = newSpan == null || !StringUtils.hasText(newSpan.name()) ? pjp.getMethod().getName()
				: newSpan.name();
		String changedName = SpanNameUtil.toLowerHyphen(name);
		if (log.isDebugEnabled()) {
			log.debug("For the class [" + pjp.getThis().getClass() + "] method " + "[" + pjp.getMethod().getName()
					+ "] will name the span [" + changedName + "]");
		}
		span.name(changedName);
	}

}
