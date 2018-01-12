package com.almejo.osom;

import org.apache.commons.cli.*;

import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException {

		CommandLine commandLine;
		try {
			commandLine = parseCommandArguments(args);
		} catch (ParseException exp) {
			System.err.println("Error: " + exp.getMessage());
			System.exit(0);
			return;
		}

		boolean bios = !commandLine.hasOption("no-bios");
		String filename = commandLine.getOptionValue("rom");

//		System.out.println("Running emulator");
//		System.out.println("Bios enabled: " + bios);
//		System.out.println("Rom filename: " + filename);
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