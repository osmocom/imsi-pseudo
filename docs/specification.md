# [WIP] Make IMSI Pseudonymization an optional extension of 3GPP TS

Optional additions we need to make, and where to make them:

* Initial provisioning of the SIM: can optionally have a pseudo IMSI
* During location update, the HLR uses the pseudo IMSI for all communication
  with the VLR / MSC
* After successful location update:
  * HLR deallocates a subscriber's previous pseudo IMSI, if it exists, and the
    subscriber has done the location update with the newer pseudo IMSI entry.
    This is the case, if the SIM applet acknowledged the new pseudo IMSI, but
    its ACK SMS did not arrive at the HLR. There are at most two pseudo IMSIs
    allocated for one subscriber.
  * If there is just one pseudo IMSI for the subscriber (no new pseudo IMSI to
    switch to), the HLR allocates a new pseudo IMSI, and increases the
    session_id by one for that new pseudo IMSI, compared to the last pseudo
    IMSI.
  * The HLR sends the new pseudo IMSI, and the associated session_id, to the
    SIM via SMS. No matter, if the new pseudo IMSI was just created, or if it
    existed already.
  * The SIM applet checks, if the session_id is greater than the one that it
    has stored, and rejects the SMS otherwise. If the session_id is fine, it
    overwrites the SIM's IMSI and session_id with the new data. Then the SIM
    sends an ACK packet back to the HLR, containing both the new session_id and
    the new pseudo IMSI.
  * The HLR verifies the session_id and pseudo IMSI in the ACK packet, discards
    the packet if it doesn't know both. If it was not discarded, the HLR
    deallocates the old pseudo IMSI.
* When allocating and deallocating pseudo IMSIs, the HLR flushes information in
  the VLR related to them, so an old TMSI does not point to the wrong pseudo
  IMSI.
* The SIM applet registers EVENT_DOWNLOAD_LOCATION_STATUS, uses it to count the
  location updates that were done with the same pseudo IMSI, and warns the user
  if the pseudo IMSI did not change over several location updates. This means,
  that for some reason, the SMS from the HLR are not arriving (e.g. because an
  attacker is blocking them).

TODO:
* extend the list above with the exact sections of the spec, where the new
  information should be placed
* Is there a spec for SIM applets, or do we put the SIM applet behaviour in the
  regular spec for SIM cards, or mention its behavior in the location update
  related change?
* describe everything in detail, fill in the full contents for the SMS etc.
