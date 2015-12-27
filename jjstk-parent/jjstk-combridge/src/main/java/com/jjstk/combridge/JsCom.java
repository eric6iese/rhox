/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge;

import jdk.nashorn.api.scripting.AbstractJSObject;

/**
 * Javascript-Variante des Com-Knotens.
 */
@SuppressWarnings("restriction")
public class JsCom extends AbstractJSObject {

    /**
     * Baut die Verbindung zum Com-Knoten auf.
     */
    public static JsCom connect(String name) {
        return new JsCom(ComNode.connect(name));
    }

    private final ComNode node;

    private JsCom(ComNode node) {
        this.node = node;
    }

    @Override
    public Object call(Object thiz, Object... args) {
        return new JsCom(node.invoke(args));
    }

    @Override
    public Object getMember(String name) {
        return new JsCom(node.get(name));
    }

    @Override
    public void setMember(String name, Object value) {
        node.set(name, value);
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        Object o = node.value();
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
}
