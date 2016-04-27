package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.util.CompareUtils;

public abstract class AbstractBasicGraphFieldList<T extends ListableGraphField, RM extends Field, U> extends AbstractGraphFieldList<T, RM, U> {

	protected abstract T createField(String key);

	protected T convertBasicValue(String itemKey) {
		String key = itemKey.substring(0, itemKey.lastIndexOf("-"));
		return createField(key);
	}

	protected T getField(int index) {
		return createField("item-" + index);
	}

	protected T createField() {
		return createField("item-" + (getSize() + 1));
	}

	@Override
	public long getSize() {
		return getProperties("item").size();
	}

	@Override
	public void removeAll() {
		for (String key : getProperties("item-").keySet()) {
			setProperty(key, null);
		}
	}

	@Override
	public List<? extends T> getList() {

		Map<String, String> map = getProperties("item");
		List<T> list = new ArrayList<>();
		// TODO sorting is not very efficient, because the keys are transformed to their order too often
		map.keySet().stream().sorted((key1, key2) -> {
			int index1 = Integer.parseInt(key1.substring("item-".length(), key1.lastIndexOf("-")));
			int index2 = Integer.parseInt(key2.substring("item-".length(), key2.lastIndexOf("-")));
			return index1 - index2;
		}).forEachOrdered(itemKey -> {
			list.add(convertBasicValue(itemKey));
		});
		return list;
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		container.getImpl().unlinkOut(getImpl(), HAS_LIST);

		if (in(HAS_LIST).count() == 0) {
			delete(null);
		}
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		container.getImpl().linkOut(getImpl(), HAS_LIST);
		return container.getList(getClass(), getFieldKey());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ListGraphField) {
			List<? extends T> listA = getList();
			List<? extends T> listB = ((ListGraphField) obj).getList();
			return CompareUtils.equals(listA, listB);
		}
		return false;
	}

	public void removeField() {
		delete(null);
	}
}
