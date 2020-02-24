/* Copyright 2020 sysmocom s.f.m.c. GmbH
 * SPDX-License-Identifier: Apache-2.0 */
package org.osmocom.IMSIPseudo;

import sim.access.*;
import sim.toolkit.*;
import javacard.framework.*;

public class IMSIPseudo extends Applet implements ToolkitInterface, ToolkitConstants {
	// DON'T DECLARE USELESS INSTANCE VARIABLES! They get saved to the EEPROM,
	// which has a limited number of write cycles.

	private byte STKServicesMenuId;
	private SIMView gsmFile;
	static byte[] LUCounter = new byte[] { '0', 'x', ' ', 'L', 'U' };

	/* Main menu */
	static byte[] title = new byte[] { 'I', 'M', 'S', 'I', ' ', 'P', 's', 'e', 'u', 'd', 'o', 'n', 'y', 'm',
					   'i', 'z', 'a', 't', 'i', 'o', 'n'};
	static byte[] showLU = new byte[] {'S', 'h', 'o', 'w', ' ', 'L', 'U', ' ', 'c', 'o', 'u', 'n', 't', 'e', 'r'};
	static byte[] showIMSI = new byte[] {'S', 'h', 'o', 'w', ' ', 'I', 'M', 'S', 'I'};
	static byte[] changeIMSI = new byte[] {'C', 'h', 'a', 'n', 'g', 'e', ' ', 'I', 'M', 'S', 'I', ' '};
	private Object[] itemListMain = {title, showLU, showIMSI, changeIMSI};

	/* Change IMSI menu */
	static byte[] setDigit1 = new byte[] {'S', 'e', 't', ' ', '1', ' ', 'a', 's', ' ', 'l', 'a', 's', 't', ' ',
						  'd', 'i', 'g', 'i', 't'};
	static byte[] setDigit2 = new byte[] {'S', 'e', 't', ' ', '2', ' ', 'a', 's', ' ', 'l', 'a', 's', 't', ' ',
						  'd', 'i', 'g', 'i', 't'};
	private Object[] itemListChangeIMSI = {changeIMSI, setDigit1, setDigit2};

	private IMSIPseudo() {
		gsmFile = SIMSystem.getTheSIMView();

		/* Register menu and trigger on location updates */
		ToolkitRegistry reg = ToolkitRegistry.getEntry();
		STKServicesMenuId = reg.initMenuEntry(title, (short)0, (short)title.length, PRO_CMD_SELECT_ITEM, false,
						 (byte)0, (short)0);
		reg.setEvent(EVENT_EVENT_DOWNLOAD_LOCATION_STATUS);
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		IMSIPseudo applet = new IMSIPseudo();
		applet.register();
	}

	public void process(APDU arg0) throws ISOException {
		if (selectingApplet())
			return;
	}

	public void processToolkit(byte event) throws ToolkitException {
		EnvelopeHandler envHdlr = EnvelopeHandler.getTheHandler();

		if (event == EVENT_MENU_SELECTION) {
			byte selectedItemId = envHdlr.getItemIdentifier();

			if (selectedItemId == STKServicesMenuId) {
				showMenu(itemListMain, (byte)4);
				handleMenuResponseMain();
			}
		}

		if (event == EVENT_EVENT_DOWNLOAD_LOCATION_STATUS) {
			LUCounter[0]++;
			showMsg(LUCounter);
		}
	}

	private void showMenu(Object[] itemList, byte itemCount) {
		ProactiveHandler proHdlr = ProactiveHandler.getTheHandler();
		proHdlr.init((byte) PRO_CMD_SELECT_ITEM,(byte)0,DEV_ID_ME);

		for (byte i=(byte)0;i<itemCount;i++) {
			if (i == 0) {
				/* Title */
				proHdlr.appendTLV((byte)(TAG_ALPHA_IDENTIFIER | TAG_SET_CR), (byte[])itemList[i],
						  (short)0, (short)((byte[])itemList[i]).length);

			} else {
				/* Menu entry */
				proHdlr.appendTLV((byte)(TAG_ITEM | TAG_SET_CR), (byte)i, (byte[])itemList[i], (short)0,
						  (short)((byte[])itemList[i]).length);
			}
		}
		proHdlr.send();
	}

	private void showMsg(byte[] msg) {
		ProactiveHandler proHdlr = ProactiveHandler.getTheHandler();
		proHdlr.initDisplayText((byte)0, DCS_8_BIT_DATA, msg, (short)0, (short)(msg.length));
		proHdlr.send();
	}

	private void showError(short code) {
		byte[] msg = new byte[] {'E', '?', '?'};
		msg[1] = (byte)('0' + code / 10);
		msg[2] = (byte)('0' + code % 10);
		showMsg(msg);
	}

	/* Convert BCD-encoded digit into printable character
	 *  \param[in] bcd A single BCD-encoded digit
	 *  \returns single printable character
	 */
	private byte bcd2char(byte bcd)
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
	private boolean bcd2str(byte dst[], byte dst_ofs, byte dst_len,
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

	private boolean mi2str(byte dst[], byte dst_ofs, byte dst_len,
			       byte mi[], boolean allow_hex)
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
		return bcd2str(dst, dst_ofs, dst_len, mi, start_nibble, end_nibble, allow_hex);
	}

	private void showIMSI() {
		/* 3GPP TS 31.102 4.2.2: IMSI */
		byte[] IMSI = new byte[9];
		byte[] msg = {'C', 'u', 'r', 'r', 'e', 'n', 't', ' ', 'I', 'M', 'S', 'I', ':', ' ',
			      ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};

		gsmFile.select((short) SIMView.FID_DF_GSM);
		gsmFile.select((short) SIMView.FID_EF_IMSI);

		try {
			gsmFile.readBinary((short)0, IMSI, (short)0, (short)9);
		} catch (SIMViewException e) {
			showError(e.getReason());
			return;
		}

		mi2str(msg, (byte)14, (byte)16, IMSI, false);
		showMsg(msg);
	}

	private void handleMenuResponseMain() {
		ProactiveResponseHandler rspHdlr = ProactiveResponseHandler.getTheHandler();

		switch (rspHdlr.getItemIdentifier()) {
			case 1: /* Show LU counter */
				showMsg(LUCounter);
				break;
			case 2: /* Show IMSI */
				showIMSI();
				break;
			case 3: /* Change IMSI */
				showMenu(itemListChangeIMSI, (byte)3);
				handleMenuResponseChangeIMSI();
				break;
		}
	}

	private void handleMenuResponseChangeIMSI() {
		/* TODO */
	}
}
