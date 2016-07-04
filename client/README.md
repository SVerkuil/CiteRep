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

Run citerep.Run
Parameters: [url] [passphrase] [name ''?] [verbose 1-3 (1)?]