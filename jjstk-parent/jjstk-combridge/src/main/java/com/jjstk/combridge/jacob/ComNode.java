/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jjstk.combridge.jacob;

import com.jacob.com.Dispatch;
import com.jacob.com.JacobException;
import com.jacob.com.Variant;

/**
 * Ein Knoten in einer Com-Hierarchie von Werten.
 */
public class ComNode {

	static {
		JacobLoader.initialize();
	}

	/**
	 * Baut die Verbindung zum angegebenen Com-Object auf.
	 */
	public static ComNode connect(String name) {
		return new ComNode(name, new Dispatch(name), null);
	}

	private final String desc;
	private Object dispatch;
	private final String member;

	private ComNode(String desc, Object dispatch, String member) {
		this.desc = desc;
		this.dispatch = dispatch;
		this.member = member;
	}

	private Variant var(Object in) {
		if (in instanceof CharSequence) {
			in = in.toString();
		}
		return new Variant(in);
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

	/**
	 * Interpretiert den aktuellen als Knoten und ruft ihn mit den Parametern
	 * auf.
	 */
	public ComNode invoke(Object... args) {
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
		return new ComNode(desc + "()", v, null);
	}

	/**
	 * Löst ein Sub-Member des aktuellen Knotens auf.
	 */
	public ComNode get(String name) {
		Dispatch d = getDispatchMember();
		return new ComNode(desc + "." + name, d, name);
	}

	/**
	 * Setzt den Wert des Members neu.
	 */
	public void set(String name, Object value) {
		Dispatch d = getDispatchMember();
		Variant v = var(value);
		try {
			Dispatch.put(d, name, v);
		} catch (JacobException e) {
			throw new UnsupportedOperationException(desc + " failed!", e);
		}
	}

	/**
	 * Liefert den Java-Wert des Com-Noten zurück.
	 */
	public Object value() {
		Object o;
		if (dispatch instanceof Variant) {
			return ((Variant) dispatch).toJavaObject();
		} else if (member == null) {
			return dispatch.toString();
		}
		Variant v = get();
		return v.toJavaObject();
	}

	@Override
	public String toString() {
		return "ComNode(" + desc + ")";
	}
}
