SIMTOOLS_DIR    = ../../sim-tools

APPLET_AID      = 0xd0:0x70:0x02:0xca:0x44:0x90:0x01:0x01
APPLET_NAME     = org.osmocom.IMSIPseudo.IMSIPseudo
PACKAGE_AID     = 0xd0:0x70:0x02:0xCA:0x44:0x90:0x01
PACKAGE_NAME    = org.osmocom.IMSIPseudo
PACKAGE_VERSION = 1.0

SOURCES = \
	  src/org/osmocom/IMSIPseudo/Bytes.java \
	  src/org/osmocom/IMSIPseudo/MobileIdentity.java \
	  src/org/osmocom/IMSIPseudo/IMSIPseudo.java \
	  $(NULL)

CAP_FILE = build/javacard/org/osmocom/IMSIPseudo/javacard/IMSIPseudo.cap

include ./applet-project.mk

flash: classes
	$(eval MODULE_AID   := $(shell echo $(APPLET_AID) | sed 's/0x//g' | sed 's/\://g'))
	$(eval INSTANCE_AID := $(shell echo $(APPLET_AID) | sed 's/0x//g' | sed 's/\://g'))
	. $$PWD/.sim-keys && $(SIMTOOLS_DIR)/bin/shadysim \
		--pcsc \
		-l $(CAP_FILE) \
		-i $(CAP_FILE) \
		--enable-sim-toolkit \
		--access-domain=00 \
		--module-aid $(MODULE_AID) \
		--instance-aid $(INSTANCE_AID) \
		--nonvolatile-memory-required 0100 \
		--volatile-memory-for-install 0100 \
		--max-menu-entry-text 21 \
		--max-menu-entries 01 \
		--kic "$$KIC1" \
		--kid "$$KID1"

remove:
	. $$PWD/.sim-keys && $(SIMTOOLS_DIR)/bin/shadysim \
		--pcsc \
		-d "$$(echo $(PACKAGE_AID) | sed 's/0x//g' | sed 's/\://g')" \
		--kic "$$KIC1" \
		--kid "$$KID1"

list:
	. $$PWD/.sim-keys && $(SIMTOOLS_DIR)/bin/shadysim \
		--pcsc \
		--list-applets \
		--kic "$$KIC1" \
		--kid "$$KID1"

delete: remove

.PHONY: test
test:
	mkdir -p ./test/classes
	javac -target 1.1 -source 1.3 -classpath test/classes -g -d ./test/classes src/org/osmocom/IMSIPseudo/Bytes.java
	javac -target 1.1 -source 1.3 -classpath test/classes -g -d ./test/classes src/org/osmocom/IMSIPseudo/MobileIdentity.java
	javac -target 1.1 -source 1.3 -classpath test/classes -g -d ./test/classes src/org/osmocom/IMSIPseudo/Test.java
	java -classpath test/classes org.osmocom.IMSIPseudo.Test
