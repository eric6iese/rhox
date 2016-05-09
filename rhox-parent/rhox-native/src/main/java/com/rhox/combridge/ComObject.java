/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rhox.combridge;

import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.OaIdl.DISPID;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.Variant.VARIANT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jdk.nashorn.api.scripting.AbstractJSObject;

/**
 * A Com-Node in a readable hierachy of values.<br/>
 * Note: This class is not threadsafe (which should be the default for most
 * com-objects anyway).
 */
@SuppressWarnings("restriction")
public final class ComObject extends AbstractJSObject {

    private final ComObject parent;
    /**
     * Name of this Element.
     */
    private final String name;

    private final Dispatcher dispatcher;

    /**
     * Creates a new JsComObject for the given com name.
     *
     * @param name the com name
     */
    public ComObject(String name) {
        this(null, name, new Dispatcher(name));
    }

    /**
     * Converts a ComDispatch into a ComObject.
     *
     * @param comDispatch which represents the initial state of this dispatch.
     */
    public ComObject(ComDispatch comDispatch) {
        this(null, comDispatch.getPath(), comDispatch.getDispatcher());
    }

    /**
     * Internal helper constructor.
     */
    ComObject(ComObject parent, String name, Dispatcher dispatcher) {
        this.parent = parent;
        this.name = Objects.requireNonNull(name, "Name must not be null");;
        this.dispatcher = dispatcher;
    }

    /**
     * Yields the internal dispatcher for package-friends.
     */
    Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * Re-creates the full qualified path of this ComObject.
     */
    String getPath() {
        List<String> path = new ArrayList<>();
        ComObject n = this;
        while (n != null) {
            path.add(n.name);
            n = n.parent;
        }
        Collections.reverse(path);
        return String.join(".", path);
    }

    @Override
    public String toString() {
        return "ComObject(" + getPath() + ")";
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == null || CharSequence.class.isAssignableFrom(hint)) {
            return toString();
        }
        return super.getDefaultValue(hint);
    }

    /**
     * Gets a property of this node.
     * <br/>
     * First, check if an element with this id exists. If it does not, throw an
     * exception. if it does, then read it. if this fails, then instead of
     * throwing an exception a new special sub-com-node is returned. this
     * comnode can be used to invoke a method all on the element later on.
     *
     * @param name member name
     * @return the member value
     */
    @Override
    public Object getMember(String name) {
        requireRealNode();

        // Get id FIRST - if this operation fails then no field/ method parsing is necessary
        DISPID id = getId(name);
        Variant.VARIANT v;
        try {
            v = dispatcher.get(id);
        } catch (COMException e) {
            return new ComObject(this, name, null);
        }
        int vtype = v.getVarType().intValue();
        if (vtype == Variant.VT_EMPTY) {
            return new ComObject(this, name, null);
        }
        Object o = Variants.from(v);
        if (o instanceof Dispatcher) {
            return new ComObject(this, name, (Dispatcher) o);
        }
        return o;
    }

    /**
     * Sets the property of this node.
     *
     * @param name member name
     * @param value member value
     */
    @Override
    public void setMember(String name, Object value) {
        requireRealNode();

        DISPID id = getId(name);
        VARIANT vValue = Variants.to(value);
        try {
            dispatcher.set(id, vValue);
        } catch (COMException e) {
            throw newException(e);
        }
    }

    @Override
    public Object call(Object thiz, Object... args) {
        return invoke(args);
    }

    /**
     * Invokes the method on the current method-special node.
     *
     * @param args the parameters, will be converted to variants
     * @return the result of the invocation
     */
    public Object invoke(Object... args) {
        if (parent == null) {
            throw new IllegalStateException("Cannot invoke " + name + Variants.toSignature(args) + " on the Root COM element!");
        }
        DISPID id = parent.getId(name);
        VARIANT[] vArgs = Variants.toArray(args);
        // If no dispatcher is present, this is treatened as a method call.
        // otherwise, it must be a property resolution
        boolean method = dispatcher == null;
        Variant.VARIANT v;
        try {
            v = parent.dispatcher.call(method, id, vArgs);
        } catch (COMException e) {
            throw newException(e);
        }
        Object o = Variants.from(v);
        if (o instanceof Dispatcher) {
            return new ComObject(parent, name + Variants.toSignature(args), (Dispatcher) o);
        }
        return o;
    }

    /**
     * Returns the Result of a property or method invocation. This can either be
     * value (string, int, etc) or another ComNode.
     */
    /**
     * Property getting and setting requires the current node to be 'valid',
     * meaning that it has a valid dispatcher.<br/>
     * This differs from method invocation, which only requires the parent and
     * current name to be valid.
     */
    private void requireRealNode() {
        if (dispatcher == null) {
            throw new UnsupportedOperationException(getPath() + " is not a valid ComNode!"
                    + " You can use it for method invocation, but you cannot resolve properties against it!");
        }
    }

    /**
     * Wraps a COM-specific exception and raises it with full node-information
     * as a runtime-exception.
     */
    private RuntimeException newException(COMException e) {
        return Variants.newException(getPath(), e);
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
