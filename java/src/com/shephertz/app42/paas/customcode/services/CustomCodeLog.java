package com.shephertz.app42.paas.customcode.services;
/**
 * Custom code debug class
 * @author Himanshu Sharma
 *
 */
public class CustomCodeLog {

	private static boolean debug = false;

	public static boolean isDebug() {
		return debug;
	}

	public static void setDebug(boolean debug) {
		CustomCodeLog.debug = debug;
	}

	public static void info(String msg) {
		System.out.println(msg);
	}

	public static void debug(String msg) {
		if (debug)
			System.out.println(msg);
	}

	public static void error(String msg) {
		System.out.println(msg);
	}

	public static void fatal(String msg) {
		System.out.println(msg);
	}

}
