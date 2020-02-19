# Osmocom IMSI Pseudonymization Project

Specification and reference SIM applet implementation to conceal the IMSI of a
mobile subscriber on the radio interface in a 2G, 3G, 4G network.

Homepage: https://osmocom.org/projects/imsi-pseudo/wiki

## How it works

The first pseudo IMSI gets allocated, as the SIM card is provisioned. After
that pseudo IMSI is used for the first time in location update, the HLR decides
the next pseudo IMSI and sends it as SMS to the SIM. The SIM applet overwrites
its current IMSI with the new one, and uses it in the next location update.
Afterwards, the HLR will generate the next IMSI and so on.

**FIXME:** details below need updating, see [OS#4400](https://osmocom.org/issues/4400).

```
HLR <-> SIM  LOCATION UPDATE, imsi_pseudo=200
HLR  -> SIM  NEW PSEUDO IMSI REQ, session_id=1, imsi_pseudo=123
HLR <-  SIM  NEW PSEUDO IMSI RESP ACK, session_id=1, imsi_pseudo=123
(time passes)
HLR <-> SIM  LOCATION UPDATE, imsi_pseudo=123
...
```

## In Detail

1. The HLR has a table of allocated pseudo IMSIs. When provisioning a new SIM,
it randomly decides a new pseudo IMSI. There must be no existing entry in the
table with the same pseudo IMSI in the imsi_pseudo column, but the pseudo IMSI
may be the real IMSI of an existing entry.

|   id |   imsi |   imsi_pseudo |   session_id |
|------|--------|---------------|--------------|
|    1 |   100  |   200         | 0            |

(Other interesting fields to store in the table may be a boolean for
"provisioned", the allocation date and usage count. The usage count would
increase whenever the SIM does a successful Location Update with that pseudo
IMSI.)

2. The SIM does a successful Location Update with its current pseudo IMSI.

(Clean up: if the ACK from the SIM card in step 4 did not arrive in a previous
 provisioning of a new pseudo IMSI, and the SIM has connected with the newer
 pseudo IMSI entry, the old pseudo IMSI entry gets deleted now.)

Then the HLR creates a new entry with a new pseudo IMSI (generated as described
in 1.), and with the session_id increased by one.

|   id |   imsi |   imsi_pseudo |   session_id |
|------|--------|---------------|--------------|
|    1 |   100  |   200         | 0            |
|    2 |   100  |   123         | 1            |

The new information is encoded in an SMS and sent to the SIM.

```
HLR  -> SIM  NEW PSEUDO IMSI REQ, session_id=1, imsi_pseudo=123
```

3. The SIM applet verifies, that the session_id is higher than the last
session_id it has seen (initially: 0). If that is not the case, it discards the
message.

The SIM applet writes the new pseudo IMSI and session_id to the SIM card,
overwriting the old data. It acknowledges the new data with a SMS back to the
HLR:

```
HLR <-  SIM  NEW PSEUDO IMSI RESP ACK, session_id=1, imsi_pseudo=123
```

4. The HLR verifies, that an entry with the session_id and imsi_pseudo from the
NEW PSEUDO IMSI RESP ACK message exists in the table. If not, it discards the
message.

HLR it deletes the old entry with the same IMSI (in the example: the one with
imsi_pseudo=200).

|   id |   imsi |   imsi_pseudo |   session_id |
|------|--------|---------------|--------------|
|    2 |   100  |   123         | 1            |

## Messages getting lost

### What if "NEW PSEUDO IMSI REQ" gets lost?

Both the old and the new pseudo IMSI entry exist in the HLR.

The SIM will use the old pseudo IMSI in the next location update. The HLR will
try to send _the same_ new pseudo IMSI with the same new session_id, as soon
as the next location update is complete.

### What if "NEW PSEUDO IMSI RESP" gets lost?

Both the old and the new pseudo IMSI entry exist in the HLR.

The SIM will use the new pseudo IMSI in the next location update. The HLR will
then clean up the old pseudo IMSI entry, and proceed with generating a new
pseudo IMSI entry and sending it to the SIM, as usually.

## Messages arriving late

### What if "NEW PSEUDO IMSI REQ" arrives late?

The session_id will not be higher than the session_id, which the SIM card
already knows. Therefore, the applet will discard the message.

### What if "NEW PSEUDO IMSI RESP" arrives late?

Session_id and imsi_pseudo from the message will not match what's in the HLR
database, so HLR will discard the message.

## Warning the user if SMS don't arrive

An attacker could possibly block the SMS with NEW PSEUDO IMSI REQ from arriving
at the SIM applet. In that case, the SIM would continue using the old pseudo
IMSI indefinitely.

We could possibly count the location updates done with the same pseudo IMSI in
the SIM applet, and warn the user if the same pseudo IMSI has been used more
than N (e.g. 5) times.

(Could be possible by listening to EVENT_DOWNLOAD_LOCATION_STATUS?)

## End2end encryption

When deploying the IMSI pseudonymization, the operator should make sure that
the pseudo IMSI related SMS between the HLR and the SIM cannot be read or
modified by third parties. Otherwise, the next pseudonymous IMSI is leaked, and
in case of modifying the IMSI in the SMS, the SIM may be locked out of the
network.

OTA SMS are usually encrypted and authenticated (TS 03.48), with algorithms and
key lengths that the operator chooses (depending on the SIM and how it is
configured).

It was considered to add an additional layer of end2end encryption for the
pseudonymized IMSIs on top, but this is out-of-scope for this project. For
reference, once could pre-provision a random "imsi_pseudo_key" with the SIM
card, store it in the pseudo IMSI table in the HLR, and deploy a new encryption
key together with each new pseudo IMSI, attached to the NEW PSEUDO IMSI REQ.
