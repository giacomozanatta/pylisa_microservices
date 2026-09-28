package it.unive.pylisa.ros2;

import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.string.BoundedStringSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.outputs.HtmlResults;
import it.unive.pylisa.analysis.PyFieldSensitivePointBasedHeap;
import it.unive.pylisa.analysis.ValueDomainProduct;
import it.unive.pylisa.analysis.constants.ConstantPropagation;
import it.unive.pylisa.analysis.types.PythonInferredTypes;
import it.unive.pylisa.testutil.LiSAConfigs;
import java.util.function.Supplier;

/**
 * The analysis configurations the ROS 2 state tests run with. They share
 * pylisa's field-sensitive heap and inferred Python types, and differ in the
 * value domain.
 * <p>
 * Every configuration is best-effort: calls to unknown code are assumed to
 * return an unknown value and to have no other effect, so results are not
 * sound over-approximations of every execution.
 * </p>
 */
public enum RosConfig {

	/**
	 * Constant propagation: every value is either one known constant or
	 * unknown.
	 */
	CP(ConstantPropagation::new, ValueReader.constantPropagation()),

	/**
	 * Constant propagation together with a bounded set of strings: a string
	 * can also be known to be one of a few constants.
	 */
	CP_BSS(
			() -> new ValueDomainProduct<>(new ConstantPropagation(), new BoundedStringSet()),
			ValueReader.constantPropagationWithStringSets());

	private final Supplier<ValueDomain<?>> valueDomain;

	private final ValueReader reader;

	RosConfig(
			Supplier<ValueDomain<?>> valueDomain,
			ValueReader reader) {
		this.valueDomain = valueDomain;
		this.reader = reader;
	}

	/**
	 * Builds the LiSA configuration for this analysis. The analysis results
	 * are also dumped as JSON under the given working directory, so that a
	 * failing test can be investigated on the full states.
	 *
	 * @param workdir the working directory, relative to {@code tests/}
	 *
	 * @return the configuration
	 */
	public LiSAConfiguration configuration(
			String workdir) {
		LiSAConfiguration conf = LiSAConfigs.getDefaultConf(workdir);
		conf.outputs.removeIf(HtmlResults.class::isInstance);
		conf.analysis = new SimpleAbstractDomain<>(
				new PyFieldSensitivePointBasedHeap(),
				valueDomain.get(),
				new PythonInferredTypes());
		return conf;
	}

	/**
	 * Yields the reader for the value domain of this configuration.
	 *
	 * @return the reader
	 */
	public ValueReader reader() {
		return reader;
	}

	/**
	 * Yields the label of this configuration, which also states that its
	 * results are best-effort.
	 *
	 * @return the label
	 */
	public String label() {
		return name() + "/best-effort";
	}
}
