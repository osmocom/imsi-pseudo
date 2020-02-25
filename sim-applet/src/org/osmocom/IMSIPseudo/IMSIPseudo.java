/* Copyright 2020 sysmocom s.f.m.c. GmbH
 * SPDX-License-Identifier: Apache-2.0 */
package org.osmocom.IMSIPseudo;
import org.osmocom.IMSIPseudo.MobileIdentity;

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
	private static final byte[] title = new byte[] { 'I', 'M', 'S', 'I', ' ', 'P', 's', 'e', 'u', 'd', 'o', 'n', 'y', 'm',
					   'i', 'z', 'a', 't', 'i', 'o', 'n'};
	private static final byte[] showLU = new byte[] {'S', 'h', 'o', 'w', ' ', 'L', 'U', ' ', 'c', 'o', 'u', 'n', 't', 'e', 'r'};
	private static final byte[] showIMSI = new byte[] {'S', 'h', 'o', 'w', ' ', 'I', 'M', 'S', 'I'};
	private static final byte[] changeIMSI = new byte[] {'C', 'h', 'a', 'n', 'g', 'e', ' ', 'I', 'M', 'S', 'I', ' '};
	private final Object[] itemListMain = {title, showLU, showIMSI, changeIMSI};

	/* Change IMSI menu */
	private static final byte[] enterIMSI = new byte[] {'E', 'n', 't', 'e', 'r', ' ', 'I', 'M', 'S', 'I' };
	private static final byte[] setDigit1 = new byte[] {'S', 'e', 't', ' ', '1', ' ', 'a', 's', ' ', 'l', 'a', 's', 't', ' ',
						  'd', 'i', 'g', 'i', 't'};
	private static final byte[] setDigit2 = new byte[] {'S', 'e', 't', ' ', '2', ' ', 'a', 's', ' ', 'l', 'a', 's', 't', ' ',
						  'd', 'i', 'g', 'i', 't'};
	private final Object[] itemListChangeIMSI = {changeIMSI, enterIMSI, setDigit1, setDigit2};

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
				showMenu(itemListMain);
				handleMenuResponseMain();
			}
		}

		if (event == EVENT_EVENT_DOWNLOAD_LOCATION_STATUS) {
			LUCounter[0]++;
			showMsg(LUCounter);
		}
	}

	private void showMenu(Object[] itemList) {
		ProactiveHandler proHdlr = ProactiveHandler.getTheHandler();
		proHdlr.init((byte) PRO_CMD_SELECT_ITEM,(byte)0,DEV_ID_ME);

		for (byte i=(byte)0; i < itemList.length; i++) {
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

	private byte[] getResponse()
	{
		ProactiveResponseHandler rspHdlr = ProactiveResponseHandler.getTheHandler();
		byte[] resp = new byte[rspHdlr.getTextStringLength()];
		rspHdlr.copyTextString(resp, (short)0);
		return resp;
	}

	/*
	This was used to find out that the first byte of a text field seems to be 4.
	private byte[] getResponseDBG()
	{
		ProactiveResponseHandler rspHdlr;
		byte resp[];
		byte strlen = -1;
		rspHdlr = ProactiveResponseHandler.getTheHandler();

		for (byte occurence = 1; occurence <= 3; occurence++) {
			short len;
			try {
				if (rspHdlr.findTLV(TAG_TEXT_STRING, (byte)occurence) != TLV_NOT_FOUND) {
					if ((len = rspHdlr.getValueLength()) > 1) {
						len = 3;
						resp = new byte[len];
						rspHdlr.copyValue((short)0, resp, (short)0, (short)(len));
						showMsg(resp);
						showMsgAndWaitKey(Bytes.hexdump(resp));
						return resp;
					}
				}
			} catch (Exception e) {
				showError((short)(30 + occurence));
				return null;
			}
		}
		showError((short)(39));
		return null;
	}
	*/

	private byte[] showMsgAndWaitKey(byte[] msg) {
		ProactiveHandler proHdlr = ProactiveHandler.getTheHandler();
		proHdlr.initGetInkey((byte)0, DCS_8_BIT_DATA, msg, (short)0, (short)(msg.length));
		proHdlr.send();

		return getResponse();
	}

	private byte[] prompt(byte[] msg, byte[] prefillVal, short minLen, short maxLen) {
		/* if maxLen < 1, the applet crashes */
		if (maxLen < 1)
			maxLen = 1;

		ProactiveHandler proHdlr = ProactiveHandler.getTheHandler();
		proHdlr.initGetInput((byte)0, DCS_8_BIT_DATA, msg, (short)0, (short)(msg.length), minLen, maxLen);
		if (prefillVal != null && prefillVal.length > 0) {
			/* appendTLV() expects the first byte to be some header before the actual text.
			 * At first I thought it was the value's length, but turned out to only work for lengths under 8...
			 * In the end I reversed the value 4 from the first byte read by rspHdlr.copyValue() for
			 * TAG_TEXT_STRING fields. As long as we write 4 into the first byte, things just work out,
			 * apparently.
			 * Fucking well could have said so in the API docs, too; oh the brain damage, oh the hours wasted.
			 * This is the appendTLV() variant that writes one byte ahead of writing an array: */
			proHdlr.appendTLV((byte)(TAG_DEFAULT_TEXT), (byte)4, prefillVal, (short)0,
					  (short)(prefillVal.length));
		}
		proHdlr.send();

		return getResponse();
	}

	private void showError(short code) {
		byte[] msg = new byte[] {'E', '?', '?'};
		msg[1] = (byte)('0' + code / 10);
		msg[2] = (byte)('0' + code % 10);
		showMsg(msg);
	}


	private void showIMSI() {
		/* 3GPP TS 31.102 4.2.2: IMSI */
		byte[] msg = {'C', 'u', 'r', 'r', 'e', 'n', 't', ' ', 'I', 'M', 'S', 'I', ':', ' ',
			      ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};

		try {
			byte IMSI[] = readIMSI();
			MobileIdentity.mi2str(msg, (byte)14, (byte)16, IMSI, false);
			showMsgAndWaitKey(msg);
		} catch (SIMViewException e) {
			showError(e.getReason());
		}
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
			showMenu(itemListChangeIMSI);
			handleMenuResponseChangeIMSI();
			break;
		}
	}

	private void handleMenuResponseChangeIMSI() {
		ProactiveResponseHandler rspHdlr = ProactiveResponseHandler.getTheHandler();
		switch (rspHdlr.getItemIdentifier()) {
		case 1: /* enter IMSI */
			promptIMSI();
			break;
		case 2: /* set last digit to 1 */
			promptIMSI();
			break;
		case 3: /* set last digit to 2 */
			promptIMSI();
			break;
		}
	}

	private void promptIMSI()
	{
		byte[] msg = {'N', 'e', 'w', ' ', 'I', 'M', 'S', 'I', '?'};
		byte imsi[] = prompt(msg, (short)0, (short)15);
		/* The IMSI file should be 9 bytes long, even if the IMSI is shorter */
		byte mi[];
		try {
			mi = MobileIdentity.str2mi(imsi, MobileIdentity.MI_IMSI, (byte)9);
			writeIMSI(mi);
		} catch (Exception e) {
			byte err[] = {'E', 'R', 'R' };
			showMsgAndWaitKey(err);
		}
	}

	private byte[] readIMSI()
	{
		gsmFile.select((short) SIMView.FID_DF_GSM);
		gsmFile.select((short) SIMView.FID_EF_IMSI);
		byte[] IMSI = new byte[9];
		gsmFile.readBinary((short)0, IMSI, (short)0, (short)9);
		return IMSI;
	}

	private void writeIMSI(byte mi[])
	{
		gsmFile.select((short) SIMView.FID_DF_GSM);
		gsmFile.select((short) SIMView.FID_EF_IMSI);
		gsmFile.updateBinary((short)0, mi, (short)0, (short)mi.length);
	}
}
