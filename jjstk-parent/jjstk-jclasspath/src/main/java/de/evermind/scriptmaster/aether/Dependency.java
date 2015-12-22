package de.evermind.scriptmaster.aether;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Defines the structure of dependencies.
 */
final class Dependency {

	private final String group;
	private final String name;
	private final String version;
	private final String classifier;
	private final String ext;

	/**
	 * Creates a dependency out of the default gradle-like pattern with the :
	 * separator.<br/>
	 * Example:<br/>
	 * 'org.springframework.data:spring-data-jpa:1.8.0.RELEASE' <br/>
	 */
	public static Dependency parse(String fullFormat) {
		Iterator<String> parts = Arrays.asList(fullFormat.split(":")).iterator();
		String group = parts.next();
		String name = parts.next();
		String ext = parts.next();
		if (!parts.hasNext()) {
			return new Dependency(group, name, ext);
		}
		String classifier = parts.next();
		if (!parts.hasNext()) {
			return new Dependency(group, name, ext, classifier);
		}
		return new Dependency(group, name, ext, classifier, parts.next());
	}

	public Dependency(String group, String name, String version) {
		this(group, name, "", version);
	}

	public Dependency(String group, String name, String ext, String version) {
		this(group, name, ext, "", version);
	}

	public Dependency(String group, String name, String ext, String classifier, String version) {
		this.group = group;
		this.name = name;
		this.version = version;
		this.classifier = classifier;
		this.ext = ext;
	}

	public String getGroup() {
		return group;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getExt() {
		return ext;
	}

	@Override
	public int hashCode() {
		return Objects.hash(group, name, version, classifier, ext);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Dependency that = (Dependency) obj;
		return Objects.equals(this.group, that.group) && //
				Objects.equals(this.name, that.name) && //
				Objects.equals(this.version, that.version) && //
				Objects.equals(this.classifier, that.classifier) && //
				Objects.equals(this.ext, that.ext);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("{");
		append(s, "group", group);
		append(s, "name", name);
		append(s, "version", version);
		append(s, "classifier", classifier);
		append(s, "ext", ext);
		return s.append("}").toString();
	}

	private void append(StringBuilder s, String name, String value) {
		if (value.isEmpty()) {
			return;
		}
		if (s.length() > 4) {
			s.append(", ");
		}
		s.append('"').append(name).append("\":\"").append(value).append('"');
	}

}
