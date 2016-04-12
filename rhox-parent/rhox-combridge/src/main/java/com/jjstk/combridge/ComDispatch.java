/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge;

import com.sun.jna.platform.win32.COM.COMException;
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

    private final String desc;
    final Dispatcher dispatcher;

    /**
     * Creates a new JsComDispatch for the given name.
     *
     * @param name com object identifier
     */
    public ComDispatch(String name) {
        this(name, new Dispatcher(name));
    }

    public ComDispatch(String desc, Dispatcher dispatcher) {
        this.desc = desc;
        this.dispatcher = dispatcher;
    }

    /**
     * Gets the object value for the property with the given name.
     *
     * @param name name of the property to get
     * @return value of the call, will be simple java type or a ComDispatch if
     * it is a subobject
     */
    public Object get(String name) {
        try {
            DISPID id = dispatcher.getId(name);
            VARIANT v = dispatcher.get(id);
            return toResult(name, v);
        } catch (COMException ex) {
            throw newException(ex);
        }
    }

    /**
     * Sets the object value for the property with the given name.
     *
     * @param name name of the property to get
     * @param value value to set
     */
    public void set(String name, Object value) {
        try {
            DISPID id = dispatcher.getId(name);
            VARIANT vValue = Variants.to(value);
            dispatcher.set(id, vValue);
        } catch (COMException ex) {
            throw newException(ex);
        }
    }

    /**
     * Calls the method with the given name and parameters and returns the
     * result.
     *
     * @param name of the method
     * @param arguments for the call
     * @return value of the call, will be simple java type or a ComDispatch if
     * it is a subobject
     */
    public Object call(String name, Object... arguments) {
        try {
            DISPID id = dispatcher.getId(name);
            VARIANT[] vArgs = Variants.toArray(arguments);
            VARIANT v = dispatcher.call(id, vArgs);
            return toResult(name + "()", v);
        } catch (COMException ex) {
            throw newException(ex);
        }
    }

    private Object toResult(String name, VARIANT v) {
        Object o = Variants.from(v);
        if (o instanceof Dispatcher) {
            return new ComDispatch(desc + '.' + name, (Dispatcher) o);
        }
        return o;
    }

    private RuntimeException newException(COMException ex) {
        return Variants.newException(desc, ex);
    }
}
