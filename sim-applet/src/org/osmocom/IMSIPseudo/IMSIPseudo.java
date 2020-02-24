package org.osmocom.IMSIPseudo;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISOException;

import sim.toolkit.EnvelopeHandler;
import sim.toolkit.ProactiveHandler;
import sim.toolkit.ProactiveResponseHandler;
import sim.toolkit.ToolkitConstants;
import sim.toolkit.ToolkitException;
import sim.toolkit.ToolkitInterface;
import sim.toolkit.ToolkitRegistry;

public class IMSIPseudo extends Applet implements ToolkitInterface, ToolkitConstants {
	// DON'T DECLARE USELESS INSTANCE VARIABLES! They get saved to the EEPROM,
	// which has a limited number of write cycles.

	private byte STKServicesMenuId;
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

	private void showMsg(byte[] msg) {
		ProactiveHandler proHdlr = ProactiveHandler.getTheHandler();
		proHdlr.initDisplayText((byte)0, DCS_8_BIT_DATA, msg, (short)0, (short)(msg.length));
		proHdlr.send();
		return;
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

	private void handleMenuResponseMain() {
		ProactiveResponseHandler rspHdlr = ProactiveResponseHandler.getTheHandler();

		switch (rspHdlr.getItemIdentifier()) {
			case 1: /* Show LU counter */
				showMsg(LUCounter);
				break;
			case 2: /* Show IMSI */
				/* TODO */
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
