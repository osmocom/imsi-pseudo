package org.osmocom.IMSIPseudo;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISOException;

import sim.toolkit.EnvelopeHandler;
import sim.toolkit.ProactiveHandler;
import sim.toolkit.ToolkitConstants;
import sim.toolkit.ToolkitException;
import sim.toolkit.ToolkitInterface;
import sim.toolkit.ToolkitRegistry;

public class IMSIPseudo extends Applet implements ToolkitInterface, ToolkitConstants {
	// DON'T DECLARE USELESS INSTANCE VARIABLES! They get saved to the EEPROM,
	// which has a limited number of write cycles.
	private byte helloMenuItem;

	static byte[] welcomeMsg = new byte[] { 'H', 'e', 'l', 'l', 'o', ',', ' ',
						'W', 'o', 'r', 'l', 'd', '!' };
	static byte[] menuItemText = new byte[] { 'I', 'M', 'S', 'I', ' ', 'P', 's', 'e', 'u', 'd', 'o', 'n', 'y', 'm',
						  'i', 'z', 'a', 't', 'i', 'o', 'n'};

	private IMSIPseudo() {
		// This is the interface to the STK applet registry (which is separate
		// from the JavaCard applet registry!)
		ToolkitRegistry reg = ToolkitRegistry.getEntry();

		// Define the applet Menu Entry
		helloMenuItem = reg.initMenuEntry(menuItemText, (short)0, (short)menuItemText.length,
				PRO_CMD_SELECT_ITEM, false, (byte)0, (short)0);
		reg.setEvent(EVENT_EVENT_DOWNLOAD_LOCATION_STATUS);
	}

	// This method is called by the card when the applet is installed. You must
	// instantiate your applet and register it here.
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		IMSIPseudo applet = new IMSIPseudo();

		applet.register();
	}

	// This processes APDUs sent directly to the applet. For STK applets, this
	// interface isn't really used.
	public void process(APDU arg0) throws ISOException {
		// ignore the applet select command dispached to the process
		if (selectingApplet())
			return;
	}

	// This processes STK events.
	public void processToolkit(byte event) throws ToolkitException {
		EnvelopeHandler envHdlr = EnvelopeHandler.getTheHandler();

		if (event == EVENT_MENU_SELECTION) {
			byte selectedItemId = envHdlr.getItemIdentifier();

			if (selectedItemId == helloMenuItem) {
				showHello();
			}
		}

		if (event == EVENT_EVENT_DOWNLOAD_LOCATION_STATUS) {
			/* TODO: count the location updates done with the same
			 * pseudo IMSI, and warn the user if it becomes too
			 * high */
			showHello();
		}
	}

	private void showHello() {
		ProactiveHandler proHdlr = ProactiveHandler.getTheHandler();
		proHdlr.initDisplayText((byte)0, DCS_8_BIT_DATA, welcomeMsg, (short)0,
				(short)(welcomeMsg.length));
		proHdlr.send();
		return;
	}
}
