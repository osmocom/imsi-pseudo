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
		return;
	}

	private void showError(short code) {
		byte[] msg = new byte[] {'E', '?', '?'};
		msg[1] = (byte)('0' + code / 10);
		msg[2] = (byte)('0' + code % 10);
		showMsg(msg);
	}

	private void showIMSI() {
		/* 3GPP TS 31.102 4.2.2: IMSI */
		byte[] IMSI = new byte[9];
		byte[] msg = {'C', 'u', 'r', 'r', 'e', 'n', 't', ' ', 'I', 'M', 'S', 'I', ':', ' ',
			      '_', '_', '_', '_', '_', '_', '_', '_', '_', '_', '_', '_', '_', '_', '_'};

		gsmFile.select((short) SIMView.FID_DF_GSM);
		gsmFile.select((short) SIMView.FID_EF_IMSI);

		try {
			gsmFile.readBinary((short)0, IMSI, (short)0, (short)9);
		} catch (SIMViewException e) {
			showError(e.getReason());
		}

		for (byte i = (byte)0; i < (byte)15; i++) {
			byte msg_i = (byte)(14 + i);
			if (i >= IMSI[0]) {
				msg[msg_i] = ' ';
			} else if (i % (byte)2 == (byte)0) {
				msg[msg_i] = (byte)('0' + (IMSI[i / (byte)2] & 0x0f));
			} else {
				msg[msg_i] = (byte)('0' + (IMSI[i / (byte)2] >>> 4));
			}
			showMsg(msg); /* DEBUG */
		}
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
