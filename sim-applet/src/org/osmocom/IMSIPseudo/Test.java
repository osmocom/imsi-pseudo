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

	private static final String[] imsis = {
		"123456",
		"1234567",
		"12345678",
		"123456789",
		"1234567890",
		"12345678901",
		"123456789012",
		"1234567890123",
		"12345678901234",
		"123456789012345",
		"1234567890123456",
	};

	private static void test_str2mi2str()
	{
		for (int i = 0; i < imsis.length; i++) {
			byte str[] = imsis[i].getBytes();
			byte mi[] = MobileIdentity.str2mi(str, MobileIdentity.MI_IMSI, (byte)9);
			byte str_from_mi[] = MobileIdentity.mi2str(mi);
			System.out.print("IMSI " + new String(str) + " --> MI " + hexdumpStr(mi) + " --> IMSI "
					   + new String(str_from_mi));
			if (Bytes.equals(str, str_from_mi))
				System.out.println(" (ok)");
			else
				System.out.println(" ERROR!");
		}
	}

	private static void test_toStr()
	{
		byte nr = -128;
		while (true) {
			System.out.println("" + nr + " = '" + new String(Bytes.toStr(nr)) + "'");
			if (nr == 127)
				break;
			nr++;
		}
	}

	public static void main(String args[]){
		test_str2mi2str();
		test_toStr();
	}
}
