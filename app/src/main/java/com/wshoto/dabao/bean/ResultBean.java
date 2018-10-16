package com.wshoto.dabao.bean;

/**
 * Created by user on 2017/11/9.
 */

public class ResultBean {
    /**
     * errno : 1
     * message :
     * data : aaaasssdddffffeee
     */

    private int errno;
    private String message;
    private String data;

    public int getErrno() {
        return errno;
    }

    public void setErrno(int errno) {
        this.errno = errno;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
