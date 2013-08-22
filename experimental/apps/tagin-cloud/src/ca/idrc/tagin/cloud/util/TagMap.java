package ca.idrc.tagin.cloud.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import ca.idrc.tagin.cloud.tag.Tag;

public class TagMap implements Serializable {

	private static final long serialVersionUID = 9111294008139563193L;
	private Map<String,Tag> mTags;
	
	public TagMap() {
		mTags = new LinkedHashMap<String,Tag>();
	}
	
	public Tag put(String key, Tag value) {
		return mTags.put(key, value);
	}
	
	public Tag get(String key) {
		return mTags.get(key);
	}
	
	public boolean containsKey(String key) {
		return mTags.containsKey(key);
	}
	
	public Collection<Tag> values() {
		return mTags.values();
	}
	
	public int size() {
		return mTags.size();
	}
}