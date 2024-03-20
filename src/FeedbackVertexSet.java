import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A class that holds algorithms to solve the Feedback Vertex Set (FVS)
 */
public class FeedbackVertexSet {

	// The current boundary of the current BST
	private static float bound;

	/**
	 * Reads a graph and solves the FVS with the heuristic algorithm, the
	 * approximation algorithm and the exact algorithm
	 * 
	 * @param args the argument array (expected to have a length of 1 and the only
	 *             argument is supposed to be a file path)
	 */
	public static void main(String[] args) {
		// check if argument is legal
		if (args.length != 1) {
			System.err.println("Exactly one argument is expected, usage: \"java FeedbackVertexSet <GraphFilePath>\"");
			return;
		}

		File file = new File(args[0]);

		if (!file.exists()) {
			System.err.println("File not found: " + file.getAbsolutePath());
			return;
		}

		if (file.isDirectory()) {
			System.err.println("File is a directory, invalid type");
			return;
		}

		// read graph
		Graph g = null;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String header = reader.readLine();

			int n = Integer.valueOf(header);

			List<Float> weights = new ArrayList<>();
			List<List<Integer>> connectionsList = new ArrayList<>();

			for (int i = 0; i < n; i++) {
				weights.add(Float.valueOf(reader.readLine()));
				connectionsList.add(new ArrayList<>());
			}

			String current;
			while ((current = reader.readLine()) != null) {
				if (current.equals(""))
					continue;

				String[] split = current.split(" ");

				int v = Integer.valueOf(split[0]);
				int w = Integer.valueOf(split[1]);

				connectionsList.get(v).add(w);
			}

			reader.close();

			g = Graph.createGraph(weights, connectionsList);

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// try algorithms

		// heuristic
		System.out.println();
		System.out.println("Heuristic algorithm:");
		System.out.println();
		long l1 = System.currentTimeMillis();
		Tuple<Set<Integer>, Float> solution1 = heuristic(g);
		long l2 = System.currentTimeMillis();
		System.out.println("    Exectuion Time: " + (l2 - l1) + " ms");
		System.out.println("    Solution Weight: " + solution1.r);
		System.out.println("    Solution Set: " + solution1.l);
		System.out.println();

		// approximation
		System.out.println();
		System.out.println("Approximation algorithm:");
		System.out.println();
		l1 = System.currentTimeMillis();
		Tuple<Set<Integer>, Float> solution2 = approximation(g);
		l2 = System.currentTimeMillis();
		System.out.println("    Exectuion Time: " + (l2 - l1) + " ms");
		System.out.println("    Solution Weight: " + solution2.r);
		System.out.println("    Solution Set: " + solution2.l);
		System.out.println();

		// exact
		System.out.println();
		System.out.println("Exact algorithm (1 processor, approximation boundary):");
		System.out.println();
		l1 = System.currentTimeMillis();
		Tuple<Set<Integer>, Float> solution3 = exact(g, solution2.r, 1);
		l2 = System.currentTimeMillis();
		System.out.println("    Exectuion Time: " + (l2 - l1) + " ms");
		System.out.println("    Solution Weight: " + solution3.r);
		System.out.println("    Solution Set: " + solution3.l);
		System.out.println();

	}

	/**
	 * A simple greedy algorithm that takes a random vertex in a cycle until there
	 * are no cycles left
	 * 
	 * @param g the graph
	 * @return the set of vertices contained in a FVS
	 */
	public static Tuple<Set<Integer>, Float> heuristic(Graph g) {
		Set<Integer> solution = new HashSet<>();

		List<int[]> cycles = g.findCycles();

		float sumweight = 0.0f;
		// as long as there are cycles its not a valid FVS
		while (cycles.size() > 0) {
			for (int[] cycle : cycles) {

				// get smallest weight
				int v = -1;
				float minweight = Float.MAX_VALUE;
				for (int w : cycle) {
					float weight = g.vertices[w].originalWeight;
					if (weight < minweight) {
						v = w;
						minweight = weight;
					}
				}

				// add to solution if its not already in the solution
				if (solution.add(v)) {
					// remove from graph and update sum
					g.disableVertex(v);
					sumweight += minweight;
				}
			}

			// update cycles
			cycles = g.findCycles();
		}

		g.reset();

		return new Tuple<Set<Integer>, Float>(solution, sumweight);
	}

	/**
	 * Bafna-Berman-Fujito 2-Approximation on FVS
	 * 
	 * @param g the graph
	 * @return the set of vertices contained in the 2 approx FVS
	 */
	public static Tuple<Set<Integer>, Float> approximation(Graph g) {
		List<Integer> stack = new ArrayList<>();

		g.cleanUp();
		while (g.enabledVerticesCount > 0) {
			// see if semidisjoint cycle exists
			int[] semidisjoint = g.getDisjointSet();

			if (semidisjoint != null) {
				// semidisjoint cycle exists

				float gamma = Float.MAX_VALUE;

				// find min weight
				for (int v : semidisjoint) {
					float vgamma = g.vertices[v].weight;
					gamma = Math.min(gamma, vgamma);
				}

				// update weights
				for (int v : semidisjoint) {
					g.vertices[v].weight -= gamma;

					// check if its part of a solution
					if (g.vertices[v].weight <= 0) {
						stack.add(v);
						g.disableVertex(v);
					}
				}

			} else {
				// no semidisjoint cycle exists

				// find smallest gamma
				float gamma = Float.MAX_VALUE;

				for (Vertex v : g.vertices) {
					if (!v.enabled)
						continue;
					float vgamma = v.weight / ((float) (v.degree - 1));
					gamma = Math.min(vgamma, gamma);
				}

				// update weights
				for (int i = 0; i < g.vertices.length; i++) {
					Vertex v = g.vertices[i];
					if (!v.enabled)
						continue;
					v.weight -= (gamma * (v.degree - 1));

					// check if its part of a solution
					if (v.weight <= 0) {
						stack.add(i);
						g.disableVertex(i);
					}
				}
			}

			// clean the graph (so only cycles are left)
			g.cleanUp();
		}

		// reset graph to final removed graph to check for redundant vertices
		g.reset();
		for (Integer i : stack) {
			g.disableVertex(i);
		}

		// get non redundant solution
		Set<Integer> solution = new HashSet<>();
		float weight = 0.0f;

		while (stack.size() > 0) {
			// pop from stack
			int i = stack.get(stack.size() - 1);
			stack.remove(stack.size() - 1);

			// check redundancy with enabling vertex and checking if FVS is violated
			g.enableVertex(i);
			if (g.findCycles().size() > 0) {
				// non redundant

				solution.add(i);
				g.disableVertex(i);
				weight += g.vertices[i].originalWeight;
			}
		}

		g.reset();

		return new Tuple<Set<Integer>, Float>(solution, weight);
	}

	/**
	 * Determine the exact Feedback Vertex Set of a graph
	 * 
	 * @param g     the graph
	 * @param bound the bounded weight or -1 if no bound is given
	 * @return the FVS
	 */
	public static Tuple<Set<Integer>, Float> exact(Graph g, float bound, int processors) {
		Tuple<Set<Integer>, Float> solution;

		// update bond
		FeedbackVertexSet.bound = bound;

		if (bound < 0.0f) {
			// no bound specified

			// calc max bound
			float maxbound = 0.0f;
			for (Vertex v : g.vertices) {
				maxbound += v.originalWeight;
			}

			// update bond
			FeedbackVertexSet.bound = maxbound;
		}

		solution = bstFVS(g, 0, processors);

		g.reset();

		return solution;
	}

	/**
	 * A bounded search tree that is used for exact determination of the FVS
	 * 
	 * @param g          the graph
	 * @param bound2     the bound (found with the approximation i.e.)
	 * @param processors number of processors used
	 * @return the founded solution in this graph or null if solution would exceed
	 *         bound
	 */
	private static Tuple<Set<Integer>, Float> bstFVS(Graph g, float current, int processors) {

		////////// CHECK //////////

		// see if bound is exceeded
		if (current > bound) {
			return null;
		}

		List<int[]> cycles = g.findCycles();

		// see if it is already acyclic
		if (cycles.size() == 0)
			return new Tuple<>(new HashSet<>(), current);

		////////// BRANCH //////////

		Tuple<Set<Integer>, Float> solution = null;

		// clean graph (remove vertices with deg <= 1)
		List<Integer> removed = g.cleanUp();

		// see if semidisjoint cycle is available
		boolean foundSemidisjointCycle = false;
		for (int[] cycle : cycles) {

			// checking if its semidisjoint
			short countDegGreater2 = 0;
			int semidisjointVertex = -1;

			for (int v : cycle) {
				if (g.vertices[v].degree > 2) {
					countDegGreater2++;
					if (countDegGreater2 > 1) {
						break;
					}
					semidisjointVertex = v;
				}
			}

			if (countDegGreater2 <= 1) {
				// semidisjoint cycle is available
				foundSemidisjointCycle = true;

				// get smallest weight in cycle
				int smallestID = -1;
				float smallestWeight = Float.MAX_VALUE;

				for (int v : cycle) {
					float weight = g.vertices[v].weight;
					if (weight < smallestWeight) {
						smallestID = v;
						smallestWeight = weight;
					}
				}

				// make array of possible candidates
				int[] possible;
				if (semidisjointVertex != -1 && semidisjointVertex != smallestID) {
					possible = new int[] { smallestID, semidisjointVertex };
				} else {
					possible = new int[] { smallestID };
				}

				// branch on possible candidates
				solution = decide(g, current, possible, processors);

				break;
			}

		}

		// see if semidisjoint cycle was found
		if (!foundSemidisjointCycle) {
			// else branch smallest cycle (smallest cycle length)

			// find smallest cycle (length)
			int[] smallest = cycles.get(0);
			for (int[] cycle : cycles) {
				if (cycle.length < smallest.length) {
					smallest = cycle;
				}
			}

			// branch on every vertex of smallest cycle

			solution = decide(g, current, smallest, processors);
		}

		// enable all vertices disabled by cleaning (kernel)
		for (Integer i : removed) {
			g.enableVertex(i);
		}

		return solution;
	}

	/**
	 * A method that decides which of the possible vertices are the best decision
	 * for the FVS (done by branching until solution). Parallelization offered.
	 * 
	 * @param g        the graph
	 * @param bound2   the bound
	 * @param vertices all possible vertices
	 * @return the optimal soltuion
	 */
	private static Tuple<Set<Integer>, Float> decide(Graph g, float current, int[] vertices, int processors) {
		// check for parallelization
		if (processors == 1) {
			return decide(g, current, vertices);
		}

		// create solutions list
		CopyOnWriteArrayList<Tuple<Set<Integer>, Float>> solutions = new CopyOnWriteArrayList<Tuple<Set<Integer>, Float>>();

		// threads
		Thread[] threads;

		if (vertices.length >= processors) {
			// less processors than vertices

			threads = new Thread[processors];

			// as long as there are not assigned vertices
			int verticesUsed = 0;
			while (verticesUsed < vertices.length) {

				// see if thread any processor is available
				int available = -1;

				for (int i = 0; i < threads.length; i++) {

					// check if its even initialized
					if (threads[i] == null) {
						available = i;
						break;
					}

					try {
						threads[i].join(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					// check if its running
					if (!threads[i].isAlive()) {
						available = i;
						break;
					}
				}

				// setup new thread if processor available
				if (available != -1) {
					int v = vertices[verticesUsed++];

					threads[available] = new Thread(new Runnable() {

						@Override
						public void run() {
							// copy graph
							Graph copy = g.copy();

							// create solution
							copy.disableVertex(v);
							Tuple<Set<Integer>, Float> solution = bstFVS(copy, current + copy.vertices[v].weight, 1);
							if (solution != null) {
								solution.l.add(v);
								// update bond
								bound = Math.min(bound, solution.r);
							}

							// adding to solutions
							solutions.add(solution);
						}
					});

					// start thread
					threads[available].start();

				}

			}

		} else {
			// less vertices than processors
			threads = new Thread[vertices.length];

			// sub processors count calculation
			int subProcessCount = processors / vertices.length;
			int over = processors % vertices.length;

			// create threads
			for (int i = 0; i < threads.length; i++) {
				int v = vertices[i];
				int processorsCount = (i < over) ? 1 + subProcessCount : subProcessCount;
				threads[i] = new Thread(new Runnable() {

					@Override
					public void run() {
						// copy graph
						Graph copy = g.copy();

						// create solution
						copy.disableVertex(v);
						Tuple<Set<Integer>, Float> solution = bstFVS(copy, current + copy.vertices[v].weight,
								processorsCount);
						if (solution != null) {
							solution.l.add(v);
							// update bond
							bound = Math.min(bound, solution.r);
						}

						// adding to solutions
						solutions.add(solution);
					}
				});

				// start thread
				threads[i].start();
			}

		}

		// wait for threads to be done
		for (int i = 0; i < threads.length; i++) {
			if (threads[i] != null)
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}

		// pick best solution
		Tuple<Set<Integer>, Float> solution = null;

		for (Tuple<Set<Integer>, Float> solution2 : solutions) {
			// compare and take best result
			if (solution == null) {
				solution = solution2;
			} else if (solution2 != null) {
				if (solution2.r < solution.r) {
					solution = solution2;
				}
			}
		}

		return solution;
	}

	/**
	 * A method that decides which of the possible vertices are the best decision
	 * for the FVS (done by branching until solution). No parallelization.
	 * 
	 * @param g        the graph
	 * @param bound    the bound
	 * @param vertices all possible vertices
	 * @return the optimal soltuion
	 */
	private static Tuple<Set<Integer>, Float> decide(Graph g, float current, int[] vertices) {
		// current best solution
		Tuple<Set<Integer>, Float> solution = null;

		for (int v : vertices) {
			// try solution with v

			g.disableVertex(v);
			Tuple<Set<Integer>, Float> solution2 = bstFVS(g, current + g.vertices[v].weight, 1);
			if (solution2 != null) {
				solution2.l.add(v);
				// update bond
				bound = Math.min(bound, solution2.r);
			}
			g.enableVertex(v);

			// compare and take best result
			if (solution == null) {
				solution = solution2;
			} else if (solution2 != null) {
				if (solution2.r < solution.r) {
					solution = solution2;
				}
			}
		}

		return solution;
	}

}

/**
 * A simple Tuple class
 * 
 * @param <T> type 1
 * @param <V> type 2
 */
class Tuple<T, V> {
	T l;
	V r;

	public Tuple(T l, V r) {
		this.l = l;
		this.r = r;
	}

	@Override
	public String toString() {
		return "(" + l.toString() + "," + r.toString() + ")";
	}

}
