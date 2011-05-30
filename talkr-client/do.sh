#!/bin/bash

case $1 in
	'build')
	    javac -Xlint:unchecked TalkrClient.java;
		;;
	'clean')
		find . -name \*.class -exec rm {} \;
		;;
	*)
		echo "Usage: $0 [build|clean]";
		;;
esac
