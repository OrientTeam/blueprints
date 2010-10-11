package com.tinkerpop.blueprints.pgm.impls.orientdb.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.orientechnologies.orient.core.db.graph.OGraphEdge;
import com.orientechnologies.orient.core.db.graph.OGraphElement;
import com.orientechnologies.orient.core.db.graph.OGraphVertex;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.exception.OGraphException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.ORecord.STATUS;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.impls.orientdb.OrientEdge;
import com.tinkerpop.blueprints.pgm.impls.orientdb.OrientGraph;
import com.tinkerpop.blueprints.pgm.impls.orientdb.OrientVertex;

/**
 * @author Luca Garulli (http://www.orientechnologies.com)
 */
public class OrientElementSequence<T extends Element> implements Iterator<T>, Iterable<T> {
	private final Iterator<? extends OGraphElement>	elements;
	private final OrientGraph												graph;

	public OrientElementSequence(final OrientGraph iGraph, final Iterator<? extends OGraphElement> edges) {
		this.graph = iGraph;
		this.elements = edges;
	}

	public boolean hasNext() {
		return this.elements.hasNext();
	}

	@SuppressWarnings("unchecked")
	public T next() {
		Object o = this.elements.next();
		if (null == o)
			throw new NoSuchElementException();

		OGraphElement e = null;
		if (o instanceof OGraphElement)
			e = (OGraphElement) o;
		else {
			final ODocument doc;
			if (o instanceof ODocument)
				doc = (ODocument) o;
			else if (o instanceof ORID) {
				doc = new ODocument((ODatabaseRecord<?>) graph.getRawGraph().getUnderlying(), (ORID) o);
			} else
				throw new IllegalArgumentException("Can't transform object " + o + " in graph element");

			if (doc.getInternalStatus() == STATUS.NOT_LOADED)
				doc.load();

			if (doc.getClassName().equals(OGraphVertex.class.getSimpleName()))
				e = new OGraphVertex(graph.getRawGraph(), doc);
			else if (doc.getClassName().equals(OGraphEdge.class.getSimpleName()))
				e = new OGraphEdge(graph.getRawGraph(), doc);
			else {
				final OClass cls = graph.getRawGraph().getMetadata().getSchema().getClass(doc.getClassName());
				if (cls != null && cls.getSuperClass() != null) {
					if (cls.getSuperClass().getName().equals(OGraphVertex.class.getSimpleName()))
						e = new OGraphVertex(graph.getRawGraph(), doc.getClassName());
					else if (cls.getSuperClass().getName().equals(OGraphEdge.class.getSimpleName()))
						e = new OGraphEdge(graph.getRawGraph(), doc.getClassName());
				}
			}

			if (e == null)
				throw new OGraphException("Unrecognized class: " + doc.getClassName());
		}

		// TRY IN CACHE
		final Element el = this.graph.getCachedElement(e.getDocument().getIdentity());
		if (el != null)
			return (T) el;

		if (e instanceof OGraphEdge)
			return (T) new OrientEdge(graph, (OGraphEdge) e);
		else
			return (T) new OrientVertex(graph, (OGraphVertex) e);
	}

	public void remove() {
		this.elements.remove();
	}

	public Iterator<T> iterator() {
		return this;
	}
}
