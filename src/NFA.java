import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class NFA extends DFA{
	
	// Non-deterministic transition function. 
	public Map<String, HashMap<String, ArrayList<String>>> ndTransitionFunction; 
	
	public NFA() {
		super();
	}
	
	public String test(String in) {
		return null;
	}
	
	public ArrayList<String> testAll(){
		return null;
	}
	
	public String[] getEClosure(String s) {		
		HashSet<String> vis = new HashSet<String>();
		Queue<String> q = new LinkedList<String>();
		
		
		
		q.add(s);
		vis.add(s);
		
		if(ndTransitionFunction.get(s) != null)
		if(ndTransitionFunction.get(s).containsKey("$"))
			while(!q.isEmpty()) {
				String curr = q.remove();
				if(ndTransitionFunction.get(curr).containsKey("$"))
					for(String state:  ndTransitionFunction.get(curr).get("$")) {
						if(!vis.contains(state)) {
							vis.add(state);
							q.add(state);
						}
					}
			}
		String [] acc = vis.toArray(new String[vis.size()]);
		Arrays.sort(acc);
		return acc;
	}
	
	public DFA toDFA() {
		DFA res = new DFA();
		res.alphabet = this.alphabet; // This contains $, however it's not a big deal.
		res.inputs = this.inputs;
		res.wasNFA = true;
		
		if(!valid) {
			res.inValidate();
			res.errors = this.errors;
			return res;
		}
		
		// Takes a state from the NFA and returns its Epsilon closure,
		// if previously computed.
		Map<String, String[]> epsilonClosure = new HashMap<String, String[]>();
		
		Map<String, HashMap<String, String>> _transitionFunction = new HashMap<String, 
				HashMap<String, String>>();
		ArrayList<String> _states = new ArrayList<String>();
		ArrayList<String> _accepted = new ArrayList<String>();
		
		epsilonClosure.put(start, getEClosure(start));
		String _start = join(epsilonClosure.get(start), "*");
		_states.add(_start);
		if(accepted.contains(start) || containsAny(accepted, _start.split("\\*")))
			_accepted.add(_start);
		
		Queue<String> q = new LinkedList<String>();
		HashSet<String> vis = new HashSet<String>();
		
		// TODO: Dead state
		// TODO: add to accepted
		// TODO: enqueue
		q.add(_start);
		while(!q.isEmpty()) {
			String curr = q.remove();
			vis.add(curr);
			
			String [] individual_states = curr.split("\\*");
			
			for(int k = 0; k < alphabet.size(); ++k) {
				String input = alphabet.get(k);
				if(!input.equals("$")) {
					HashSet<String> acc = new HashSet<String>();
					for(int i = 0; i < individual_states.length; ++i) {
						ArrayList<String> destinations = null;

						if(ndTransitionFunction.get(individual_states[i]) != null
								&& ndTransitionFunction.get(individual_states[i])
								.containsKey(input)) {
							destinations = ndTransitionFunction.get(
									individual_states[i]).get(input);
						}
						if(destinations != null)
							for(String d: destinations) {
								String[] d_closure;
								if(!epsilonClosure.containsKey(d)) {
									d_closure = getEClosure(d);
									epsilonClosure.put(d, d_closure);
								}
								else {
									d_closure = epsilonClosure.get(d);
								}
								for(int j = 0; j < d_closure.length; ++j)
									acc.add(d_closure[j]);
							}
					}
					
					if(!acc.isEmpty()) {
						String[] ac = acc.toArray(new String[acc.size()]);
						Arrays.sort(ac);
						String gen_state = join(ac, "*");
						
						if(!vis.contains(gen_state)) {
							if(containsAny(accepted, ac))
								_accepted.add(gen_state);
							_states.add(gen_state);
							q.add(gen_state);
						}
						
						if(!_transitionFunction.containsKey(curr)) {
							HashMap<String, String> temp = new HashMap<String, String>();
							temp.put(input, gen_state);
							_transitionFunction.put(curr, temp);
						}else {
							_transitionFunction.get(curr).put(input, gen_state);
						}
					}else {
						if(!_states.contains("dead")) {
							_states.add("dead");
							HashMap<String, String> dtmp = new HashMap<String, String>();
							
							for(int z = 0; z < alphabet.size(); ++z)
								if(!alphabet.get(z).equals("$"))
									dtmp.put(alphabet.get(z), "dead");
							_transitionFunction.put("dead", dtmp);		
						}
						
						if(!_transitionFunction.containsKey(curr)) {
							HashMap<String, String> temp = new HashMap<String, String>();
							temp.put(input, "dead");
							_transitionFunction.put(curr, temp);
						}else {
							_transitionFunction.get(curr).put(input, "dead");
						}
					}
				}
			}
		}
		
		res.transitionFunction = _transitionFunction;
		res.accepted = _accepted;
		res.states = _states;
		res.start = _start;
		
		HashMap<String, HashMap<String, Integer>> _actions = 
				new HashMap<String, HashMap<String, Integer>>();
		for(int i = 0; i < res.accepted.size(); ++i) {
			String[] individual_states = res.accepted.get(i).split("\\*");
			String action = "";
			int min = (int) 1e31;
			for(int j = 0; j < individual_states.length; ++j) {
				if(actions.containsKey(individual_states[j])) {
					for(String action1: actions.get(individual_states[j]).keySet()) { // 1 action
						if(actions.get(individual_states[j]).get(action1) < min) {
							min = actions.get(individual_states[j]).get(action1);
							action = action1;
						}
					}
				}
				HashMap<String, Integer> tmp = new HashMap<String, Integer>();
				tmp.put(action, 0);
				_actions.put(res.accepted.get(i), tmp);
			}
		}
		res.actions = _actions;
		return res;
	}
	
	public boolean containsAny(ArrayList<String> accepted, String[] states) {
		for(int i = 0; i < states.length; ++i)
			if(accepted.contains(states[i]))
				return true;
		return false;
	}
	
	public String join(ArrayList<String> in, String delimiter) {
		if(in.size() == 0)
			return in.get(0);
		String acc = "";
		for(int i = 0; i < in.size()-1; ++i)
			acc += in.get(i) + delimiter; 
		return acc += in.get(in.size()-1);
	}
	
	public String join(String[] in, String delimiter) {
		return join(new ArrayList<String>(Arrays.asList(in)), "*");
	}
	
	
}
