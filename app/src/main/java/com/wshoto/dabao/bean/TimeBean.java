package com.wshoto.dabao.bean;

/**
 * Created by user on 2017/11/9.
 */

public class TimeBean {
    /**
     * errno : 1
     * message : 1512459164
     * data :
     */

    private int errno;
    private long message;
    private String data;

    public int getErrno() {
        return errno;
    }

    public void setErrno(int errno) {
        this.errno = errno;
    }

    public long getMessage() {
        return message;
    }

    public void setMessage(long message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
