package com.almejo.osom.cpu

import spock.lang.Specification

class ToSignedByteSpec extends Specification {

	def "toSignedByte returns positive values unchanged for 0 to 127"() {
		expect: "the value is unchanged"
		BitUtils.toSignedByte(unsignedValue) == expectedSigned

		where:
		unsignedValue | expectedSigned
		0             | 0
		1             | 1
		127           | 127
	}

	def "toSignedByte converts values 128-255 to negative range"() {
		expect: "the value is correctly converted to its two's complement negative"
		BitUtils.toSignedByte(unsignedValue) == expectedSigned

		where:
		unsignedValue | expectedSigned
		128           | -128
		200           | -56
		254           | -2
		255           | -1
	}
}
