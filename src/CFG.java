import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class CFG {
	private String S;
	private ArrayList<String> V;
	private ArrayList<String> alpha;
	private HashMap<String, ArrayList<String>> R;
	private HashMap<String, ArrayList<String>> invertedIndex;
	private ArrayList<String> containsEpsilon;
	
	// The dollar sign is represented as an &
	// and epsilon as $.
	private HashMap<String, HashSet<String>> first;
	private HashMap<String, HashSet<String>> follow;
	
	public CFG(String s, ArrayList<String> v, ArrayList<String> alpha, HashMap<String, ArrayList<String>> r,
			HashMap<String, ArrayList<String>> invertedIndex, ArrayList<String> containsEpsilon) {
		S = s;
		V = v;
		this.alpha = alpha;
		R = r;
		this.invertedIndex = invertedIndex;
		this.containsEpsilon = containsEpsilon;
		
		first = follow = null;
	}
	
	// Removes a certain character in each possible way => O(2^numOcc) ways.
	// Where numOcc is the number of occurrences of this char in the input string.
	private static ArrayList<String> genCombinations(String rule, String optionalVar){
		String[] tokens = rule.split("@");
		ArrayList<String> res = new ArrayList<>();
		ArrayList<Integer> varLocations = new ArrayList<>(tokens.length);
		
		for(int i = 0; i < tokens.length; ++i)
			if(tokens[i].equals(optionalVar)) 
				varLocations.add(i);
		
		int numOcc = varLocations.size();
		int n = (1 << numOcc) - 1;
		
		for(int i = 0; i < n; ++i) {
			HashSet<Integer> excluded = new HashSet<>();
			for(int j = 0; j < numOcc; ++j) 
				if((i & (1 << j)) == 0)
					excluded.add(varLocations.get(j));
			
			StringBuilder sb = new StringBuilder();
			for(int j = 0; j < tokens.length; ++j)
				if(!(excluded.contains(j)))
					sb.append(tokens[j]);
			
			if(sb.length() != 0) // we're not allowing epsilons
				res.add(sb.toString());
		}
		
		return res;
	}
	
	// Removes epsilon rules from the grammar appropriately.
	// If there are unit rules of vars that are optional 
	// or rules that only contain repetitions of an optional 
	// variable then these rules will be removed.
	private void removeEpsilon() {
		for(String var: containsEpsilon) 
			for(String _var: invertedIndex.get(var)) { 
				ArrayList<String> rules = new ArrayList<>(R.get(_var));
				for(String rule: rules) 
					if(rule.contains(var)) 
						R.get(_var).addAll(genCombinations(rule, var));
			}
	}
	
	private ArrayList<String> substituteVars(String rule, ArrayList<String> 
	substitutions){
		return (ArrayList<String>) substitutions.stream().
				map(sub -> sub + rule).collect(Collectors.toList());
	}
	
	private void eliminateImmediateLeftRec(String var){
		ArrayList<String> rules = R.get(var);
		ArrayList<String> alpha = new ArrayList<>(rules.size()), 
				beta = new ArrayList<>(rules.size());
		String _var = "_" + var;
		boolean isLeftRecursive = false;
		
		for(String rule: rules) 
			if(rule.startsWith(var)) {
				isLeftRecursive = true;
				alpha.add(rule.replaceFirst("^"+var+"@", "") + "@" + _var);
			}else
				beta.add(rule + "@" + _var);
		
		if(!isLeftRecursive)
			return;
		alpha.add("$");
		V.add(_var);
		R.put(_var, alpha);
		R.put(var, beta);
	}
	
	// Eliminates left recursion. Unexpected behavior if the grammar
	// contains a unit rule, where the variable associated with the unit
	// rule did not produce epsilon.
	public void eliminateLeftRecursion() {
		if(containsEpsilon.size() > 0)
			removeEpsilon();
		
		ArrayList<String> tempV = new ArrayList<>(V);
		for(int i = 0; i < tempV.size(); ++i) {
			for(int j = 0; j < i; ++j) {
				ArrayList<String> rules = R.get(tempV.get(i));
				ArrayList<String> additions = new ArrayList<>();
				ListIterator<String> iter = rules.listIterator();
				while(iter.hasNext()) {
					String rule = iter.next();
					if(rule.startsWith(tempV.get(j))) {
						additions.addAll(substituteVars(
								rule.replaceFirst("^"+tempV.get(j), ""),
								R.get(tempV.get(j))));
						iter.remove();
					}
				}
				rules.addAll(additions);
			}
			eliminateImmediateLeftRec(V.get(i));
		}
	}
	
	private void computeFirst() {
		first = new HashMap<String, HashSet<String>>();
		for(String terminal: alpha)
			first.put(terminal, new HashSet<>(Arrays.asList(terminal.split(","))));
		
		for(String var: V)
			first.put(var, new HashSet<>());
		
		boolean change = true;
		
		while(change) {
			change = false;
			
			for(String v: V)
				for(String rule: R.get(v)) {
					String[] tokens = rule.split("@");
					HashSet<String> acc =  new HashSet<>(first.get(tokens[0]));
					
					for(int i = 1; i < tokens.length; ++i)
						acc.retainAll(first.get(tokens[i]));
					
					if(acc.contains("$") || rule.equals("$")) {
						if(!(first.get(v).contains("$"))) {
							first.get(v).add("$");
							change = true;
						}
					}else {
						for(int i = 0; i < tokens.length; ++i) {
							HashSet<String> cacc =  new HashSet<>(first.get(tokens[0]));
							for(int j = 0; j < i - 1; ++j)
								cacc.retainAll(first.get(tokens[j]));
							
							if(i == 0 || cacc.contains("$")) {
								HashSet<String> cFirst = new HashSet<String>(first.get(tokens[i]));
								cFirst.remove("$");
								if(!(first.get(v).containsAll(cFirst))) {
									first.get(v).addAll(cFirst);
									change = true;
								}
							}
						}
					}
				}
		}
	}
	
	private void computeFollow() {
		follow = new HashMap<>();
		if(first == null)
			computeFirst();
		
		for(String var: V)
			follow.put(var, new HashSet<>());
		
		follow.get(S).add("&");
		boolean change = true;
		while(change) {
			change = false;
			
			for(String v: V) {
				for(String rule: R.get(v)) {
					if(!(alpha.contains(rule))) {
						String[] tokens = rule.split("@");
						for(int i = 0; i < tokens.length; ++i) {
							if(V.contains(tokens[i])) {
								String beta = substringArray(tokens, i+1, tokens.length-1);
								HashSet<String> fbeta = null;
								if(!(beta.equals(""))) {
									fbeta = first(beta);
								}
								else {
									fbeta = new HashSet<>();
									fbeta.add("$");
								}
								HashSet<String> temp = new HashSet<>(fbeta);
								temp.remove("$");
								
								if(!(follow.get(tokens[i]).containsAll(temp))) {
									follow.get(tokens[i]).addAll(temp);
									change = true;
								}
								
								if(fbeta.contains("$")) {
									if(!(follow.get(tokens[i]).containsAll(follow.get(v)))) {
										follow.get(tokens[i]).addAll(follow.get(v));
										change = true;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public HashSet<String> first(String syntantialForm) {
		String[] tokens = syntantialForm.split("@");
		HashSet<String> res = new HashSet<>();
		boolean allEpsilon = false;
		for(int i = 0; i < tokens.length; ++i) {
			if(alpha.contains(tokens[i])) {
				res.add(tokens[i]);
				allEpsilon = false;
				break;
			}else if(containsEpsilon.contains(tokens[i])) {
				HashSet<String> temp = new HashSet<String>(first.get(tokens[i]));
				temp.remove("$");
				res.addAll(temp);
				allEpsilon = true;
			}else {
				HashSet<String> temp = new HashSet<String>(first.get(tokens[i]));
				temp.remove("$");
				res.addAll(temp);
				allEpsilon = false;
				break;
			}
		}
		if(allEpsilon)
			res.add("$");
		return res;
	}
	
	private static String substringArray(String[] a, int lo, int hi) {
		StringBuilder res = new StringBuilder();
		for(int i = lo; i <= hi; ++i) {
			res.append(a[i]);
			if(lo != hi)
				res.append("@");
		}
		return res.toString();
	}
	
	public HashMap<String, HashSet<String>> getFirst() {
		if(first == null)
			computeFirst();
		return first;
	}

	public HashMap<String, HashSet<String>> getFollow() {
		if(follow == null)
			computeFollow();
		return follow;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n---------------------\n");
		sb.append("Start variable: ");
		sb.append(S);
		sb.append("\nVariables: ");
		sb.append(V.toString());
		sb.append("\nAlphabet: ");
		sb.append(alpha.toString());
		sb.append("\nRules: ");
		sb.append(R.toString());
		sb.append("\n---------------------\n");
		
		return sb.toString();
	}
	
	public static void testLeftRecElimination(CFG in) {
		in.eliminateLeftRecursion();
		System.out.println(in);
	}
	
	public static void leftRecTest() {
		HashMap<String, ArrayList<String>> r1 = new HashMap<>();
		r1.put("S", new ArrayList<String>(Arrays.asList("S@a,b".split(","))));
		CFG _1 = new CFG("S", new ArrayList<String>(Arrays.asList("S".split(",")))
				, new ArrayList<String>(Arrays.asList("a,b".split(","))), 
				r1, new HashMap<String, ArrayList<String>>(0), new ArrayList<String>(0));
		
		HashMap<String, ArrayList<String>> r2 = new HashMap<>();
		r2.put("S", new ArrayList<String>(Arrays.asList("S@a@b,c@d".split(","))));
		CFG _2 = new CFG("S", new ArrayList<String>(Arrays.asList("S".split(",")))
				, new ArrayList<String>(Arrays.asList("a,b,c,d".split(","))), 
				r2, new HashMap<String, ArrayList<String>>(0), new ArrayList<String>(0));
		
		HashMap<String, ArrayList<String>> r3 = new HashMap<>();
		r3.put("S", new ArrayList<String>(Arrays.asList("S@u@S,S@S,"
				+ "S@*,(S),a".split(","))));
		CFG _3 = new CFG("S", new ArrayList<String>(Arrays.asList("S".split(",")))
				, new ArrayList<String>(Arrays.asList("a,u,*,(,)".split(","))), 
				r3, new HashMap<String, ArrayList<String>>(0), new ArrayList<String>(0));
		
		HashMap<String, ArrayList<String>> r4 = new HashMap<>();
		r4.put("rexpr", new ArrayList<String>(Arrays.asList("rexpr@u@rterm,rterm".split(","))));
		r4.put("rterm", new ArrayList<String>(Arrays.asList("rterm@rfactor,rfactor"
				.split(","))));
		r4.put("rfactor", new ArrayList<String>(Arrays.asList("rfactor@*,rprimary".split(","))));
		r4.put("rprimary", new ArrayList<String>(Arrays.asList("a,b".split(","))));
		CFG _4 = new CFG("rexpr", new ArrayList<String>(Arrays.asList(
				"rexpr,rterm,rfactor,rprimary".split(",")))
				, new ArrayList<String>(Arrays.asList("a,u,*,b".split(","))), 
				r4, new HashMap<String, ArrayList<String>>(0), new ArrayList<String>(0));
		
		HashMap<String, ArrayList<String>> r5 = new HashMap<>();
		r5.put("A", new ArrayList<String>(Arrays.asList("0,T@1".split(","))));
		r5.put("T", new ArrayList<String>(Arrays.asList("1,A@0".split(","))));
		CFG _5 = new CFG("A", new ArrayList<String>(Arrays.asList("A,T".split(",")))
				, new ArrayList<String>(Arrays.asList("0,1".split(","))), 
				r5, new HashMap<String, ArrayList<String>>(0), new ArrayList<String>(0));
		
		HashMap<String, ArrayList<String>> r6 = new HashMap<>();
		r6.put("A", new ArrayList<String>(Arrays.asList("B@C".split(","))));
		r6.put("B", new ArrayList<String>(Arrays.asList("B@b".split(","))));
		r6.put("C", new ArrayList<String>(Arrays.asList("A@C,a".split(","))));
		HashMap<String, ArrayList<String>> _6idx = new HashMap<>();
		_6idx.put("B", new ArrayList<String>(Arrays.asList("A,B".split(","))));
		CFG _6 = new CFG("A", new ArrayList<String>(Arrays.asList("A,B,C".split(",")))
				, new ArrayList<String>(Arrays.asList("a,b".split(","))), 
				r6, _6idx, 
				new ArrayList<String>(Arrays.asList("B".split(","))));
		
		testLeftRecElimination(_1);
		testLeftRecElimination(_2);
		testLeftRecElimination(_3);
		testLeftRecElimination(_4);
		testLeftRecElimination(_5);
		testLeftRecElimination(_6);
	}
	
	public static void printFirstAndFollow(CFG in) {
		System.out.println(in);
		System.out.println("First:");
		System.out.println(in.getFirst());
		System.out.println("Follow:");
		System.out.println(in.getFollow());
	}
	
	public static void main(String[] args) {
//		leftRecTest();
		
		HashMap<String, ArrayList<String>> r1 = new HashMap<>();
		r1.put("S", new ArrayList<String>(Arrays.asList("a@S@b,T".split(","))));
		r1.put("T", new ArrayList<String>(Arrays.asList("a@T,$".split(","))));
		CFG _1 = new CFG("S", new ArrayList<String>(Arrays.asList("S,T".split(",")))
				, new ArrayList<String>(Arrays.asList("a,b,$".split(","))), 
				r1, null, new ArrayList<String>(Arrays.asList("T".split(","))));
		printFirstAndFollow(_1);

		HashMap<String, ArrayList<String>> r2 = new HashMap<>();
		r2.put("E", new ArrayList<String>(Arrays.asList("T@_E".split(","))));
		r2.put("_E", new ArrayList<String>(Arrays.asList("+@T@_E,$".split(","))));
		r2.put("T", new ArrayList<String>(Arrays.asList("F@_T".split(","))));
		r2.put("_T", new ArrayList<String>(Arrays.asList("*@F@_T,$".split(","))));
		r2.put("F", new ArrayList<String>(Arrays.asList("(@E@),id".split(","))));
		CFG _2 = new CFG("E", new ArrayList<String>(Arrays.asList("E,_E,T,_T,F".split(",")))
				, new ArrayList<String>(Arrays.asList("+,*,(,),id,$".split(","))), 
				r2, null, new ArrayList<String>(Arrays.asList("_E,_T".split(","))));
		printFirstAndFollow(_2);
		
		HashMap<String, ArrayList<String>> r3 = new HashMap<>();
		r3.put("S", new ArrayList<String>(Arrays.asList("0@T@1@S,$".split(","))));
		r3.put("T", new ArrayList<String>(Arrays.asList("0@T@1,$".split(","))));
		CFG _3 = new CFG("S", new ArrayList<String>(Arrays.asList("S,T".split(",")))
				, new ArrayList<String>(Arrays.asList("0,1,$".split(","))), 
				r3, null, new ArrayList<String>(Arrays.asList("S,T".split(","))));
		printFirstAndFollow(_3);
		
		HashMap<String, ArrayList<String>> r4 = new HashMap<>();
		r4.put("S", new ArrayList<String>(Arrays.asList("S@A@B,S@B@C,$".split(","))));
		r4.put("A", new ArrayList<String>(Arrays.asList("a@A@a,$".split(","))));
		r4.put("B", new ArrayList<String>(Arrays.asList("b@B,$".split(","))));
		r4.put("C", new ArrayList<String>(Arrays.asList("c@C,$".split(","))));
		CFG _4 = new CFG("S", new ArrayList<String>(Arrays.asList("S,A,B,C".split(",")))
				, new ArrayList<String>(Arrays.asList("a,b,c,$".split(","))), 
				r4, null, new ArrayList<String>(Arrays.asList("S,A,B,C".split(","))));
		printFirstAndFollow(_4);
		
		HashMap<String, ArrayList<String>> r5 = new HashMap<>();
		r5.put("S", new ArrayList<String>(Arrays.asList("(@L@),a".split(","))));
		// , WILL BE *
		r5.put("L", new ArrayList<String>(Arrays.asList("L@*@S,S".split(","))));
		CFG _5 = new CFG("S", new ArrayList<String>(Arrays.asList("S,L".split(",")))
				, new ArrayList<String>(Arrays.asList("a,(,),*".split(","))), 
				r5, null, new ArrayList<String>());
		System.out.println("CHECK COMMENT AT LINE: 407!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		printFirstAndFollow(_5);
		
		HashMap<String, ArrayList<String>> r6 = new HashMap<>();
		r6.put("S", new ArrayList<String>(Arrays.asList("S@S@+,S@S@*,a".split(","))));
		CFG _6 = new CFG("S", new ArrayList<String>(Arrays.asList("S".split(",")))
				, new ArrayList<String>(Arrays.asList("a,*,+".split(","))), 
				r6, null, new ArrayList<String>());
		printFirstAndFollow(_6);
	}

}
