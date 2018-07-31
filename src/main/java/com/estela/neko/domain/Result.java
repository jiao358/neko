package com.estela.neko.domain;

/**
 * @author fuming.lj 2018/7/31
 **/
public class Result<T> {

    private T model;

    private boolean success ;

    private String errorMsg;


    public Result(){
        success=true;

    }
    public T getModel() {
        return model;
    }

    public void setModel(T model) {
        this.model = model;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
