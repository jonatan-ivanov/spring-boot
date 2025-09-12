/*
 * Copyright 2012-present the original author or authors.
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

package smoketest.data.jpa.web;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import smoketest.data.jpa.domain.City;
import smoketest.data.jpa.service.CityRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SampleController {

	@Autowired
	private CityRepository cityRepository;

	@Autowired
	private ObservationRegistry observationRegistry;

	@GetMapping("/")
	@ResponseBody
	public String helloWorld() {
		City city = this.cityRepository.findByNameAndCountryAllIgnoringCase("Bath", "UK");
		Observation observation = this.observationRegistry.getCurrentObservation();
		System.out.println(observation.getContextView().getName());
		return city.getName();
	}

}
