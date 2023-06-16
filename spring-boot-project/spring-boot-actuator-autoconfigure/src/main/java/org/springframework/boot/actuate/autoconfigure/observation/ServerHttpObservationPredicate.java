package org.springframework.boot.actuate.autoconfigure.observation;

import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationPredicate;

import org.springframework.lang.Nullable;

/**
 * {@link ObservationPredicate} to ignore HTTP server Observations.
 *
 * @author Jonatan Ivanov
 */
public class ServerHttpObservationPredicate implements ObservationPredicate {

	private final List<String> ignoredPaths;

	private final PathProvider pathProvider;

	private final ServerHttpObservationProvider serverHttpObservationProvider;

	public ServerHttpObservationPredicate(ObservationProperties properties, PathProvider pathProvider,
			ServerHttpObservationProvider serverHttpObservationProvider) {
		this.ignoredPaths = properties.getHttp().getServer().getIgnoredPaths();
		this.pathProvider = pathProvider;
		this.serverHttpObservationProvider = serverHttpObservationProvider;
	}

	@Override
	public boolean test(String name, Context context) {
		if (this.ignoredPaths.isEmpty()) {
			return true;
		}

		String path = this.pathProvider.getPath(context);
		if (path != null) {
			return this.ignoredPaths.stream().noneMatch(path::startsWith);
		}

		Observation serverHttpObservation = this.serverHttpObservationProvider.getServerHttpObservation(context);
		return serverHttpObservation == null || !serverHttpObservation.isNoop();
	}

	/**
	 * Functional interface to get the path from the observation context.
	 */
	@FunctionalInterface
	public interface PathProvider {

		/**
		 * Returns the path from the http request or null if not found.
		 * @param context the context of the observation
		 * @return the path or null if not found
		 */
		@Nullable
		String getPath(Context context);

	}

	/**
	 * Functional interface to get the corresponding http observation (if any).
	 */
	@FunctionalInterface
	public interface ServerHttpObservationProvider {

		/**
		 * Returns the http observation that triggered the current one (if any).
		 * @return the corresponding http observation or null if there is none.
		 */
		@Nullable
		Observation getServerHttpObservation(Context context);

	}

}
