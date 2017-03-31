/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.prosody.mdlcompiler.grammar.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseListNode<E extends BaseNode> extends BaseNode
		implements Iterable<E> {
	protected List<E> elements = new ArrayList<E>();

	private String separator;

	public BaseListNode() {
	}

	public BaseListNode(String separator) {
		this.separator = separator;
	}

	public BaseListNode(E element) {
		this.elements.add(element);
	}

	public BaseListNode(E element, String separator) {
		this.elements.add(element);
		this.separator = separator;
	}

	public boolean add(E element) {
		return element != null && this.elements.add(element);
	}

	public E get(int idx) {
		return this.elements.get(idx);
	}

	public int size() {
		return this.elements.size();
	}

	public Iterator<E> iterator() {
		return this.elements.iterator();
	}

	/**
	 * This functionality is not implemented. Perhaps we don't need it.
	 * 
	 * @deprecated
	 */
	public Iterator<BaseNode> getChildIterator() {
		throw new RuntimeException(
				"This functionality is not implemented. Mey be we don't need it.");
	}

	public List<E> getElements() {
		return elements;
	}

	public String getSeparator() {
		return separator;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		BaseListNode n;
		try {
			n = (BaseListNode) o;
		} catch (ClassCastException e) {
			return false;
		}
		if (separator != n.separator)
			return false;
		if (this.elements.size() != n.elements.size())
			return false;
		Iterator itr = n.iterator();
		for (E element : this.elements) {
			if (!element.equals(itr.next()))
				return false;
		}
		return true;
	}

	public boolean contains(E element) {
		return this.elements.contains(element);
	}

	@Override
	public void visit(Visitor visitor, boolean visitChildren) {
		if (visitChildren) {
			for (E element : this.elements) {
				element.visit(visitor, visitChildren);
			}
		}
		visitor.execute(this);
	}

	@Override
	public String toString() {
		return (size() > 0) ? toString(0) : "";
	}

	/**
	 * Return examples:<br>
	 * <code>
	 * "abc.cde.fgh".toString(0) = "abc.def.fgh"
	 * "abc.cde.fgh".toString(1) = "abc.def"
	 * "abc.cde.fgh".toString(2) = "abc"
	 * "abc.cde.fgh".toString(10) = ""
	 * "abc".toString(1) = ""
	 * </code>
	 */
	public String toString(int idx) {
		if (this.elements.size() > idx) {
			StringBuffer sb = new StringBuffer(this.elements.get(0).toString());
			for (int i = 1; i < elements.size() - idx; i++) {
				sb.append((this.separator == null) ? " " : this.separator);
				sb.append(this.elements.get(i).toString());
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	public int getColumn() {
		return (size() == 0) ? super.getColumn() : get(0).getColumn();
	}

	public int getLeft() {
		return (size() == 0) ? super.getLeft() : get(0).getLeft();
	}

	public int getLine() {
		return (size() == 0) ? super.getLine() : get(0).getLine();
	}

	public int getRight() {
		return (size() == 0) ? super.getRight() : get(size() - 1).getRight();
	}
}
