README
Authors: Cameron Kadre, Gail Kinder, Spencer Ford
Class:   cs441
Date:    Sep. 28, 2025
Assignment: Project 1

Included Files:
S12_Sim.java
S12_IL.java
S12_IL_Interface.java
simple_test

Compile/Run:
> javac *.java
> java S12_Sim simple_test

We started first with reading files with initializeMem(). In here we're parsing mem files and setting
the board, filling memory. We used an int array to hold memory and used int everywhere for convienice.
To per clock tick one has to call update(). This holds the instruction logic. It's a big switch, based on
the opcode. Perform simple one step tack/update/advance. This is called 100000 times or until it hits
halt opcode. We use bit arithmetic/logic to create masks to isolate the correct bits and convert to to hex.
We use hex to express values that humans should see and binary for the computer. The S12_Sim is set up to run
one test at a time from one file at a time. Simple checks and flags as requested by the assignment. Repeat update()
until the sim is halted or maxCycles reached. Output to stdout and mem/trace files populated. 