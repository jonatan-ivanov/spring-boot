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

package org.springframework.boot.autoconfigure.observability;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.TimerRecordingListener;

/**
 * Using this {@link RecordingListener} implementation, you can register multiple
 * listeners but only the first matching one will be applied.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FirstMatchingCompositeRecordingListener
		implements CompositeRecordingListener {

	private final List<? extends TimerRecordingListener> listeners;

	/**
	 * Creates a new instance of {@link AllMatchingCompositeRecordingListener}.
	 *
	 * @param listeners the listeners that are registered under the composite
	 */
	public FirstMatchingCompositeRecordingListener(
			TimerRecordingListener<?>... listeners) {
		this(Arrays.asList(listeners));
	}

	/**
	 * Creates a new instance of {@link AllMatchingCompositeRecordingListener}.
	 *
	 * @param listeners the listeners that are registered under the composite
	 */
	public FirstMatchingCompositeRecordingListener(
			List<? extends TimerRecordingListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void onStart(Timer.Sample intervalRecording, Timer.Context context) {
		getFirstApplicableListener(context)
				.ifPresent(listener -> listener.onStart(intervalRecording, context));
	}

	private Optional<? extends TimerRecordingListener> getFirstApplicableListener(
			Timer.Context context) {
		return this.listeners.stream()
				.filter(listener -> listener.supportsContext(context)).findFirst();
	}

	@Override
	public void onStop(Timer.Sample intervalRecording, Timer.Context context, Timer timer,
			Duration duration) {
		getFirstApplicableListener(context).ifPresent(
				listener -> listener.onStop(intervalRecording, context, timer, duration));
	}

	@Override
	public void onError(Timer.Sample intervalRecording, Timer.Context context,
			Throwable throwable) {
		getFirstApplicableListener(context).ifPresent(
				listener -> listener.onError(intervalRecording, context, throwable));
	}

	@Override
	public void onRestore(Timer.Sample intervalRecording, Timer.Context context) {
		getFirstApplicableListener(context)
				.ifPresent(listener -> listener.onRestore(intervalRecording, context));
	}

	@Override
	public List<? extends TimerRecordingListener> getListeners() {
		return this.listeners;
	}

	@Override
	public boolean supportsContext(Timer.Context context) {
		return true;
	}

}
