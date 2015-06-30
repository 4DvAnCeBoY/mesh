package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.Translated;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.syncleus.ferma.AbstractEdgeFrame;

public class TranslatedImpl extends AbstractEdgeFrame implements Translated {

	public static final String LANGUAGE_TAG_KEY = "languageTag";

	public String getLanguageTag() {
		return getProperty(LANGUAGE_TAG_KEY);
	}

	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_TAG_KEY, languageTag);
	}

	public AbstractGenericNode getStartNode() {
		return inV().nextOrDefault(AbstractGenericNode.class, null);
	}

	public AbstractBasicFieldContainerImpl getI18NProperties() {
		return outV().nextOrDefault(AbstractBasicFieldContainerImpl.class, null);
	}
}
