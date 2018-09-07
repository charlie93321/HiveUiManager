package com.hbgj.entity;

public class Table {
    private String fname;
    private String ftype;
    private String fdesc;

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getFtype() {
        return ftype;
    }

    public void setFtype(String ftype) {
        this.ftype = ftype;
    }

    public String getFdesc() {
        return fdesc;
    }

    public void setFdesc(String fdesc) {
        this.fdesc = fdesc;
    }

    public Table(String fname, String ftype, String fdesc) {
        this.fname = fname;
        this.ftype = ftype;
        this.fdesc = fdesc;
    }

    @Override
    public String toString() {
        return "Table{" +
                "fname='" + fname + '\'' +
                ", ftype='" + ftype + '\'' +
                ", fdesc='" + fdesc + '\'' +
                '}';
    }
}
