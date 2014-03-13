# Patient Waiting Time Simulator

## Introduction

The **Patient Waiting Time Simulator** is an attempt to model the
effects of patient referrals and clinic appointment availability on the
patient waiting time for an out-patient department. The primary focus is on
showing the effect on breaching the maximum waiting time permitted.

The simulator uses a queue to simulate appointments for patient referrals,
that is on a first-come, first-served basis.

The simulator presentation consists of four panels:

  * Entry of simulation parameters (yellow)
  * Display of simulation result statistics (blue)
  * Control of the simulator (magenta)
  * Graphics representation for the display of simulation results and clinic slots

## Simulation Parameters

The parameters that can be specified are as follows:

### Periods

The defines the number of periods the simulation should run. Periods are
whatever time interval makes sense for the simulation you are running; they
could be hours, days, weeks...

### Referrals

This sets the average number of referrals to the clinic every period (.e.g
every week). This is used during the simulation run to add new patients to the
waiting list. If you change this value, be sure to click on the "Reset Slots"
button for the change to take effect.

### Patients Queued

The number of patients queued (i.e. on the waiting list) at the start of the
simulation run. If zero (the default) then there are no patients queued at the
beginning of the run. Queued patients are assumed to have been referred in the
past (before period 0). The number of patients referred per past period is
based on the # Referrals and Distribution settings.

### Max. Slots/Period

This defines the maximum number of clinic appointment slots per period you
intend to offer during the duration of the simulation. It is used to size the
display of slots and appointments in the display panel.

### Av.Slots/Period

This defines the average number of clinic slots per period for the duration of
the simulation run. The exact number of slots for each period can be set from
the display panel, by clicking on the appropriate location. For more details,
please see the description of the simulation results panel. If you change this
value, you will need to to click on the the "Reset Slots" button to effect the
change.

### Breach Threshold

This defines the maximum number of periods that a patient have to wait to
avoid breaking the waiting time threshold. Appointment slots are coloured to
indicate if an appointment breached the maximum waiting time (red) or was
within the limit (green).

### Distribution

This setting controls how many new patients are referred during any one
period. If Uniform, the every period has exactly the same number of new
patients referred, that is the the number of referrals you defined (see #
Referals). If set to Normal, then the number of new patients is randomised,
based on a normal distribution, using the number of Referals as the mean.
Therefore the number of patient referrals could be much higher, or lower, than
the the number of referrals you have set. This setting is probably more
realisitic than Uniform, but no more accurate. 

## Results

### Total # Patients Breached

This displays the total number of patients who were not seen within the breach
threshold at the end of the simulation run.

### Average Queue Time

This displays the average waiting time for all patients, both those breaching
the threshold and not.

### Total Unused Slots

This displays how many clinic slots were available but never actually used to
see a patient.

## Controls

### Show Slots

When clicked, the appointment slots defined for each period are displayed on
the graphics panel. These are shown as blue rectangles. The number of slots
available for any period can be modified by clocking on the graphical display
panel.

### Reset Slots

When clicked, this clears any changes you have made to the number of available
slots for specific periods and returns the number of slots for each period to
the average number of slots defined.

### Run

When clicked, this will run the simulation. Appointment slots are coloured
green for patients seen within the breach threshold, red for outside the
threshold and blue for an unused clinic slot.

### Clear

When clicked, this will clear the graphics display. The statistics while
remain until a new simulation is run.

### Import

This button does nothing (yet).

### Export

This button does nothing (yet).

## Graphics Panel

The graphics panel has two functions:

  1. Display of simulation results
  2. Display and editing of clinic appointment slots

The graphic panel is of a fixed size. If the number of slots and/or duration
will not fit into the panel display, scroll bars will appear to allow viewing
the whole of the results.

### Simulation Results

After clicking the Run button in the Controls section, the simulation results
are displayed in this area. Each period is numbered on the X-axis at the foot
of the panel starting from 0, which is the current period. Slots are number on
the left of the panel, the Y-axis. Positions with no appointment slot are
shown with a + character. Appointment slots are coloured green for patients
seen within the breach threshold, red for outside the threshold and blue for
an unused clinic slot.

### Editing Appointment Slots

By clicking the "Show Slots" button, the number of slots available will be
displayed, appearing as blue squares. The initial display is based on the
average number of slots you have defined. It is possible to change the number
of slots for any period by clicking on the appropriate location on the
graphics panel. The number of slots selected will be then be replicated into
the future until either the maximum duration is reached, or a previously
adjusted slot count is found, whichever is encounted first. It is probably
best to experiment with this to get a feel for the process (you can't do any
harm!).

You can reset the slots counts back to the default setting by clicking on
"Reset Slots". You will lose any changes you have made to the slot counts for
individual periods, so don't click this button unless you are sure you no
longer need the slot settings.

## Technical Details

The simulation is written in ClojureScript, using the HTML5 Canvas object for
the graohics display, and runs entirely in the browser. Not all browsers
support the Canvas object (although all recent ones should). If your browser
does not support the Canvas object, the simulation will refuse to run.

If the simulator will not run, I recommend upgrading to the latest version of
Chrome, Safari, Firefox, Opera or IE (9 or later).

## Usage

A presentation for the simulator can be found in ```pat/resources/public/pat.html```.  This hooks up the gui code at appropriate places.

## License

Copyright © 2013, 2014 Mark Willson

Distributed under the Eclipse Public License, the same as Clojure.
