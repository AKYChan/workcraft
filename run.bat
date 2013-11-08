set WORKCRAFT_HOME=%~dp0

set PATH=%PATH%;%WORKCRAFT_HOME%\tools

set CLASSPATH=.\Workcraft\bin;^
.\ThirdParty\batik\batik-anim.jar;^
.\ThirdParty\batik\batik-awt-util.jar;^
.\ThirdParty\batik\batik-bridge.jar;^
.\ThirdParty\batik\batik-codec.jar;^
.\ThirdParty\batik\batik-css.jar;^
.\ThirdParty\batik\batik-dom.jar;^
.\ThirdParty\batik\batik-ext.jar;^
.\ThirdParty\batik\batik-extension.jar;^
.\ThirdParty\batik\batik-gui-util.jar;^
.\ThirdParty\batik\batik-gvt.jar;^
.\ThirdParty\batik\batik-parser.jar;^
.\ThirdParty\batik\batik-script.jar;^
.\ThirdParty\batik\batik-svg-dom.jar;^
.\ThirdParty\batik\batik-svggen.jar;^
.\ThirdParty\batik\batik-swing.jar;^
.\ThirdParty\batik\batik-transcoder.jar;^
.\ThirdParty\batik\batik-util.jar;^
.\ThirdParty\batik\batik-xml.jar;^
.\ThirdParty\batik\xml-apis-ext.jar;^
.\ThirdParty\batik\xml-apis.jar;^
.\ThirdParty\commons-logging-1.1.3.jar;^
.\ThirdParty\flexdock-0.5.1.jar;^
.\ThirdParty\jackson-core-asl-1.8.10.jar;^
.\ThirdParty\jackson-mapper-asl-1.8.10.jar;^
.\ThirdParty\javaparser-1.0.7.jar;^
.\ThirdParty\jedit.jar;^
.\ThirdParty\jga-0.8-lgpl.jar;^
.\ThirdParty\js.jar;^
.\ThirdParty\junit-4.5.jar;^
.\ThirdParty\log4j-1.2.8.jar;^
.\ThirdParty\pcollections-1.0.0.jar;^
.\ThirdParty\substance.jar;^
.\ThirdParty\TableLayout-bin-jdk1.5-2009-08-26.jar;^
.\CircuitPlugin\bin;^
.\CpogsPlugin\bin;^
.\DfsPlugin\bin;^
.\GatesPlugin\bin;^
.\GraphPlugin\bin;^
.\MpsatPlugin\bin;^
.\PetrifyPlugin\bin;^
.\PetriNetPlugin\bin;^
.\PolicyNetPlugin\bin;^
.\SONPlugin\bin;^
.\STGPlugin\bin;^
.\XmasPlugin\bin;^
;

javaw.exe -Dfile.encoding=Cp1252 -classpath %CLASSPATH% org.workcraft.Console
