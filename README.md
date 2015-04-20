#A4BP-BPSimJbpmSimulator#
##How to install BPSim Simulator##

###Clone this repository in your local pc###
```
git clone https://github.com/ajperalt/A4BP-BPSimJbpmSimulator.git
```
###Donwload the code from smalltalkhub###
```
Gofer new smalltalkhubUser: 'ajperalt' project: 'A4BP'; package: 'ConfigurationOfA4BP'; load. 
(Smalltalk at: #ConfigurationOfA4BP) loadDevelopment.
```
###Download JDK1.6 from java repository###

For Mac [here](https://support.apple.com/downloads/DL1572/en_US/JavaForOSX2014-001.dmg)
For others [here](http://www.oracle.com/technetwork/java/javaee/downloads/java-ee-sdk-6u3-jdk-6u29-downloads-523388.html)

###Configure the machine to run java###
```
A4BPJVMConfiguration >> sp
  "file separator using in the OSSystem
	^ '/'
A4BPJVMConfiguration >> psp
  "file separator using by the jvm in the class path"
  
  "On Mac OS X and Linux, class path entries are separated by colons. eg ^ ':'"
  "On Windows, you have to use semicolons instead. Edit the following line as needed. eg ^ ';'"
	^ ':'
A4BPJVMConfiguration >> libraryFile
  "for mac with default configuration"
  ^ './System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Libraries/libclient.dylib'.
  " for windows JNIPortJNIInterface libraryFile:"
  "^ 'C:\Programs Files\Java\jre7\bin\client\jvm.dll'."
A4BPJVMConfiguration >> basePathDir
  "/Users/peralta/A4BP_LIB"
	^ self sp,'Users',self sp,'peralta',self sp,'A4BP_LIB'
```
###When do you have a model loaded, just inspect the model and run:###
```
A4BPJVMSimServer runSimulationModel: self. "return a json string with values"
NeoJSONReader fromString: (A4BPJVMSimServer runSimulationModel: self) "return a string with values parse by jsonreader"
```
