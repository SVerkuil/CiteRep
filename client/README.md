This directory contains all files associated with the CiteRep Java client

REQUIREMENTS
============
* Java Runtime Environment 1.7 or higher

DEPENDENCIES
============
CiteRep builds on other open source projects; the JAR files these projects
build into are included in the "lib" folder for convenience.

INSTALLATION
============
The following instructions use the Eclipse Java IDE

Create a new Java Project "CiteRep"

Within your project choose Import->General->File System
Point to the contents of this folder

Configure your project to include all jar files from /lib

Instructions for IntelliJ IDEA:
1. select new java project with client as source.
2. in the src file create a sub folder main and test.
3. in both these folders create a subfolder called java.
4. Put all packages (not the test packege) into the java foler and set the java folder as root directory.
5. put the test package in the test/java folder and set to test directory.

Run citerep.Run with these parameters:
- [url] = can be found on your server dashboard under the workers tab.
- [passphrase] = same als the URL
- [name ''?] = name of your worker
- [verbose 1-3 (1)?] = logging level for extra logging
