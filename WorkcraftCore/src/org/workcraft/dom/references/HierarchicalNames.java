package org.workcraft.dom.references;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.workcraft.util.Identifier;

public class HierarchicalNames {
	final static String quoteType = "'";
	final public static String hierarchySeparator = "/";
	final public static String flatNameSeparator = "__";

	// TODO: make it work with the embedded ' characters
	private static String hPattern = "(/)?(((\\'([^\\']+)\\')|([_A-Za-z][_A-Za-z0-9]*))([\\+\\-\\~])?(/[0-9]+)?)(.*)";



	public static String getFlatName(String reference) {
		return getFlatName(reference, flatNameSeparator, true);
	}

	private static String getFlatName(String reference, String flatSeparator, boolean isFirst) {
		if (flatSeparator==null) flatSeparator=flatNameSeparator;

		String ret = "";
		// in this version the first separator is not shown
		if (!isFirst && reference.startsWith(hierarchySeparator)) ret=flatSeparator;
		//
//		if (reference.startsWith(hierarchySeparator)) ret=flatSeparator;

		String head = getReferenceHead(reference);
		String tail = getReferenceTail(reference);

		if (tail.equals(""))
			return ret+head;
		else
			return ret+head+getFlatName(tail, flatSeparator, false);
	}


	public static String flatToHierarchicalName(String reference) {
		return flatToHierarchicalName(reference, flatNameSeparator);
	}

	private static String flatToHierarchicalName(String reference, String flatSeparator) {
		if (flatSeparator==null) flatSeparator = flatNameSeparator;

		return reference.replaceAll(flatSeparator, hierarchySeparator);

	}


	public static void splitReference(String reference, LinkedList<String> path) {

		if (reference.equals("")) return;

		Pattern pattern = Pattern.compile(hPattern);

		Matcher matcher = pattern.matcher(reference);
		if (matcher.find()) {
			String str = matcher.group(2);
			str=str.replace("'", "");
			path.add(str);
			splitReference(matcher.group(9), path);
		}
	}

	public static String getParentReference(String reference) {
		// legacy reference support
		if (Identifier.isNumber(reference)) return "";

		LinkedList<String> path = new LinkedList<String>();
		splitReference(reference, path);

		String ret = "";
		for (int i=0;i<path.size()-1;i++) {
			ret +=path.get(i);
			if (i<path.size()-2) ret+=hierarchySeparator;
		}
		return ret;
	}

	public static String getReferencePath(String reference) {
		// legacy reference support
		if (Identifier.isNumber(reference)) return "";

		LinkedList<String> path = new LinkedList<String>();
		splitReference(reference, path);

		String ret = "";
		for (int i=0;i<path.size()-1;i++) {
			ret +=path.get(i)+hierarchySeparator;
		}
		return ret;
	}

	public static String getReferenceHead(String reference) {

		// legacy reference support
		if (Identifier.isNumber(reference)) return reference;


		Pattern pattern = Pattern.compile(hPattern);

		Matcher matcher = pattern.matcher(reference);
		if (matcher.find()) {

			String head = matcher.group(2);
			head = head.replace("'", "");
			return head;
		}

		return null;
	}

	public static String getReferenceTail(String reference) {
		// legacy reference support
		if (Identifier.isNumber(reference)) return "";

		Pattern pattern = Pattern.compile(hPattern);

		Matcher matcher = pattern.matcher(reference);

		if (matcher.find()) {
			String tail = matcher.group(9);
			return tail;
		}

		return null;
	}

	public static String getNameFromReference(String reference) {

		String head = getReferenceHead(reference);
		String tail = getReferenceTail(reference);

		if (tail.equals("")) return head;
		else
			return getNameFromReference(tail);

	}
}
