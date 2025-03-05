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

package org.springframework.boot.testsupport.classpath.resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

/**
 * A collection of resources.
 *
 * @author Andy Wilkinson
 */
class Resources {

	private final Path root;

	private final Map<String, Resource> resources = new HashMap<>();

	Resources(Path root) {
		this.root = root;
	}

	Resources addPackage(Package root, String[] resourceNames) {
		Set<String> unmatchedNames = new HashSet<>(Arrays.asList(resourceNames));
		try {
			Enumeration<URL> sources = getClass().getClassLoader().getResources(root.getName().replace(".", "/"));
			for (URL source : Collections.list(sources)) {
				Path sourceRoot = Paths.get(source.toURI());
				for (String resourceName : resourceNames) {
					Path resource = sourceRoot.resolve(resourceName);
					if (Files.isRegularFile(resource)) {
						Path target = this.root.resolve(resourceName);
						Path targetDirectory = target.getParent();
						if (!Files.isDirectory(targetDirectory)) {
							Files.createDirectories(targetDirectory);
						}
						Files.copy(resource, target);
						register(resourceName, target, true);
						unmatchedNames.remove(resourceName);
					}
				}
			}
			Assert.isTrue(unmatchedNames.isEmpty(),
					"Package '" + root.getName() + "' did not contain resources: " + unmatchedNames);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
		return this;
	}

	Resources addResource(String name, String content, boolean additional) {
		Path resourcePath = this.root.resolve(name);
		if (Files.isDirectory(resourcePath)) {
			throw new IllegalStateException(
					"Cannot create resource '" + name + "' as a directory already exists at that location");
		}
		Path parent = resourcePath.getParent();
		try {
			if (!Files.isDirectory(resourcePath)) {
				Files.createDirectories(parent);
			}
			Files.writeString(resourcePath, processContent(content));
			register(name, resourcePath, additional);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return this;
	}

	private void register(String name, Path resourcePath, boolean additional) {
		Resource resource = new Resource(resourcePath, additional);
		register(name, resource);
		Path ancestor = resourcePath.getParent();
		while (!this.root.equals(ancestor)) {
			Resource ancestorResource = new Resource(ancestor, additional);
			register(this.root.relativize(ancestor).toString(), ancestorResource);
			ancestor = ancestor.getParent();
		}
	}

	private void register(String name, Resource resource) {
		this.resources.put(name, resource);
		if (Files.isDirectory(resource.path())) {
			if (name.endsWith("/")) {
				this.resources.put(name.substring(0, name.length() - 1), resource);
			}
			else {
				this.resources.put(name + "/", resource);
			}
		}
	}

	private String processContent(String content) {
		return content.replace("${resourceRoot}", this.root.toString());
	}

	Resources addDirectory(String name) {
		Path directoryPath = this.root.resolve(name);
		if (Files.isRegularFile(directoryPath)) {
			throw new IllegalStateException(
					"Cannot create directory '" + name + " as a file already exists at that location");
		}
		try {
			Files.createDirectories(directoryPath);
			register(name, directoryPath, true);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return this;
	}

	void delete() {
		try {
			FileSystemUtils.deleteRecursively(this.root);
			this.resources.clear();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	Path getRoot() {
		return this.root;
	}

	Resource find(String name) {
		return this.resources.get(name);
	}

}
