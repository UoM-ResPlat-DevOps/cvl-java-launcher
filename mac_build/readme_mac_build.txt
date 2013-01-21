After building with ant from the root directory containing build.xml,
copy the MassiveLauncher={DATE_STAMP}.jar from dist/lib/
into mac_build/MASSIVE\ Launcher\ \(Java\).app/Contents/Resources/Java/
and remove the date stamp (call it "MassiveLauncher.jar")

The copy the 3rd-party JARs from lib/ into the same directory, i.e.
mac_build/MASSIVE\ Launcher\ \(Java\).app/Contents/Resources/Java/

The file mac_build/MASSIVE\ Launcher\ \(Java\).app/Contents/Info.plist
specifies the main class (au.org.massive.launcher.Launcher), the
minimum Java Virtual Machine (JVM) version required (1.6), the class path,
and the options for the JVM's startup.

    <key>Java</key>
    <dict>
        <key>MainClass</key>
        <string>au.org.massive.launcher.Launcher</string>
        <key>JVMVersion</key>
        <string>1.6+</string>
        <key>ClassPath</key>
        <string>$JAVAROOT/MassiveLauncher.jar:$JAVAROOT/jgoodies-common-1.4.0.jar:$JAVAROOT/ws-commons-util-1.0.2.jar:$JAVAROOT/xmlrpc-common-3.1.3.jar:$JAVAROOT/commons-logging-1.1.jar:$JAVAROOT/jgoodies-forms-1.6.0.jar:$JAVAROOT/xmlrpc-client-3.1.3.jar:$JAVAROOT/xmlrpc-server-3.1.3.jar</string>
        <key>VMOptions</key>
        <string>-server -d64 -Djava.library.path=$APP_PACKAGE/Contents/Resources/TurboJPEG</string>
    </dict>

Once the Application has been built, it can be packaged in a disk image (DMG), following instructions here:

https://confluence-vre.its.monash.edu.au/display/CVL/MASSIVE-CVL+Launcher+Mac+OS+X+build+instructions

See "Creating the MASSIVE Launcher.dmg disk image".


