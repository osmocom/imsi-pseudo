# [WIP] Make IMSI Pseudonymization an optional extension of 3GPP TS

NOTE: this file is deprecated, it will be replaced by imsi-pseudo-spec.adoc.

Relevant specs:
* 3GPP TS 23.008: Organization of subscriber data
  * Add pseudo IMSI and pseudo_imsi_i optionally to be saved in the HLR
* "Process Update_Location_HLR" of TS 09.02
  * Cancel location in old VLR/SGSN if IMSI pseudonymization is enabled, and a
    previous pseudonymous IMSI exists for the subscriber

Optional additions we need to make, and where to make them:

* Initial provisioning of the SIM: can optionally have a pseudo IMSI
* During location update, the HLR uses the pseudo IMSI for all communication
  with the VLR / MSC
  * Is there anything to update? We just replace the IMSI, so the SIM and the
    VLR / MSC don't act any different
* After successful location update:
  * See 2. in README.md

TODO:
* extend the list above with the exact sections of the spec, where the new
  information should be placed
* Is there a spec for SIM applets, or do we put the SIM applet behaviour in the
  regular spec for SIM cards, or mention its behavior in the location update
  related change?
* describe everything in detail, fill in the full contents for the SMS etc.
