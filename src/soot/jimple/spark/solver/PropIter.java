/* Soot - a J*va Optimization Framework
 * Copyright (C) 2002 Ondrej Lhotak
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package soot.jimple.spark.solver;
import soot.jimple.spark.*;
import soot.jimple.spark.pag.*;
import soot.jimple.spark.sets.*;
import soot.*;
import java.util.*;

/** Propagates points-to sets along pointer assignment graph using iteration.
 * @author Ondrej Lhotak
 */

public final class PropIter extends Propagator {
    public PropIter( PAG pag ) { this.pag = pag; }
    /** Actually does the propagation. */
    public final void propagate() {
        final OnFlyCallGraph ofcg = pag.getOnFlyCallGraph();
        new TopoSorter( pag, false ).sort();
	for( Iterator it = pag.allocSources().iterator(); it.hasNext(); ) {
	    handleAllocNode( (AllocNode) it.next() );
	}

        int iteration = 1;
	boolean change;
        TreeSet simpleSources = new TreeSet( pag.simpleSources() );
	do {
            if( pag.getOpts().verbose() ) {
                System.out.println( "Iteration "+(iteration++) );
            }
	    change = false;
	    for( Iterator it = simpleSources.iterator(); it.hasNext(); ) {
                change = handleSimples( (VarNode) it.next() ) | change;
	    }
	    for( Iterator it = pag.loadSources().iterator(); it.hasNext(); ) {
                change = handleLoads( (FieldRefNode) it.next() ) | change;
	    }
	    for( Iterator it = pag.storeSources().iterator(); it.hasNext(); ) {
                change = handleStores( (VarNode) it.next() ) | change;
	    }
            if( ofcg != null ) {
                for( Iterator recIt = ofcg.allReceivers().iterator(); recIt.hasNext(); ) {
                    final VarNode rec = (VarNode) recIt.next();
                    PointsToSetInternal recSet = rec.getP2Set();
                    if( recSet != null ) {
                        change = rec.getP2Set().forall( new P2SetVisitor() {
                        public final void visit( Node n ) {
                                returnValue = ofcg.addReachingType(
                                    rec, n.getType(), null ) | returnValue;
                            }
                        } ) | change;
                    }
                }
            }
	} while( change );
    }

    /* End of public methods. */
    /* End of package methods. */

    /** Propagates new points-to information of node src to all its
     * successors. */
    protected final boolean handleAllocNode( AllocNode src ) {
	boolean ret = false;
	Node[] targets = pag.allocLookup( src );
	for( int i = 0; i < targets.length; i++ ) {
	    ret = targets[i].makeP2Set().add( src ) | ret;
	}
	return ret;
    }

    protected final boolean handleSimples( VarNode src ) {
	boolean ret = false;
	PointsToSetInternal srcSet = src.getP2Set();
	if( srcSet.isEmpty() ) return false;
	Node[] simpleTargets = pag.simpleLookup( src );
	for( int i = 0; i < simpleTargets.length; i++ ) {
	    ret = simpleTargets[i].makeP2Set().addAll( srcSet, null ) | ret;
	}
        return ret;
    }

    protected final boolean handleStores( VarNode src ) {
	boolean ret = false;
	final PointsToSetInternal srcSet = src.getP2Set();
	if( srcSet.isEmpty() ) return false;
	Node[] storeTargets = pag.storeLookup( src );
	for( int i = 0; i < storeTargets.length; i++ ) {
            final FieldRefNode fr = (FieldRefNode) storeTargets[i];
            final SparkField f = fr.getField();
            ret = fr.getBase().getP2Set().forall( new P2SetVisitor() {
            public final void visit( Node n ) {
                    AllocDotField nDotF = pag.makeAllocDotField( 
                        (AllocNode) n, f );
                    if( nDotF.makeP2Set().addAll( srcSet, null ) ) {
                        returnValue = true;
                    }
                }
            } ) | ret;
	}
        return ret;
    }

    protected final boolean handleLoads( FieldRefNode src ) {
	boolean ret = false;
	final Node[] loadTargets = pag.loadLookup( src );
        final SparkField f = src.getField();
        ret = src.getBase().getP2Set().forall( new P2SetVisitor() {
        public final void visit( Node n ) {
                AllocDotField nDotF = ((AllocNode)n).dot( f );
                if( nDotF == null ) return;
                PointsToSetInternal set = nDotF.getP2Set();
                if( set.isEmpty() ) return;
                for( int i = 0; i < loadTargets.length; i++ ) {
                    VarNode target = (VarNode) loadTargets[i];
                    if( target.makeP2Set().addAll( set, null ) ) {
                        returnValue = true;
                    }
                }
            }
        } ) | ret;
        return ret;
    }

    protected PAG pag;
}


