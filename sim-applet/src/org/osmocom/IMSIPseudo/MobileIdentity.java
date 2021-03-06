/* Copyright 2020 sysmocom s.f.m.c. GmbH
 * SPDX-License-Identifier: Apache-2.0 */
package org.osmocom.IMSIPseudo;

public class MobileIdentity {
	public static final byte MI_IMSI = 1;

	/* Convert BCD-encoded digit into printable character
	 *  \param[in] bcd A single BCD-encoded digit
	 *  \returns single printable character
	 */
	public static byte bcd2char(byte bcd)
	{
		if (bcd < 0xa)
			return (byte)('0' + bcd);
		else
			return (byte)('A' + (bcd - 0xa));
	}

	/* Convert BCD to string.
	 * The given nibble offsets are interpreted in BCD order, i.e. nibble 0 is bcd[0] & 0xf, nibble 1 is bcd[0] >> 4, nibble
	 * 3 is bcd[1] & 0xf, etc..
	 *  \param[out] dst  Output byte array.
	 *  \param[in] dst_ofs  Where to start writing in dst.
	 *  \param[in] dst_len  How many bytes are available at dst_ofs.
	 *  \param[in] bcd  Binary coded data buffer.
	 *  \param[in] start_nibble  Offset to start from, in nibbles.
	 *  \param[in] end_nibble  Offset to stop before, in nibbles.
	 *  \param[in] allow_hex  If false, return false if there are digits other than 0-9.
	 *  \returns true on success, false otherwise
	 */
	public static boolean bcd2str(byte dst[], byte dst_ofs, byte dst_len,
				      byte bcd[], byte start_nibble, byte end_nibble, boolean allow_hex)
	{
		byte nibble_i;
		byte dst_i = dst_ofs;
		byte dst_end = (byte)(dst_ofs + dst_len);
		boolean rc = true;

		for (nibble_i = start_nibble; nibble_i < end_nibble && dst_i < dst_end; nibble_i++, dst_i++) {
			byte nibble = bcd[(byte)nibble_i >> 1];
			if ((nibble_i & 1) != 0)
				nibble >>= 4;
			nibble &= 0xf;

			if (!allow_hex && nibble > 9)
				rc = false;

			dst[dst_i] = bcd2char(nibble);
		}

		return rc;
	}

	public static byte[] mi2str(byte mi[])
	{
		/* The IMSI byte array by example:
		 * 08 99 10 07 00 00 10 74 90
		 *
		 * This is encoded according to 3GPP TS 24.008 10.5.1.4 Mobile
		 * Identity, short the Mobile Identity IEI:
		 *
		 * 08 length for the following MI, in bytes.
		 *  9 = 0b1001
		 *	1 = odd nr of digits
		 *	 001 = MI type = IMSI
		 * 9  first IMSI digit (BCD)
		 *  0 second digit
		 * 1  third
		 * ...
		 *  0 14th digit
		 * 9  15th and last digit
		 *
		 * If the IMSI had an even number of digits:
		 *
		 * 08 98 10 07 00 00 10 74 f0
		 *
		 * 08 length for the following MI, in bytes.
		 *  8 = 0b0001
		 *	0 = even nr of digits
		 *	 001 = MI type = IMSI
		 * 9  first IMSI digit
		 *  0 second digit
		 * 1  third
		 * ...
		 *  0 14th and last digit
		 * f  filler
		 */
		byte bytelen = mi[0];
		byte mi_type = (byte)(mi[1] & 0xf);
		boolean odd_nr_of_digits = ((mi_type & 0x08) != 0);
		byte start_nibble = 2 + 1; // 2 to skip the bytelen, 1 to skip the mi_type
		byte end_nibble = (byte)(2 + bytelen * 2 - (odd_nr_of_digits ? 0 : 1));
		byte str[] = new byte[end_nibble - start_nibble];
		bcd2str(str, (byte)0, (byte)str.length, mi, start_nibble, end_nibble, true);
		return str;
	}

	public static byte char2bcd(byte c)
	{
		if (c >= '0' && c <= '9')
			return (byte)(c - '0');
		else if (c >= 'A' && c <= 'F')
			return (byte)(0xa + (c - 'A'));
		else if (c >= 'a' && c <= 'f')
			return (byte)(0xa + (c - 'a'));
		else
			return 0;
	}

	public static byte[] str2mi(byte str[], byte mi_type, byte min_buflen)
	{
		boolean odd_digits = ((str.length & 1) != 0);
		/* 1 nibble of mi_type.
		 * str.length nibbles of MI BCD.
		 */
		byte mi_nibbles = (byte)(1 + str.length);
		byte mi_bytes = (byte)(mi_nibbles / 2 + ((mi_nibbles & 1) != 0? 1 : 0));
		/* 1 byte of total MI length in bytes, plus the MI nibbles */
		byte buflen = (byte)(1 + mi_bytes);
		/* Fill up with 0xff to the requested buffer size */
		if (buflen < min_buflen)
			buflen = min_buflen;
		byte buf[] = new byte[buflen];

		for (byte i = 0; i < buf.length; i++)
			buf[i] = (byte)0xff;

		/* 1 byte of following MI length in bytes */
		buf[0] = mi_bytes;

		/* first MI byte: low nibble has the MI type and odd/even indicator bit,
		 * high nibble has the first BCD digit.
		 */
		mi_type = (byte)(mi_type & 0x07);
		if (odd_digits)
			mi_type |= 0x08;
		buf[1] = (byte)((char2bcd(str[0]) << 4) + mi_type);

		/* fill in the remaining MI nibbles */
		byte str_i = 1;
		for (byte mi_i = 1; mi_i < mi_bytes; mi_i++) {
			byte data = char2bcd(str[str_i++]);
			if (str_i < str.length)
				data |= char2bcd(str[str_i++]) << 4;
			else
				data |= 0xf0;
			buf[1 + mi_i] = data;
		}
		return buf;
	}
}
