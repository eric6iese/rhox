/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge;

import com.sun.jna.platform.win32.OaIdl.DISPID;
import com.sun.jna.platform.win32.Variant.VARIANT;

/**
 * An alternative to the JsComObject is the ComDispatch.<br/>
 * This class is less elegant but it allows for more explicit control over the
 * input and output calling methods.<br/>
 * Should be used when the JsComObject cannot be used, for example if the
 * (experimental) protocol it uses for method resolution does not work in some
 * cases.
 */
public class ComDispatch {

    private final Dispatcher dispatcher;

    /**
     * Creates a new JsComDispatch for the given name.
     *
     * @param name com object identifier
     */
    public ComDispatch(String name) {
        dispatcher = new Dispatcher(name);
    }

    public Object get(String member) {
        DISPID id = dispatcher.getId(member);
        VARIANT v = dispatcher.get(id);
        return v.getValue();
    }

    public void set(String member, Object value) {
        DISPID id = dispatcher.getId(member);
        dispatcher.set(id, value);
    }

    public Object invoke(String method, Object... args) {
        DISPID id = dispatcher.getId(method);
        VARIANT v = dispatcher.invoke(id, args);
        return v.getValue();
    }
}
