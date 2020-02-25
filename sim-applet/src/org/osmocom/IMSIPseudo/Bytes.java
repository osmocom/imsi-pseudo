/* Copyright 2020 sysmocom s.f.m.c. GmbH
 * SPDX-License-Identifier: Apache-2.0 */
package org.osmocom.IMSIPseudo;

public class Bytes {
	public static byte nibble2hex(byte nibble)
	{
		nibble = (byte)(nibble & 0xf);
		if (nibble < 0xa)
			return (byte)('0' + nibble);
		else
			return (byte)('a' + nibble - 0xa);
	}

	public static byte[] hexdump(byte data[])
	{
		byte res[] = new byte[(byte)(data.length*2)];
		for (byte i = 0; i < data.length; i++) {
			res[(byte)(i*2)] = nibble2hex((byte)(data[i] >> 4));
			res[(byte)(i*2 + 1)] = nibble2hex(data[i]);
		}
		return res;
	}

	public static boolean equals(byte a[], byte b[])
	{
		if (a.length != b.length)
			return false;
		for (short i = 0; i < (short)a.length; i++) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}

	public static boolean isDigit(byte digits[])
	{
		for (short i = 0; i < (short)digits.length; i++) {
			if (digits[i] < '0' || digits[i] > '9')
				return false;
		}
		return true;
	}

	public static byte[] toStr(byte byte_nr)
	{
		byte str[];
		short nr = byte_nr;
		byte d;
		byte l = 0;
		if (nr < 0) {
			l = 1;
			nr = (short)-nr;
		}

		if (nr > 99) {
			l += 3;
			d = 100;
		}
		else if (nr > 9) {
			l += 2;
			d = 10;
		}
		else {
			str = new byte[1];
			l += 1;
			d = 1;
		}

		byte i = 0;
		str = new byte[l];
		if (byte_nr < 0)
			str[i++] = '-';

		while (d > 0) {
			str[i++] = (byte)('0' + (nr / d));
			nr %= d;
			d /= 10;
		}
		return str;
	}
}
