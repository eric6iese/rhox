/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge;

import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.COM.COMException;

/**
 * Ein Knoten in einer Com-Hierarchie von Werten.
 */
public class ComNode {

    /**
     * Baut die Verbindung zum angegebenen Com-Object auf.
     */
    public static ComNode connect(String name) {
        return new ComNode(name, new Dispatcher(name), null);
    }

    private final String desc;
    private Object dispatch;
    private final String member;

    private ComNode(String desc, Object dispatch, String member) {
        this.desc = desc;
        this.dispatch = dispatch;
        this.member = member;
    }

    private Dispatcher dispatch() {
        if (dispatch instanceof VARIANT) {
            dispatch = new Dispatcher((VARIANT) dispatch);
        }
        return (Dispatcher) dispatch;
    }

    private VARIANT get() {
        try {
            return dispatch().get(member);
        } catch (COMException e) {
            throw newException(e);
        }
    }

    private Dispatcher getDispatchMember() {
        if (member == null) {
            return dispatch();
        }
        VARIANT v = get();
        return new Dispatcher(v);
    }

    /**
     * Interpretiert den aktuellen als Knoten und ruft ihn mit den Parametern
     * auf.
     */
    public ComNode invoke(Object... args) {
        Dispatcher d = dispatch();
        VARIANT v;
        try {
            v = d.invoke(member, args);
        } catch (COMException e) {
            throw newException(e);
        }
        return new ComNode(desc + "()", v, null);
    }

    /**
     * Löst ein Sub-Member des aktuellen Knotens auf.
     */
    public ComNode get(String name) {
        Dispatcher d = getDispatchMember();
        return new ComNode(desc + "." + name, d, name);
    }

    /**
     * Setzt den Wert des Members neu.
     */
    public void set(String name, Object value) {
        Dispatcher d = getDispatchMember();
        try {
            d.set(name, value);
        } catch (COMException e) {
            throw newException(e);
        }
    }

    /**
     * Liefert den Java-Wert des Com-Noten zurück.
     */
    public Object value() {
        if (dispatch instanceof VARIANT) {
            return ((VARIANT) dispatch).getValue();
        } else if (member == null) {
            return dispatch.toString();
        }
        VARIANT v = get();
        return v.getValue();
    }

    @Override
    public String toString() {
        return "ComNode(" + desc + ")";
    }

    private RuntimeException newException(COMException e) {
        String data = e.getExcepInfo().toString();
        return new UnsupportedOperationException(desc + " failed! data = " + data, e);
    }
}
