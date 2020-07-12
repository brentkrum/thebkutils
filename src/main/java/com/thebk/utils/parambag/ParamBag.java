package com.thebk.utils.parambag

public abstract class ParamBag<Subclass extends ParamBag> extends AbstractReferenceCounted {
	private static final int MAX_NUM_PARAMS = Integer.parseInt(System.getProperty("com.bk.db.concurrent.ParamBag.max-num-params", "24"));
	private final long[] m_longs = new long[MAX_NUM_PARAMS];
	private final Object[] m_objs = new Object[MAX_NUM_PARAMS];
	private int m_stackTop;

	ParamBag() {
	}

	public final Subclass pi(int value) {
		m_longs[m_stackTop++] = value;
		return (Subclass)this;
	}
	public final int pi() {
		return (int)m_longs[--m_stackTop];
	}

	public final Subclass pb(boolean value) {
		m_longs[m_stackTop++] = value ? 1 : 0;
		return (Subclass)this;
	}
	public final boolean pb() {
		return m_longs[--m_stackTop] == 1;
	}

	public final Subclass pl(long value) {
		m_longs[m_stackTop++] = value;
		return (Subclass)this;
	}
	public final long pl() {
		return m_longs[--m_stackTop];
	}

	public final Subclass po(Object value) {
		m_objs[m_stackTop++] = value;
		return (Subclass)this;
	}

	@SuppressWarnings("unchecked")
	public final <T> T po() {
		return (T)m_objs[--m_stackTop];
	}

	public final int intAt(int indexFromTop) {
		return (int)m_longs[m_stackTop-1-indexFromTop];
	}

	public final long longAt(int indexFromTop) {
		return m_longs[m_stackTop-1-indexFromTop];
	}

	@SuppressWarnings("unchecked")
	public final <T> T objAt(int indexFromTop) {
		return (T)m_objs[m_stackTop-1-indexFromTop];
	}

	@Override
	protected void deallocate() {
		for(int i=0; i<m_stackTop; i++) {
			Object o = m_objs[i];
			if (o != null) {
				ReferenceCountUtil.safeRelease(o, 1);
				m_objs[i] = null;
			}
		}
		m_stackTop = 0;

		setRefCnt(1);
	}

}
