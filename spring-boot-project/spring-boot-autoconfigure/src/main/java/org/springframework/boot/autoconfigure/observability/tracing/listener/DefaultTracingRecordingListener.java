/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.observability.tracing.listener;

import java.time.Duration;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.tracing.CurrentTraceContext;
import io.micrometer.core.instrument.tracing.Span;
import io.micrometer.core.instrument.tracing.Tracer;

/**
 * {@link RecordingListener} that uses the Tracing API to record events.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DefaultTracingRecordingListener implements TracingRecordingListener {

	private final Tracer tracer;

	// private final TracingTagFilter tracingTagFilter = new TracingTagFilter();

	/**
	 * Creates a new instance of {@link DefaultTracingRecordingListener}.
	 *
	 * @param tracer the tracer to use to record events
	 */
	public DefaultTracingRecordingListener(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public void onRestore(Timer.Sample intervalRecording, Timer.Context context) {
		// TODO: check for nulls
		CurrentTraceContext.Scope scope = this.tracer.currentTraceContext()
				.maybeScope(getTracingContext(intervalRecording).getSpan().context());
		getTracingContext(intervalRecording).setScope(scope);
	}

	@Override
	public void onStart(Timer.Sample intervalRecording, Timer.Context context) {
		Span parentSpan = getTracingContext(intervalRecording).getSpan();
		Span childSpan = parentSpan != null ? getTracer().nextSpan(parentSpan)
				: getTracer().nextSpan();
		// childSpan.name(intervalRecording.getHighCardinalityName())
		childSpan.start();
		setSpanAndScope(intervalRecording, childSpan);
	}

	@Override
	public void onStop(Timer.Sample intervalRecording, Timer.Context context, Timer timer,
			Duration duration) {
		Span span = getTracingContext(intervalRecording).getSpan();
		// .name(intervalRecording.getHighCardinalityName());

		// TODO: This should go to the tags provider
		// this.tracingTagFilter.tagSpan(span, intervalRecording.getTags());

		cleanup(intervalRecording);
		span.end();
	}

	@Override
	public void onError(Timer.Sample intervalRecording, Timer.Context context,
			Throwable throwable) {
		Span span = getTracingContext(intervalRecording).getSpan();
		span.error(throwable);
	}

	// long getStartTimeInMicros(Timer.Sample recording) {
	// return TimeUnit.NANOSECONDS.toMicros(recording.getStartWallTime());
	// }
	//
	// long getStopTimeInMicros(Timer.Sample recording) {
	// return TimeUnit.NANOSECONDS.toMicros(
	// recording.getStartWallTime() + recording.getDuration().toNanos());
	// }

	@Override
	public Tracer getTracer() {
		return this.tracer;
	}

}
