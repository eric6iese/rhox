/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.evermind.scriptmaster.jacob;

import com.jacob.com.ComFailException;
import com.jacob.com.Dispatch;
import com.jacob.com.JacobException;
import com.jacob.com.Variant;
import jdk.nashorn.api.scripting.AbstractJSObject;

/**
 *
 * @author giese
 */
public class ComJsObject extends AbstractJSObject {

    static {
        JacobLoader.initialize();
    }

    private final String desc;
    private Object dispatch;
    private final String member;

    ComJsObject(String desc, Object dispatch, String member) {
        this.desc = desc;
        this.dispatch = dispatch;
        this.member = member;
    }

    private Dispatch dispatch() {
        if (dispatch instanceof Variant) {
            dispatch = ((Variant) dispatch).toDispatch();
        }
        return (Dispatch) dispatch;
    }

    private Variant get() {
        try {
            return Dispatch.get(dispatch(), member);
        } catch (JacobException e) {
            throw new UnsupportedOperationException(desc + " failed!", e);
        }
    }

    private Dispatch getDispatchMember() {
        if (member == null) {
            return dispatch();
        }
        Variant v = get();
        return v.toDispatch();
    }

    @Override
    public Object call(Object thiz, Object... args) {
        ComJsObject obj = (ComJsObject) this;
        Variant[] vars = new Variant[args.length];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = var(args[i]);
        }
        Variant v;
        try {
            v = Dispatch.callN(dispatch(), member, (Object[]) vars);
        } catch (JacobException e) {
            throw new UnsupportedOperationException(desc + " failed!", e);
        }
        return new ComJsObject(desc + "()", v, null);
    }

    @Override
    public Object getMember(String name) {
        Dispatch d = getDispatchMember();
        return new ComJsObject(desc + "." + name, d, name);
    }

    @Override
    public void setMember(String name, Object value) {
        Dispatch d = getDispatchMember();
        Variant v = var(value);
        try {
            Dispatch.put(d, name, v);
        } catch (JacobException e) {
            throw new UnsupportedOperationException(desc + " failed!", e);
        }
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        Object o;
        if (dispatch instanceof Variant) {
            o = ((Variant) dispatch).toJavaObject();
        } else if (member == null) {
            o = dispatch.toString();
        } else {
            Variant v = get();
            o = v.toJavaObject();
        }

        if (Number.class.isInstance(hint)) {
            if (o instanceof Number) {
                return (Number) o;
            } else {
                return null;
            }
        }
        return o == null ? null : o.toString();
    }

    @Override
    public String toString() {
        return (String) getDefaultValue(String.class);
    }

    @Override
    public double toNumber() {
        return ((Number) getDefaultValue(Number.class)).doubleValue();
    }

    private Variant var(Object in) {
        if (in instanceof CharSequence) {
            in = in.toString();
        }
        return new Variant(in);
    }
}
