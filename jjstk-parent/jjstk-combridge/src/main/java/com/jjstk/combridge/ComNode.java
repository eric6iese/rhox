/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge;

import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.IDispatch;
import com.sun.jna.platform.win32.OaIdl;
import com.sun.jna.platform.win32.OaIdl.DISPID;
import com.sun.jna.platform.win32.Variant;

/**
 * Ein Knoten in einer Com-Hierarchie von Werten.
 */
public class ComNode {

    /**
     * Creates a new ComObject with the given name (or connects to it).
     */
    public static ComNode connect(String name) {
        return new ComNode(name, new Dispatcher(name), null);
    }

    private final String desc;
    private final Dispatcher dispatcher;
    private final DISPID dispId;

    private ComNode(String desc, Dispatcher dispatcher, DISPID dispId) {
        this.desc = desc;
        this.dispatcher = dispatcher;
        this.dispId = dispId;
    }

    /**
     * Interpretiert den aktuellen als Knoten und ruft ihn mit den Parametern
     * auf.
     */
    public Object invoke(Object... args) {
        VARIANT v;
        try {
            v = dispatcher.invoke(dispId, args);
        } catch (COMException e) {
            throw newException(e);
        }
        return toResult(desc + "()", v);
    }

    /**
     * Löst ein Sub-Member des aktuellen Knotens auf.<br/>
     * First, check if an element with this id exists. If it does not, throw an
     * exception. if it does, then read it. if this fails, then instead of
     * throwing an exception a new special sub-com-node is returned. this
     * comnode can be used to invoke a method all on the element later on.
     */
    public Object get(String name) {
        DISPID id;
        try {
            id = dispatcher.getId(name);
        } catch (COMException e) {
            throw newException(e);
        }
        VARIANT v;
        try {
            v = dispatcher.get(id);
        } catch (COMException e) {
            return new ComNode(desc + "." + name, dispatcher, id);
        }
        int type = v.getVarType().intValue();
        if (type == Variant.VT_EMPTY) {
            return new ComNode(desc + "." + name, dispatcher, id);
        }
        return toResult(name, v);
    }

    private Object toResult(String name, VARIANT v) {
        Object o = v.getValue();
        if (o instanceof IDispatch) {
            Dispatcher d = new Dispatcher((IDispatch) o);
            return new ComNode(name == null ? desc : desc + '.' + name, d, null);
        }
        return o;
    }

    /**
     * Setzt den Wert des Members neu.
     */
    public void set(String name, Object value) {
        try {
            DISPID id = dispatcher.getId(name);
            dispatcher.set(name, value);
        } catch (COMException e) {
            throw newException(e);
        }
    }

    @Override
    public String toString() {
        return "ComNode(" + desc + ")";
    }

    private RuntimeException newException(COMException e) {
        long error = getErrorCode(e);
        OaIdl.EXCEPINFO info = e.getExcepInfo();
        String source = info.bstrSource.getValue();
        String description = info.bstrDescription.getValue();
        return new UnsupportedOperationException(
                String.format("%s failed! ErrorCode: %s, Source: %s, Message: %s", desc, error, source, description));
    }

    long getErrorCode(COMException e) {
        OaIdl.EXCEPINFO info = e.getExcepInfo();
        long error = info.wCode.longValue();
        return error != 0L ? error : info.scode.longValue();
    }
}
