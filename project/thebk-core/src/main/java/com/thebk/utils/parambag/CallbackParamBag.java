package com.thebk.utils.parambag;

public abstract class CallbackParamBag<Subclass extends ParamBag> extends ParamBag<Subclass> {
    public abstract void callback();
}
