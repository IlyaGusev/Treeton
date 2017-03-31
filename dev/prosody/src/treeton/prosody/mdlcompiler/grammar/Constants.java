/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface Constants {
	static final String TYPE_STRING = "String";

	static final String TYPE_NUMBER = "Number";

	static final String TYPE_FLOAT = "Float";

	static final String TYPE_DATE = "Date";

	static final String TYPE_BOOLEAN = "Boolean";

	static final String TYPE_CHARACTER = "Character";

	static final String TYPE_NULL = "null";

	/** Compatible with any type. Look {@link Signature#equals(Object)} */
	static final String TYPE_OBJECT = "Object";

	static final String TYPE_ANNOTATION = "Annotation";

	static final String TYPE_BINDING = ":binding";

	static final String BINDING_ALL = ":all";

	static final List<String> ALL_TYPE_NAMES = Collections
			.unmodifiableList(Arrays.asList(TYPE_STRING, TYPE_NUMBER,
					TYPE_FLOAT, TYPE_DATE, TYPE_BOOLEAN, TYPE_CHARACTER,
					TYPE_OBJECT, TYPE_ANNOTATION, TYPE_BINDING));

	static final List<String> COMPATIBLE_ASSIGNMENT_EXPRESSION_TYPE_NAMES = Collections
			.unmodifiableList(Arrays.asList(TYPE_STRING, TYPE_NUMBER,
					TYPE_FLOAT, TYPE_DATE, TYPE_CHARACTER, TYPE_BOOLEAN));

	static final String TYPE_UNDEFINED = "UnknownType";

	// -------------------------------------------------
	public static final String SYSTEM_FEATURE_TYPE = "@type";

	public static final String SYSTEM_FEATURE_STRING = "@string";

	public static final String SYSTEM_FEATURE_LENGTH = "@length";

	public static final String SYSTEM_FEATURE_START = "@start";

	public static final String SYSTEM_FEATURE_END = "@end";

	public static final String SYSTEM_FEATURE_ID = "@id";

	static final List<String> ALL_SYSTEM_FEATURE_NAMES = Collections
			.unmodifiableList(Arrays
					.asList(SYSTEM_FEATURE_TYPE, SYSTEM_FEATURE_STRING,
							SYSTEM_FEATURE_LENGTH, SYSTEM_FEATURE_START,
							SYSTEM_FEATURE_END, SYSTEM_FEATURE_ID));

	static final List<String> ALL_READONLY_SYSTEM_FEATURE_NAMES = Collections
			.unmodifiableList(Arrays.asList(SYSTEM_FEATURE_STRING,
					SYSTEM_FEATURE_LENGTH, SYSTEM_FEATURE_ID));
	// -------------------------------------------------
	static final String BINDING_FEATURE_STRING = "@string";

	static final String BINDING_FEATURE_LENGTH = "@length";

	static final String BINDING_FEATURE_START = "@start";

	static final String BINDING_FEATURE_END = "@end";

	static final String BINDING_FEATURE_SIZE = "@size";

	static final List<String> ALL_BINDING_FEATURE_NAMES = Collections
			.unmodifiableList(Arrays.asList(BINDING_FEATURE_STRING,
					BINDING_FEATURE_LENGTH, BINDING_FEATURE_START,
					BINDING_FEATURE_END, BINDING_FEATURE_SIZE));

	// -------------------------------------------------
	public static final String CONTROL_KEY_RULE = "byrule";

	public static final String CONTROL_KEY_LENGTH = "length";

	public static final String CONTROL_KEY_MATCH = "match";

	public static final String CONTROL_KEY_STEPPING = "stepping";

	public static final String CONTROL_VALUE_ONE = "one";

	public static final String CONTROL_VALUE_ALL = "all";

	public static final String CONTROL_VALUE_SHORTEST = "shortest";

	public static final String CONTROL_VALUE_LONGEST = "longest";

	public static final String CONTROL_VALUE_ONCE = "once";
}
