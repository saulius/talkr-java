#!/bin/bash

case $1 in
	'run')
		java TalkrServer \9999;
		;;
	'build')
	    javac -Xlint:unchecked TalkrServer.java;
		;;
	'clean')
		find . -name \*.class -exec rm {} \;
		;;
	*)
		echo "Usage: $0 [build|clean|run]";
		;;
esac
