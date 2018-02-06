# Toto-Sound-Player
The song toto is inserted into a 10 second buffer, using a producer consumer format. There is no downtime when the song is playing  

The applet comes with a text area for entering commands.

*x := End song 
*p := Pause
*r := Resume
*m := Mute
*u := Unmute
*q := Raise volume
*a := Lower volume

All commands are fully functional.
Any wav file can be used instead, just change the file name in the progra.  

To compile the applet:
$ javac StudentPlayerApplet.java

To launch the applet:
$ appletviewer -J"-Djava.security.policy=all.policy" toto.wav
