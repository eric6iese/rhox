/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.natives.win32;

import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.OaIdl.DISPID;
import com.sun.jna.platform.win32.Variant.VARIANT;
import java.util.Objects;

/**
 * An alternative to the JsComObject is the ComDispatch.<br/>
 * This class is less elegant but it allows for more explicit control over the
 * input and output calling methods.<br/>
 * Should be used when the JsComObject cannot be used, for example if the
 * (experimental) protocol it uses for method resolution does not work in some
 * cases.
 */
public final class ComDispatch {

    private final String path;
    private final Dispatcher dispatcher;

    /**
     * Creates a new ComDispatch for the given name.
     *
     * @param progId the com progId
     */
    public ComDispatch(String progId) {
        this(progId, new Dispatcher(progId));
    }

    /**
     * Converts a ComObject into a comdispatch.
     *
     * @param comObject which represents the initial state of this dispatch.
     */
    public ComDispatch(ComObject comObject) {
        this(comObject.getPath(), comObject.getDispatcher());
    }

    /**
     * Creates a new ComObject with the given Dispatcher.
     */
    ComDispatch(String path, Dispatcher dispatcher) {
        this.path = Objects.requireNonNull(path, "Path must not be null");
        this.dispatcher = Objects.requireNonNull(dispatcher, "Dispatcher must not be null");
    }

    /**
     * Yields the internal dispatcher for package-friends.
     */
    Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * Yields the current comPath the Dispatch is in
     */
    String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "ComDispatch(" + getPath() + ")";
    }

    /**
     * Gets the object value for the property with the given name.
     *
     * @param name name of the property to get
     * @return value of the call, will be simple java type or a ComDispatch if
     * it is a subobject
     */
    public Object get(String name) {
        DISPID id = getId(name);
        VARIANT v;
        try {
            v = dispatcher.get(id);
        } catch (COMException ex) {
            throw newException(ex);
        }
        Object o = Variants.from(v);
        if (o instanceof Dispatcher) {
            return new ComDispatch(path + '.' + name, (Dispatcher) o);
        }
        return o;
    }

    /**
     * Calls the Property getter with the given name and parameters and returns
     * the result.
     *
     * @param name of the property
     * @param arguments for the call
     * @return value of the call, will be simple java type or a ComDispatch if
     * it is a subobject
     */
    public Object get(String name, Object... arguments) {
        return call(false, name, arguments);
    }

    /**
     * Sets the object value for the property with the given name.
     *
     * @param name name of the property to get
     * @param value value to set
     */
    public void set(String name, Object value) {
        DISPID id = getId(name);
        VARIANT vValue = Variants.to(value);
        try {
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
        return call(true, name, arguments);
    }

    private Object call(boolean method, String name, Object... arguments) {
        DISPID id = getId(name);
        VARIANT[] vArgs = Variants.toArray(arguments);
        VARIANT v;
        try {
            v = dispatcher.call(method, id, vArgs);
        } catch (COMException ex) {
            throw newException(ex);
        }
        Object o = Variants.from(v);
        if (o instanceof Dispatcher) {
            return new ComDispatch(path + '.' + name + Variants.toSignature(arguments), (Dispatcher) o);
        }
        return o;
    }

    private RuntimeException newException(COMException ex) {
        return Variants.newException(path, ex);
    }

    /**
     * Gets the display id for the given name or creates a new one.
     */
    private DISPID getId(String name) {
        try {
            return dispatcher.getId(name);
        } catch (COMException e) {
            throw newException(e);
        }
    }
}
