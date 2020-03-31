# Osmocom IMSI Pseudonymization Project

Specification and reference SIM applet implementation to conceal the IMSI of a
mobile subscriber on the radio interface in a 2G, 3G, 4G network.

Homepage: https://osmocom.org/projects/imsi-pseudo/wiki

## How it works

The first pseudo IMSI gets allocated in the HLR, as the SIM card is
provisioned. After that pseudo IMSI is used for the first time in location
update, the HLR waits for some time, then decides the next pseudo IMSI and
sends it together with a delay value as SMS to the SIM. The SIM applet receives
the SMS and waits the specified delay. Then it overwrites its current IMSI with
the new one, marks the TMSI as invalid, and initiates the next location update.
Afterwards, the process repeats.

```
HLR <-> SIM  LOCATION UPDATE, imsi_pseudo=200
(time passes)
HLR  -> SIM  NEW PSEUDO IMSI, imsi_pseudo=123, delay=60
(time passes until the SMS arrives)
(SIM applet waits 60 seconds)
HLR <-> SIM  LOCATION UPDATE, imsi_pseudo=123
...
```

## In Detail

### 1. Provisioning the SIM

The HLR allocates a new pseudo IMSI as random choice from the pool of available
IMSIs. The pseudo IMSI must not be used by any other subscriber as pseudo IMSI,
but may be the real IMSI of another subscriber. The subscriber-specific counter
imsi_pseudo_i is 0 for the first allocated IMSI for that subscriber.

|   id |   imsi |   imsi_pseudo | imsi_pseudo_i |
|------|--------|---------------|---------------|
|    1 |   100  |   200         | 0             |

The pseudo IMSI is saved to the SIM as IMSI, instead of the real IMSI. The SIM
is also provisioned with the IMSI pseudonymization applet.

### 2. Successful Location Update with pseudo IMSI

a) If this was the first Location Update after provisioning the SIM, the
subscriber has only one pseudo IMSI allocated. The HLR waits for some time.
Then it allocates the next pseudo IMSI from the pool of available IMSIs (as in
1., but with imsi_pseudo_i increased by one). The HLR sends the new
pseudo IMSI, the imsi_pseudo_i and a random delay value in one SMS to the SIM.

The random delay is how long the SIM applet should wait before changing the
IMSI. This delay prevents easy correlation of the arrival of the SMS with the
Location Update that will follow in 3. by the SIM. Due to other latencies in
the network, this is a minimum delay. At this point, the subscriber has two
allocated pseudo IMSIs:

|   id |   imsi |   imsi_pseudo | imsi_pseudo_i |
|------|--------|---------------|---------------|
|    1 |   100  |   200         | 0             |
|    2 |   100  |   123         | 1             |

b) If this was not the first Location Update after provisioning a new SIM, the
subscriber already has two pseudo IMSIs allocated when doing the Location
Update. The HLR compares imsi_pseudo_i to find out if the Location Update was
done with the newer or older pseudo IMSI.

If the older pseudo IMSI was used, then the SIM applet was not able to set the
new IMSI. This may be caused by an SMS arriving late, possibly even months
after it was sent in case the UE was without power for a long period of time.
Therefore the HLR cannot deallocate the newer pseudo IMSI without risking that
the SIM would configure that IMSI and then be locked out (unable to do any
further location updates). Instead, the HLR proceeds like in a), but sends the
same unused new pseudo IMSI again instead of allocating a new one.

If the newer pseudo IMSI was used, the SIM applet has successfully set the new
IMSI. The HLR deallocates the old pseudo IMSI and sends a Purge MS request to
the VLR with the old pseudo IMSI. Then the HLR proceeds like in a).

### 3. Arrival of the SMS

The SIM applet verifies, that imsi_pseudo_i is higher than the last
imsi_pseudo_i it has seen (initially: 0). If that is not the case, it discards
the message.

The SIM applet registers a timer to wait the specified delay. When the timer
expires, the applet updates the last imsi_pseudo_i value that it has seen. Then
it overwrites the IMSI with the next pseudo IMSI and invalidates the TMSI and
Kc. The applet triggers a refresh, which causes the SIM to do a new Location
Update with the new IMSI.

### What if the SMS gets lost?

Both the old and the new pseudo IMSI entry exist in the HLR.

The SIM will use the old pseudo IMSI in the next Location Update. The HLR will
try to send _the same_ new pseudo IMSI with the same new imsi_pseudo_i, as soon
as the next Location Update is complete.

### What if the SMS arrives late?

The imsi_pseudo_i counter will not be higher than the value the SIM applet
already knows. Therefore, the applet will discard the message.

### Warning the user if SMS don't arrive

An attacker could possibly block the SMS from arriving at the SIM applet. In
that case, the SIM would continue using the old pseudo IMSI indefinitely.

We can count the location updates done with the same pseudo IMSI in the SIM
applet, and warn the user if the same pseudo IMSI has been used more than N
(e.g. 5) times.

### End2end encryption

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
reference, one could pre-provision a random "imsi_pseudo_key" with the SIM
card, store it in the pseudo IMSI table in the HLR, and deploy a new encryption
key together with each new pseudo IMSI, attached to the SMS.

### User-configurable minimum duration between IMSI changes

It may be desirable to let users configure their minimum duration between IMSI
changes. This allows people with a high privacy requirement to switch their
pseudonymous IMSI more often, and it allows the IMSI change to happen less
often if it is distracting to the user. The latter depends on the phone's
software, for example:
* A Samsung GT-I9100 Galaxy SII smartphone with Android 4.0.3 displays a
  message at the bottom of the screen for about 5 seconds, but the user
  interface remains usable.
* A Samsung GT-E1200 feature phone displays a waiting screen for 16 to 17
  seconds and is unusable during that time.
