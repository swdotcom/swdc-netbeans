/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.models;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class KeystrokeMetrics {
    public static final Logger log = Logger.getLogger("KeystrokeMetrics");
    
    private int add = 0;
    private int open = 0;
    private int close = 0;
    private int paste = 0;
    private int delete = 0;
    private int length = 0;
    private int netkeys = 0;
    private int lines = 0;
    private int linesAdded = 0;
    private int linesRemoved = 0;
    private String syntax = "";
    private long start = 0L;
    private long end = 0L;
    private long local_start = 0L;
    private long local_end = 0L;

    
    public KeystrokeMetrics() {
        //
    }

    public int getAdd() {
        return add;
    }

    public void setAdd(int add) {
        log.log(Level.INFO, "Code Time: incrementing add");
        this.add = add;
    }

    public int getOpen() {
        return open;
    }

    public void setOpen(int open) {
        log.log(Level.INFO, "Code Time: incrementing open");
        this.open = open;
    }

    public int getClose() {
        return close;
    }

    public void setClose(int close) {
        log.log(Level.INFO, "Code Time: incrementing close");
        this.close = close;
    }

    public int getPaste() {
        return paste;
    }

    public void setPaste(int paste) {
        log.log(Level.INFO, "Code Time: incrementing paste");
        this.paste = paste;
    }

    public int getDelete() {
        return delete;
    }

    public void setDelete(int delete) {
        log.log(Level.INFO, "Code Time: incrementing delete");
        this.delete = delete;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        log.log(Level.INFO, "Code Time: setting length");
        this.length = length;
    }

    public int getNetkeys() {
        return netkeys;
    }

    public void setNetkeys(int netkeys) {
        log.log(Level.INFO, "Code Time: incrementing netkeys");
        this.netkeys = netkeys;
    }

    public int getLines() {
        return lines;
    }

    public void setLines(int lines) {
        log.log(Level.INFO, "Code Time: updating lines");
        this.lines = lines;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        log.log(Level.INFO, "Code Time: incrementing lines added");
        this.linesAdded = linesAdded;
    }

    public int getLinesRemoved() {
        return linesRemoved;
    }

    public void setLinesRemoved(int linesRemoved) {
        log.log(Level.INFO, "Code Time: incrementing lines removed");
        this.linesRemoved = linesRemoved;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getLocal_start() {
        return local_start;
    }

    public void setLocal_start(long local_start) {
        this.local_start = local_start;
    }

    public long getLocal_end() {
        return local_end;
    }

    public void setLocal_end(long local_end) {
        this.local_end = local_end;
    }
    
    
    public boolean hasData() {
        return (this.add > 0 || this.open > 0 ||
                this.close > 0 || this.paste > 0 ||
                this.delete > 0 || this.netkeys > 0 ||
                this.linesAdded > 0 || this.linesRemoved > 0);
    }
}
