package com.almejo.osom;

import com.almejo.osom.ui.EmulatorApp;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

@Slf4j
public class Main {

	private static final String USAGE = "java -jar osom.jar --rom <path> [--no-bios] [--log-level <level>] [--trace]";

	public static void main(String[] args) throws IOException {

		Options options = buildOptions();

		CommandLine commandLine;
		try {
			commandLine = new DefaultParser().parse(options, args);
		} catch (ParseException exception) {
			log.error("Error: {}", exception.getMessage());
			printUsage(options);
			System.exit(1);
			return;
		}

		if (commandLine.hasOption("log-level")) {
			String logLevel = commandLine.getOptionValue("log-level").toUpperCase();
			if (!isValidLogLevel(logLevel)) {
				log.error("Error: Invalid log level '{}'. Valid levels: TRACE, DEBUG, INFO, WARN, ERROR", logLevel);
				System.exit(1);
				return;
			}
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
			rootLogger.setLevel(Level.valueOf(logLevel));
		}

		if (!commandLine.hasOption("rom")) {
			log.error("Error: --rom option is required");
			printUsage(options);
			System.exit(1);
			return;
		}

		String filename = commandLine.getOptionValue("rom");
		File romFile = new File(filename);
		if (!romFile.exists() || !romFile.isFile()) {
			log.error("Error: ROM file not found at '{}'", filename);
			System.exit(1);
			return;
		}

		boolean bios = !commandLine.hasOption("no-bios");
		boolean trace = commandLine.hasOption("trace");

		try {
			new EmulatorApp().run(bios, filename, trace);
		} catch (IllegalStateException exception) {
			System.exit(1);
		}
	}

	private static boolean isValidLogLevel(String level) {
		return "TRACE".equals(level) || "DEBUG".equals(level) || "INFO".equals(level)
				|| "WARN".equals(level) || "ERROR".equals(level);
	}

	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(USAGE, options);
	}

	private static Options buildOptions() {
		return new Options()
				.addOption(Option.builder("r")
						.argName("path")
						.hasArg()
						.longOpt("rom")
						.desc("Path to the Game Boy ROM file (required)")
						.build())
				.addOption(Option.builder("b")
						.longOpt("no-bios")
						.desc("Boot without BIOS (skip boot sequence)")
						.build())
				.addOption(Option.builder("l")
						.argName("level")
						.hasArg()
						.longOpt("log-level")
						.desc("Set logging level (TRACE, DEBUG, INFO, WARN, ERROR)")
						.build())
				.addOption(Option.builder("t")
						.longOpt("trace")
						.desc("Output CPU execution trace in Gameboy Doctor format to stdout")
						.build());
	}
}