/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge;

import com.sun.jna.platform.win32.OaIdl;

/**
 * internal used field for caching:<br/>
 * ID and type of an already resolved comfield name.
 */
final class ComField {

    final String name;
    final OaIdl.DISPID id;
    final Type type;

    ComField(String name, OaIdl.DISPID id, Type type) {
        this.name = name;
        this.id = id;
        this.type = type;
    }
    
    boolean isType(Type type) {
        return this.type == type;
    }

    void requireType(Type type) {
        if (isType(type)) {
            return;
        }
        throw new UnsupportedOperationException("Name '" + name + "' was already resolved as a "
                + this.type + " but required was a " + type + "!");
    }

    /**
     * The Type of a resolved displayid.
     */
    enum Type {
        /**
         * used for put and set.
         */
        FIELD,
        /**
         * used for invoke
         */
        METHOD;
    }

}
