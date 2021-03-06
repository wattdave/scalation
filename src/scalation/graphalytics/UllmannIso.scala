
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
/** @author  Matthew Saltz, John Miller
 *  @version 1.0
 *  @date    Thu Jul 25 11:28:31 EDT 2013
 *  @see     LICENSE (MIT style license file).
 */

package scalation.graphalytics

// import collection.mutable.Set

import GraphTypes._

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
/** This 'UllmannIso' object provides an implementation for Subgraph Isomorphism
 *  that uses an adjacency set version of Ullmann's Algorithm.
 *  @param g  the data graph  G(V, E, l)
 *  @param q  the query graph Q(U, D, k)
 */
class UllmannIso (g: Graph, q: Graph)
      extends PatternMatcher (g, q)
{
    private val sims         = new GraphSim2 (g, q)    // object for Dual Simulation algorithm
    private var matches      = Set [Array [ISet]] ()   // initialize matches to empty
    private var noBijections = true                    // no results yet

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /** Apply the Ullmann Subgraph Isomorphism pattern matching algorithm to
     *  find the mappings from the query graph 'q' to the data graph 'g'.
     *  These are represented by a multi-valued function 'phi' that maps each
     *  query graph vertex 'u' to a set of data graph vertices '{v}'.
     */
    def mappings (): Array [ISet] = 
    {
        var psi: Set [Array [Int]] = null              // mappings from Dual Simulation
        if (noBijections) psi = bijections ()          // if no results, create them
        merge (psi)                                    // merge bijections to create mappings
    } // mappings

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /** Apply the Ullmann Subgraph Isomorphism algorithm to find subgraphs of data
     *  graph 'g' that isomorphically match query graph 'q'.  These are represented
     *  by a set of single-valued bijective functions {'psi'} where each 'psi'
     *  function maps each query graph vertex 'u' to a data graph vertices 'v'.
     */
    def bijections (): Set [Array [Int]] =
    {
        val phi = sims.feasibleMates ()                         // initial mappings from label match
        ullmannIso (sims.saltzGraphSim (phi), makeOrder (), 0)  // recursively find all bijections
        val psi = simplify (matches)                            // pull bijections out matches
        noBijections = false                                    // results now available
        psi                                                     // return the set of bijections
    } // bijections

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /** Refine the mappings 'phi' using the Ullmann Subgraph Isomorphism algorithm.
     *  @param phi    array of mappings from a query vertex u_q to { graph vertices v_g }
     *  @param depth  the depth of recursion
     */
    private def ullmannIso (phi: Array [ISet], ordering: Array [Int], depth: Int)
    {
        if (depth == q.size) {
            if (! phi.isEmpty) {
                matches += phi
                if (matches.size % CHECK == 0) println ("ullmannIso: matches so far = " + matches.size)
            } // if
        } else if (! phi.isEmpty) {
            for (i <- phi (ordering (depth)) if (! contains (phi, i, ordering,
              depth))) {
                val phiCopy = phi.map (x => x)                           // make a copy of phi
                phiCopy (ordering (depth)) = Set [Int] (i)               // isolate vertex i
                if (matches.size >= LIMIT) return                        // quit if at LIMIT
                // solve recursively for the next depth 
                ullmannIso (sims.saltzGraphSim (phiCopy), ordering, depth + 1)
            } // for
        } // if
    } // ullmannlIso

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /** Establish a vertex order from largest to smallest adjacency set ('adj') size.
     */
    def makeOrder (): Array [Int] = q.adj.zipWithIndex.sortBy (-1 * _._1.size).map (_._2)

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /** Is vertex j contained in any phi(i) for the previous depths?
     *  @param phi       array of mappings from a query vertex u_q to { graph vertices v_g }
     *  @param j         the vertex j to check
     *  @param ordering  the 'adj' size based ordering of the vertices
     *  @param depth     the current depth of recursion
     */
    private def contains (phi: Array [ISet], j: Int, ordering: Array [Int], depth: Int): Boolean =
    {
        for (i <- 0 until depth) if (phi (ordering(i)) contains j) return true
        false
    } // contains

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /** Create an array to hold matches for each vertex 'u' in the query graph
     *  'q' and initialize it to contain all empty sets.  Then for each bijection,
     *  add each element of the bijection to its corresponding match set.
     *  @param psi  the set of bijections
     */
    private def merge (psi: Set [Array [Int]]): Array [ISet] =
    {
        val matches = Array.ofDim [ISet] (q.size).map (_ => Set [Int] ())
        for (b <- bijections; i <- b.indices) matches(i) += b(i)
        matches
    } // merge

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /** Pull the bijections out of the complete match set.
     *  @param matches  the complete match set embedding all bijections
     */
    private def simplify (matches: Set [Array [ISet]]): Set [Array [Int]] =
    {
        matches.map (m => m.map (set => set.iterator.next))
    } // simplify

} // UllmannIso class


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
/** The 'UllmannIsoTest' object is used to test the 'UllmannIso' class.
 */
object UllmannIsoTest extends App
{
    val gSize     = 1000         // size of the data graph
    val qSize     =   10         // size of the query graph
    val nLabels   =  100         // number of distinct labels
    val gAvDegree =    5         // average vertex out degree for data graph
    val qAvDegree =    2         // average vertex out degree for query graph

    val g = GraphGenerator.genRandomGraph (gSize, nLabels, gAvDegree)
    val q = GraphGenerator.genBFSQuery (qSize, qAvDegree, g)

    val matcher = new UllmannIso (g, q)                 // Ullmann Subgraph Isomorphism Pattern Matcher
    val psi = matcher.time { matcher.bijections () }    // time the matcher
    for (p <- psi) println (p.deep)

} // UllmannIsoTest

