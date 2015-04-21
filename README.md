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
You need to configure the appropiate phat to run a virtual base on your version of JDK, as a default
we seed a commun configuration path for java library but if you need to change the library path you must
to execute this command in a workspace:
```
"For mac with default configuration"
A4BPJVMConfiguration instance libraryFile:
  './System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Libraries/libclient.dylib'.

" For windows JNIPortJNIInterface libraryFile:"
A4BPJVMConfiguration instance libraryFile:
  'C:\Programs Files\Java\jre7\bin\client\jvm.dll'.
  
```
###When do you have a model loaded, just inspect the model and run:###
```
"return a json string with values"
A4BPJVMSimServer runSimulationModel: self. 
```
Or if you want to see in object using NJSON
```
"return a string with values parse by jsonreader"
NeoJSONReader fromString: (A4BPJVMSimServer runSimulationModel: self). 
```
