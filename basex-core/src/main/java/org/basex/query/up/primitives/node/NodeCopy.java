package org.basex.query.up.primitives.node;

import org.basex.data.*;
import org.basex.query.up.*;
import org.basex.query.up.primitives.*;
import org.basex.query.util.*;
import org.basex.query.util.list.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.util.*;

/**
 * Abstract update primitive which holds a copy of nodes to be inserted.
 *
 * @author BaseX Team 2005-15, BSD License
 * @author Lukas Kircher
 */
abstract class NodeCopy extends NodeUpdate {
  /** Nodes to be inserted. */
  ANodeList nodes;
  /** Insertion sequence data clip (will be populated by {@link #prepare}). */
  DataClip insseq;

  /**
   * Constructor.
   * @param type type
   * @param pre target node pre value
   * @param data data
   * @param ii input info
   * @param nodes node copy insertion sequence
   */
  NodeCopy(final UpdateType type, final int pre, final Data data, final InputInfo ii,
      final ANodeList nodes) {
    super(type, pre, data, ii);
    this.nodes = nodes;
  }

  @Override
  public final void prepare(final MemData tmp) {
    // merge texts. after that, text nodes still need to be merged,
    // as two adjacent iterators may lead to two adjacent text nodes
    final ANodeList list = mergeNodeCacheText(nodes);
    nodes = null;
    // build main memory representation of nodes to be copied
    final int start = tmp.meta.size;
    new DataBuilder(tmp).build(list);
    insseq = new DataClip(tmp, start, tmp.meta.size);
    insseq.fragments = list.size();
  }

  /**
   * Adds top entries from the temporary data instance to the name pool,
   * which is used for finding duplicate attributes and namespace conflicts.
   * @param pool name pool
   */
  final void add(final NamePool pool) {
    final Data d = insseq.data;
    final int ps = insseq.start;
    final int pe = insseq.end;
    for(int p = ps; p < pe; ++p) {
      final int k = d.kind(p);
      if(k != Data.ATTR && k != Data.ELEM) continue;
      if(p > ps && d.parent(p, k) >= ps) break;
      final int u = d.uri(p, k);
      final QNm qnm = new QNm(d.name(p, k));
      if(u != 0) qnm.uri(d.nspaces.uri(u));
      pool.add(qnm, ANode.type(k));
    }
  }

  /**
   * Merges all adjacent text nodes in the given sequence.
   * @param nl iterator
   * @return iterator with merged text nodes
   */
  private static ANodeList mergeNodeCacheText(final ANodeList nl) {
    final int ns = nl.size();
    if(ns == 0) return nl;
    final ANodeList s = new ANodeList(ns);
    ANode n = nl.get(0);
    for(int c = 0; c < ns;) {
      if(n.type == NodeType.TXT) {
        final TokenBuilder tb = new TokenBuilder();
        while(n.type == NodeType.TXT) {
          tb.add(n.string());
          if(++c == ns) break;
          n = nl.get(c);
        }
        s.add(new FTxt(tb.finish()));
      } else {
        s.add(n);
        if(++c < ns) n = nl.get(c);
      }
    }
    return s;
  }

  @Override
  public final int size() {
    return insseq.fragments;
  }

  @Override
  public final String toString() {
    return Util.className(this) + '[' + ']' + ", " +
        (insseq != null ? size() : nodes.size()) + " ops]";
  }
}
