package com.almejo.osom

import groovy.io.FileType
import spock.lang.Specification

class CorePresentationSeparationSpec extends Specification {

	private static final String SOURCE_ROOT = "src/main/java/com/almejo/osom"
	private static final List<String> CORE_PACKAGES = ["cpu", "memory", "gpu"]
	private static final List<String> PROHIBITED_IMPORT_PATTERNS = ["javax.swing", "java.awt"]

	def "core packages have no Swing/AWT imports"() {
		given: "all Java source files in core packages"
		def violations = []

		when: "scanning for prohibited imports"
		CORE_PACKAGES.each { packageName ->
			def packageDir = new File("${SOURCE_ROOT}/${packageName}")
			if (packageDir.exists()) {
				packageDir.eachFileRecurse(FileType.FILES) { file ->
					if (file.name.endsWith(".java")) {
						def content = file.text
						PROHIBITED_IMPORT_PATTERNS.each { pattern ->
							if (content.contains(pattern)) {
								violations << "${file.name} imports ${pattern}"
							}
						}
					}
				}
			}
		}

		then: "no Swing or AWT imports found in core packages"
		violations == []
	}

	def "Emulator.java has no Swing/AWT imports"() {
		given: "the Emulator.java source file"
		def emulatorFile = new File("${SOURCE_ROOT}/Emulator.java")
		def violations = []

		when: "scanning for prohibited imports"
		def content = emulatorFile.text
		PROHIBITED_IMPORT_PATTERNS.each { pattern ->
			if (content.contains(pattern)) {
				violations << "Emulator.java imports ${pattern}"
			}
		}

		then: "no Swing or AWT imports found"
		violations == []
	}
}
