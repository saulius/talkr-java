# About

Simple event based plain text chat server written in java in the university. Server and client are also available in c++ at: http://github.com/sauliusg/talkr-cpp

# Usage

    cd talkr-server
    ./do.sh build
    java TalkrServer 9999

 
    cd talkr-client
    ./do.sh build
    java TalkrClient localhost 9999 client

# Commands

* SVERSION - returns server version.
* QUIT msg - disconnect from server.
* HANDSHAKE nickas - handshake with the server.
* PMSG user msg - sends private message msg to the user.
* CMSG chan msg - sends msg to the chan.
* JOIN chan - join channel chan.
* PART chan - part channel chan.

