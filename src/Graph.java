import java.util.ArrayList;
import java.util.List;

/**
 * This class holds the structure of a undirected vertex-weighted graph.
 * Adjacency List Graph structure with enableability of vertices and marking of
 * edges instead of removal.
 */
public class Graph {

	public Vertex[] vertices;
	public int enabledVerticesCount;

	/**
	 * Create a graph out of an vertex array
	 * 
	 * @param vertices The vertex array
	 */
	public Graph(Vertex[] vertices) {
		this.vertices = vertices;
		enabledVerticesCount = vertices.length;
	}

	/**
	 * Disable vertex and all incident edges
	 * 
	 * @param n the vertex to be disabled
	 */
	public void disableVertex(int n) {
		if (vertices[n].enabled) {
			vertices[n].enabled = false;
			enabledVerticesCount -= 1;
			// disable incident edges
			for (Edge e : vertices[n].edges) {
				if (e.enabled) {
					e.enabled = false;
					int other = e.other(n);
					vertices[other].degree -= 1;
				}
			}
			vertices[n].degree = 0;
		}
	}

	/**
	 * Enable vertex and all incident edges
	 * 
	 * @param n the vertex to be enabled
	 */
	public void enableVertex(int n) {
		if (!vertices[n].enabled) {
			vertices[n].enabled = true;
			enabledVerticesCount += 1;
			vertices[n].degree = 0;
			// enable edges if adjacent vertex is also enabled
			for (Edge e : vertices[n].edges) {
				int other = e.other(n);
				if (vertices[other].enabled) {
					e.enabled = true;
					vertices[other].degree += 1;
					vertices[n].degree += 1;
				}
			}
		}
	}

	/**
	 * Resets the enablings and markings (hard reset)
	 */
	public void reset() {
		for (int i = 0; i < vertices.length; i++) {
			Vertex v = vertices[i];
			v.enabled = true;
			v.weight = v.originalWeight;
			for (Edge e : v.edges) {
				e.enabled = true;
			}
			v.resetMarkings();
			v.degree = v.edges.size();
		}
		enabledVerticesCount = vertices.length;
	}

	/**
	 * Resets the markings of a graph (soft reset)
	 */
	public void resetMarkings() {
		for (Vertex v : vertices)
			v.resetMarkings();
	}

	/**
	 * Find cycles in graph. This method makes use of a DFS.
	 * 
	 * @return All cycles found
	 */
	public List<int[]> findCycles() {
		// List of cycles
		List<int[]> cycles = new ArrayList<>();

		// List used as a stack
		List<Integer> stack = new ArrayList<>();

		// DFS for every connected component
		boolean foundConnectedComponent;
		do {

			// Find first enabled unmarked vertex
			for (int i = 0; i < vertices.length; i++) {
				if (!vertices[i].enabled)
					continue;
				if (vertices[i].marked)
					continue;
				stack.add(i);
				vertices[i].marked = true;
				break;
			}

			// update whether we found an untouched connected component
			foundConnectedComponent = stack.size() > 0;

			// as long as stack is not empty
			while (stack.size() > 0) {
				// pop current stack top
				int lastIndex = stack.size() - 1;
				int cur = stack.get(lastIndex);

				// search for unmarked edge incident to popped vertex
				Edge edge = null;
				for (Edge e : vertices[cur].edges) {
					if (!e.enabled)
						continue;
					if (e.marked)
						continue;
					edge = e;
					break;
				}

				// if there exists unmarked edge
				if (edge != null) {
					// mark edge
					edge.marked = true;

					// get other vertex id
					int other = edge.other(cur);

					if (vertices[other].marked) {
						// if marked
						// get cycle

						// identify cycle length
						int c = 1;

						for (int i = stack.size() - 1; i >= 0; i--) {
							if (stack.get(i) == other)
								break;
							c++;
						}

						// create cycle array
						int[] cycle = new int[c];
						for (int i = 0; i < c; i++) {
							cycle[i] = stack.get(stack.size() - c + i);
						}
						cycles.add(cycle);
					} else {
						// if not marked
						// mark
						vertices[other].marked = true;

						// add on top of stack
						stack.add(other);
					}
				} else {
					// if there is no umnarked edge pop from stack
					stack.remove(lastIndex);
				}

			}
		} while (foundConnectedComponent);

		// remove markings
		resetMarkings();
		return cycles;
	}

	/**
	 * Find a disjoint set
	 * 
	 * @return the disjoint set as an int array of vertex indicies or NULL if there
	 *         is no disjoint set
	 */
	public int[] getDisjointSet() {
		List<int[]> cycles = findCycles();

		// find a cycle that has max 1 vertex with deg > 2
		for (int[] cycle : cycles) {
			short degOver2 = 0;
			for (int v : cycle) {
				int deg = vertices[v].degree;
				if (deg > 2) {
					degOver2++;
					if (degOver2 > 1) {
						break;
					}
				}
			}
			if (degOver2 <= 1) {
				return cycle;
			}
		}
		return null;
	}

	/**
	 * Cleans up the graph by using a kernel. Repeaditly disables all vertices with
	 * a degree of 1 or less.
	 * 
	 * @return the vertices removed
	 */
	public List<Integer> cleanUp() {
		// list of removed vertices
		List<Integer> removed = new ArrayList<>();
		boolean hasChanged;
		do {
			hasChanged = false;

			// see if there is a vertex with a degree of 1 or less
			for (int i = 0; i < vertices.length; i++) {
				if (!vertices[i].enabled)
					continue;
				if (vertices[i].degree <= 1) {
					// remove from graph
					disableVertex(i);
					removed.add(i);
					hasChanged = true;
				}
			}
		} while (hasChanged);
		return removed;
	}

	/**
	 * A method to copy graphs (i.e. used for parallelization). It does not copy
	 * markings, only graph structure and weights
	 * 
	 * @return the copied graph
	 */
	public Graph copy() {
		// list of edges list for each vertex
		List<List<Edge>> edgesList = new ArrayList<>();

		// establish same connections
		for (int i = 0; i < vertices.length; i++) {
			Vertex v = vertices[i];
			List<Edge> edges = new ArrayList<>();

			for (Edge e : v.edges) {
				int other = e.other(i);

				if (other < i) {
					Edge newEdge = new Edge(i, other);
					edges.add(newEdge);
					edgesList.get(other).add(newEdge);
				}
			}
			edgesList.add(edges);
		}

		// establish vertices
		Vertex[] verts = new Vertex[vertices.length];
		int i = 0;
		for (List<Edge> edges : edgesList) {
			verts[i] = new Vertex(edges, vertices[i].weight);
			i++;
		}

		// create graph object
		Graph g = new Graph(verts);

		// establish same "deletions"
		for (int x = 0; x < verts.length; x++) {
			if (!vertices[x].enabled) {
				g.disableVertex(x);
			}
		}

		return g;

	}

	/**
	 * Adds an edge to the graph
	 * 
	 * @param e the edge to be inserted
	 */
	public void addEdge(Edge e) {
		vertices[e.v].edges.add(e);
		vertices[e.v].degree += 1;
		vertices[e.w].edges.add(e);
		vertices[e.w].degree += 1;
	}

	/**
	 * Creates a graph out of a list of weights and a list of connections.
	 * 
	 * @param weights     A list of weights where the first weight equals the weight
	 *                    of the first vertex
	 * @param connections A list where the first index equals the list of vertices
	 *                    connected to the first vertex etc..
	 * @return a graph object with these constrains
	 */
	public static Graph createGraph(List<Float> weights, List<List<Integer>> connections) {
		// Create vertex array
		Vertex[] vertices = new Vertex[weights.size()];

		// Create list of list of edges, for each vertex own list
		List<List<Edge>> edgesList = new ArrayList<>();

		// Fill edges
		for (int i = 0; i < vertices.length; i++) {
			List<Edge> edges = new ArrayList<>();
			List<Integer> connection = connections.get(i);

			for (Integer other : connection) {
				if (other >= i)
					continue;
				Edge e = new Edge(i, other);
				edges.add(e);
				// constant time in ArrayList
				edgesList.get(other).add(e);
			}
			edgesList.add(edges);
		}

		// Create vertex objects
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = new Vertex(edgesList.get(i), weights.get(i));
		}

		return new Graph(vertices);
	}

}

/**
 * This class represents a vertex of the graph.
 */
class Vertex {

	// standard values
	float weight = 1.0f;
	float originalWeight = 1.0f;

	List<Edge> edges;

	boolean marked;
	boolean enabled = true;

	int degree;

	public Vertex(List<Edge> edges) {
		this.edges = edges;
		degree = edges.size();
	}

	public Vertex(List<Edge> edges, float weight) {
		this(edges);
		this.weight = weight;
		originalWeight = weight;
	}

	/**
	 * Reset vertex marking and incident edges markings
	 */
	public void resetMarkings() {
		marked = false;
		for (Edge e : edges)
			e.marked = false;
	}

}

/**
 * This class represents an edge of the graph.
 */
class Edge {

	int v, w;

	boolean marked;
	boolean enabled = true;

	public Edge(int v, int w) {
		this.v = v;
		this.w = w;
	}

	public int other(int x) {
		if (v == x)
			return w;
		return v;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Edge))
			return false;
		Edge e = (Edge) obj;
		return (e.v == v && e.w == w) || (e.v == w && e.w == v);
	}

}