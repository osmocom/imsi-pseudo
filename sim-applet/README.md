# IMSI Pseudonymization SIM applet

### How to flash

```
$ cp .sim-keys.example .sim-keys
$ nvim .sim-keys # adjust KIC1, KID1
$ make flash
```

Before flashing a second time, remove the sim applet:

```
$ make remove
```

Related:
* [Shadysimply in Osmocom wiki](https://osmocom.org/projects/cellular-infrastructure/wiki/Shadysimpy)
