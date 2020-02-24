/* Copyright 2020 sysmocom s.f.m.c. GmbH
 * SPDX-License-Identifier: Apache-2.0 */
package org.osmocom.IMSIPseudo;
import org.osmocom.IMSIPseudo.*;

public class Test {
	private static byte nibble2hex(byte nibble)
	{
		nibble = (byte)(nibble & 0xf);
		if (nibble < 0xa)
			return (byte)('0' + nibble);
		else
			return (byte)('a' + nibble - 0xa);
	}

	private static byte[] hexdump(byte data[])
	{
		byte res[] = new byte[(byte)(data.length*2)];
		for (byte i = 0; i < data.length; i++) {
			res[(byte)(i*2)] = nibble2hex((byte)(data[i] >> 4));
			res[(byte)(i*2 + 1)] = nibble2hex(data[i]);
		}
		return res;
	}

	private static String hexdumpStr(byte data[])
	{
		return new String(hexdump(data));
	}

	public static void main(String args[]){
		System.out.println(hexdumpStr(MobileIdentity.str2mi("123456".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("1234567".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("12345678".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("123456789".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("1234567890".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("12345678901".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("123456789012".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("1234567890123".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("12345678901234".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("123456789012345".getBytes(), (byte)1, (byte)9)));
		System.out.println(hexdumpStr(MobileIdentity.str2mi("1234567890123456".getBytes(), (byte)1, (byte)9)));
	}
}
