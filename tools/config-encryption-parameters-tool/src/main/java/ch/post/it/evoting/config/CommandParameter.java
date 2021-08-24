/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.config;

import org.apache.commons.cli.Option;

/**
 * Represents a parameter of a command. A CommandParameter contains the following parts:
 * <ul>
 * <li>A parameter name.</li>
 * <li>A required indicator.</li>
 * <li>A number of values.</li>
 * </ul>
 * Each CommandParameter is capable to generate a {@link Option} to be used by the command parser.
 */
public enum CommandParameter {
	P12_PATH("p12_path", true, 1) {
		@Override
		public Option generateOption() {
			return Option.builder(getParameterName()).longOpt("p12_path").desc("P12 path. Can be absolute or relative.")
					.numberOfArgs(getNumberOfArgs()).build();
		}
	},
	ENCRYPTION_PARAMS("params_path", true, 1) {
		@Override
		public Option generateOption() {
			return Option.builder(getParameterName()).longOpt("params_path").desc("Encryption Parameters signed JSON. Can be absolute or relative.")
					.numberOfArgs(getNumberOfArgs()).build();
		}
	},
	SEED_PATH("seed_path", true, 1) {
		@Override
		public Option generateOption() {
			return Option.builder(getParameterName()).longOpt("seed_path").desc("Path for file with the seed. Can be absolute or relative.")
					.numberOfArgs(getNumberOfArgs()).build();
		}
	},
	SEED_SIG_PATH("seed_sig_path", true, 1) {
		@Override
		public Option generateOption() {
			return Option.builder(getParameterName()).longOpt("seed_sig_path")
					.desc("Path for file with the seed signature. Can be absolute or relative.").numberOfArgs(getNumberOfArgs()).build();
		}
	},
	OUT("out", false, 1) {
		@Override
		public Option generateOption() {
			return Option.builder(getParameterName()).longOpt("outPath").desc("output path. Can be absolute or relative. default value:'./output/'.")
					.numberOfArgs(getNumberOfArgs()).build();
		}
	},
	TRUSTED_CA_PATH("trusted_ca_path", false, 1) {
		@Override
		public Option generateOption() {
			return Option.builder(getParameterName()).desc("Path for the file where the trusted CA is.").numberOfArgs(getNumberOfArgs()).build();
		}
	};

	private final String parameterName;

	private final boolean required;

	private final int numberOfArgs;

	CommandParameter(final String parameterName, final boolean required, final int numberOfArgs) {
		this.parameterName = parameterName;
		this.required = required;
		this.numberOfArgs = numberOfArgs;
	}

	/**
	 * Gets parameter name.
	 *
	 * @return the parameter name
	 */
	public String getParameterName() {
		return parameterName;
	}

	/**
	 * Is required.
	 *
	 * @return true if the parameter is required, false otherwise
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * Gets number of args.
	 *
	 * @return the number of args
	 */
	public int getNumberOfArgs() {
		return numberOfArgs;
	}

	/**
	 * Generate {@link Option} to be used by the command parser.
	 *
	 * @return the option to be used by the command parser
	 */
	public abstract Option generateOption();
}
