package com.almejo.osom;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

@Slf4j
public class Main {
	public static void main(String[] args) throws IOException {

		CommandLine commandLine;
		try {
			commandLine = parseCommandArguments(args);
		} catch (ParseException exception) {
			log.error("Error: {}", exception.getMessage());
			System.exit(0);
			return;
		}

		boolean bios = !commandLine.hasOption("no-bios");
		String filename = commandLine.getOptionValue("rom");

		new Emulator().run(bios, filename);
	}


	private static CommandLine parseCommandArguments(String[] args) throws ParseException {
		return new DefaultParser().parse(new Options().addOption(Option
				.builder("r")
				.argName("rom")
				.hasArg()
				.longOpt("rom")
				.desc("rom to load")
				.build())
				.addOption(Option
						.builder("b")
						.argName("no-bios")
						.longOpt("no-bios")
						.desc("do not boot the bios")
						.build()), args);
	}
}