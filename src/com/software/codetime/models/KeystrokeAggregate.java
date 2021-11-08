/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.models;

import swdc.java.ops.model.CodeTime;

//
public class KeystrokeAggregate {
    public int add = 0;
    public int close = 0;
    public int delete = 0;
    public int linesAdded = 0;
    public int linesRemoved = 0;
    public int open = 0;
    public int paste = 0;
    public int keystrokes = 0;
    public String directory = "";

    public void aggregate(CodeTime.FileInfo fileInfo) {
        this.add += fileInfo.add;
        this.keystrokes += fileInfo.keystrokes;
        this.paste += fileInfo.paste;
        this.open += fileInfo.open;
        this.close += fileInfo.close;
        this.delete += fileInfo.delete;
        this.linesAdded += fileInfo.linesAdded;
        this.linesRemoved += fileInfo.linesRemoved;
    }
}
