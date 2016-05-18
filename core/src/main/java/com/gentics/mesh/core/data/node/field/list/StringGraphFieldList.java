package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.schema.field.StringListFieldMigrationTest;

import rx.Observable;

public interface StringGraphFieldList extends ListGraphField<StringGraphField, StringFieldListImpl, String> {

	String TYPE = "string";

	FieldTransformator STRING_LIST_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		StringGraphFieldList stringFieldList = container.getStringList(fieldKey);
		if (stringFieldList == null) {
			return Observable.just(new StringFieldListImpl());
		} else {
			return stringFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater STRING_LIST_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		StringGraphFieldList graphStringList = container.getStringList(fieldKey);
		StringFieldListImpl stringList = fieldMap.getStringFieldList(fieldKey);
		boolean isStringListFieldSetToNull = fieldMap.hasField(fieldKey) && (stringList == null || stringList.getItems() == null);
		GraphField.failOnDeletionOfRequiredField(graphStringList, isStringListFieldSetToNull, fieldSchema, fieldKey, schema);
		GraphField.failOnMissingRequiredField(graphStringList, stringList == null, fieldSchema, fieldKey, schema);

		// Handle Deletion
		if (isStringListFieldSetToNull && graphStringList != null) {
			graphStringList.removeField(container);
			return;
		}

		// Handle Create
		if (graphStringList == null) {
			graphStringList = container.createStringList(fieldKey);
			for (String item : stringList.getItems()) {
				graphStringList.createString(item);
			}
			return;
		}

		// Handle update
		if (graphStringList != null && stringList != null) {
			graphStringList.removeAll();
			for (String item : stringList.getItems()) {
				graphStringList.createString(item);
			}
		}
	};

	FieldGetter STRING_LIST_GETTER = (container, fieldSchema) -> {
		return container.getStringList(fieldSchema.getName());
	};

	/**
	 * Create a new string field and add it to the list.
	 * 
	 * @param string
	 * @return
	 */
	StringGraphField createString(String string);

	/**
	 * Return the string item at the given position.
	 * 
	 * @param index
	 * @return
	 */
	StringGraphField getString(int index);

}
