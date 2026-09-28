package it.unive.pylisa.libraries.natives;

import it.unive.lisa.program.cfg.CodeLocation;
import java.util.Objects;

/**
 * A location derived from another one by a tag, used to give distinct
 * allocation sites to the several objects that a single library call creates
 * (for instance, the several helper objects one constructor creates).
 * Two tagged locations are equal when both their base location and their tag
 * are.
 */
public final class TaggedLocation implements CodeLocation {

	private final CodeLocation base;

	private final String tag;

	/**
	 * Builds the location.
	 *
	 * @param base the location it derives from
	 * @param tag  what distinguishes it from other locations with the same
	 *                 base
	 */
	public TaggedLocation(
			CodeLocation base,
			String tag) {
		this.base = Objects.requireNonNull(base);
		this.tag = Objects.requireNonNull(tag);
	}

	@Override
	public String getCodeLocation() {
		return base.getCodeLocation() + "#" + tag;
	}

	@Override
	public int compareTo(
			CodeLocation other) {
		return getCodeLocation().compareTo(other.getCodeLocation());
	}

	@Override
	public boolean equals(
			Object other) {
		return other instanceof TaggedLocation location && base.equals(location.base) && tag.equals(location.tag);
	}

	@Override
	public int hashCode() {
		return Objects.hash(base, tag);
	}

	@Override
	public String toString() {
		return getCodeLocation();
	}
}
