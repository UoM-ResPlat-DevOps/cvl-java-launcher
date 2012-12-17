
timestamp is generated from timestamp.in by TurboVNC's build system (cmake).

1. Check out the latest TurboVNC source code.
2. cd java
3. cmake .
4. The timestamp file should be generated in com/turbovnc/vncviewer/

The Java Launcher does not generate this file automatically yet, but since
it is required by the TurboVNC Viewer java code, I generate it once (as above)
each time I check out a new version of the TurboVNC source code, and then copy it
into the src/main/java/com/turbovnc/vncviewer/ directory.  The Apache Ant 
build.xml file provides instructions for Apache Ant to copy the timestamp file
into build/com/turbovnc/vncviewer/ 
