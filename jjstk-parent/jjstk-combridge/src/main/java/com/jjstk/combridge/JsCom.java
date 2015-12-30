/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge;

import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.IDispatch;
import com.sun.jna.platform.win32.OaIdl;
import com.sun.jna.platform.win32.Variant;
import java.util.HashMap;
import java.util.Map;
import jdk.nashorn.api.scripting.AbstractJSObject;

/**
 * A Com-Node in a readable hierachy of values.<br/>
 * Note: This class is not threadsafe (which should be the default for most
 * com-objects anyway).
 */
@SuppressWarnings("restriction")
public class JsCom extends AbstractJSObject {

    /**
     * Creates a new ComObject with the given name (or connects to it).
     */
    public static JsCom connect(String name) {
        return new JsCom(name, new Dispatcher(name));
    }

    private final String desc;
    private final Dispatcher dispatcher;
    /**
     * Internal Cache which stores all display ids (and types) after an element
     * has been resolved.
     */
    private final Map<String, ComField> fieldCache;
    private final String method;

    private JsCom(String desc, Dispatcher dispatcher) {
        this(desc, dispatcher, new HashMap<>(), null);
    }

    private JsCom(String desc, Dispatcher dispatcher, Map<String, ComField> fieldCache, String method) {
        this.desc = desc;
        this.dispatcher = dispatcher;
        this.fieldCache = fieldCache;
        this.method = method;
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
        ComField field = getId(name, ComField.Type.FIELD);
        if (field.isType(ComField.Type.METHOD)) {
            return methodNode(name);
        }
        Variant.VARIANT v;
        try {
            v = dispatcher.get(field.id);
        } catch (COMException e) {
            return methodNode(name);
        }
        int type = v.getVarType().intValue();
        if (type == Variant.VT_EMPTY) {
            return methodNode(name);
        }
        Object result = toResult(name, v);
        putToCache(field);
        return result;
    }

    /**
     * Creates a subinstance specially designed for invocations.
     */
    private JsCom methodNode(String name) {
        return new JsCom(desc + "." + name, dispatcher, fieldCache, name);
    }

    /**
     * Sets the property of this node.
     *
     * @param name member name
     * @param value member value
     */
    @Override
    public void setMember(String name, Object value) {
        ComField field = getId(name, ComField.Type.FIELD);
        field.requireType(ComField.Type.FIELD);
        try {
            dispatcher.set(field.id, value);
        } catch (COMException e) {
            throw newException(e);
        }
        putToCache(field);
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
        ComField field = getId(method, ComField.Type.METHOD);
        field.requireType(ComField.Type.METHOD);
        Variant.VARIANT v;
        try {
            v = dispatcher.invoke(field.id, args);
        } catch (COMException e) {
            throw newException(e);
        }
        Object result = toResult(desc + "()", v);
        putToCache(field);
        return result;
    }

    @Override
    public String toString() {
        return "ComNode(" + desc + ")";
    }

    private Object toResult(String name, Variant.VARIANT v) {
        Object o = v.getValue();
        if (o instanceof IDispatch) {
            Dispatcher dispatchResult = new Dispatcher((IDispatch) o);
            return new JsCom(name == null ? desc : desc + '.' + name, dispatchResult);
        }
        return o;
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

    /**
     * Caches the id and and type, if they have not already been cached.
     */
    private void putToCache(ComField field) {
        fieldCache.putIfAbsent(field.name, field);
    }

    /**
     * Gets the display id for the given name or creates a new one.
     */
    private ComField getId(String name, ComField.Type type) {
        ComField field = fieldCache.get(name);
        if (field != null) {
            return field;
        }
        OaIdl.DISPID id;
        try {
            id = dispatcher.getId(name);
        } catch (COMException e) {
            throw newException(e);
        }
        return new ComField(name, id, type);
    }
}
