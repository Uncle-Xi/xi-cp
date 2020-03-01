package com.xicp;

public interface OpCode {
    int notification = 0;

    int create = 1;

    int delete = 2;

    int exists = 3;

    int getData = 4;

    int setData = 5;

    int getACL = 6;

    int setACL = 7;

    int getChildren = 8;

    int sync = 9;

    int ping = 11;

    int getChildren2 = 12;

    int check = 13;

    int multi = 14;

    int auth = 100;

    int setWatches = 101;

    int sasl = 102;

    int createSession = -10;

    int closeSession = -11;

    int error = -1;
}
