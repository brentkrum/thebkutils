package com.thebk.utils.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ObjectConfigBuilder
{
    private static final ObjectConfig EMPTY_CONFIG = ObjectConfigBuilder.config().build();

    public static ObjectConfig emptyConfig() {
        return EMPTY_CONFIG;
    }

    public static ObjectConfigObjectBuilder config() {
        return new ObjectConfig();
    }

    public static class ObjectConfigArrayBuilder {
        private final List<Object> m_array = new LinkedList<>();

        public ObjectConfigArrayBuilder option(int value) {
            m_array.add(value);
            return this;
        }

        public ObjectConfigArrayBuilder option(long value) {
            m_array.add(value);
            return this;
        }

        public ObjectConfigArrayBuilder option(String value) {
            m_array.add(value);
            return this;
        }

        public ObjectConfigArrayBuilder optionArray(ObjectConfigArrayBuilder array) {
            m_array.add(array.m_array);
            return this;
        }

        public ObjectConfigArrayBuilder optionObject(ObjectConfigObjectBuilder obj) {
            m_array.add(obj.m_obj);
            return this;
        }
    }

    public abstract static class ObjectConfigObjectBuilder {
        protected final Map<String,Object> m_obj = new HashMap<>();

        public ObjectConfigObjectBuilder option(String key, String value) {
            m_obj.put(key, value);
            return this;
        }
        public ObjectConfigObjectBuilder option(String key, boolean value) {
            m_obj.put(key, value);
            return this;
        }
        public ObjectConfigObjectBuilder option(String key, int value) {
            m_obj.put(key, value);
            return this;
        }
        public ObjectConfigObjectBuilder option(String key, long value) {
            m_obj.put(key, value);
            return this;
        }
        public ObjectConfigObjectBuilder pojoOption(String key, Object value) {
            m_obj.put(key, value);
            return this;
        }
        public ObjectConfigObjectBuilder optionArray(String key, ObjectConfigArrayBuilder array) {
            m_obj.put(key, array);
            return this;
        }

        public ObjectConfigObjectBuilder optionObject(String key, ObjectConfigObjectBuilder obj) {
            m_obj.put(key, obj);
            return this;
        }

        public abstract ObjectConfig build();
    }

    public static class ObjectConfig extends ObjectConfigObjectBuilder {

        public String getString(String key, String defaultValue) {
            final Object n = m_obj.get(key);
            return (n==null) ? defaultValue : n.toString();
        }

        public Object getPOJO(String key) {
            return m_obj.get(key);
        }

        public boolean has(String key) {
            return m_obj.containsKey(key);
        }

        public Boolean getBoolean(String key, Boolean defaultValue) {
            final Object n = m_obj.get(key);
            if (n instanceof Boolean) {
                return (Boolean)n;
            }
            return defaultValue;
        }
        public boolean getBoolean(String key, boolean defaultValue) {
            final Object n = m_obj.get(key);
            if (n instanceof Boolean) {
                return (Boolean)n;
            }
            return defaultValue;
        }

        public Integer getInteger(String key, Integer defaultValue) {
            final Object n = m_obj.get(key);
            if (n instanceof Integer) {
                return (Integer)n;
            }
            if (n != null) {
                try {
                    return Integer.parseInt(n.toString());
                } catch(NumberFormatException ex) {
                }
            }
            return defaultValue;
        }

        public Long getLong(String key, Long defaultValue) {
            final Object n = m_obj.get(key);
            if (n instanceof Long) {
                return (Long)n;
            }
            if (n != null) {
                try {
                    return Long.parseLong(n.toString());
                } catch(NumberFormatException ex) {
                }
            }
            return defaultValue;
        }

        public long getlong(String key, long defaultValue) {
            final Object n = m_obj.get(key);
            if (n instanceof Long) {
                return (Long)n;
            }
            if (n != null) {
                try {
                    return Long.parseLong(n.toString());
                } catch(NumberFormatException ex) {
                }
            }
            return defaultValue;
        }

        @Override
        public ObjectConfig build() {
            return this;
        }
    }

    private ObjectConfigBuilder() {
    }
}
