import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class DFA {
	
	public ArrayList<String> states;
	public ArrayList<String> alphabet;
	public ArrayList<String> accepted;
	public String start;
	public Map<String, HashMap<String, String>> transitionFunction;
	public ArrayList<String> inputs;
	
	public boolean valid;
	public boolean wasNFA;
	public ArrayList<String> errors;
	
	// state => action
	public HashMap<String, HashMap<String, Integer>> actions;
	
	public DFA(){
		valid = true;
	}
	
	public void inValidate(){
		if(valid == true){
			valid = false;
			errors = new ArrayList<String>();
		}
	}
	
	
	public String test(String in){
		
		if(!valid)
			return "Ignored";
		
		String[] tokens = in.split(",");
		String curr = this.start;
		for(int i = 0; i < tokens.length; ++i){
			if(!(alphabet.contains(tokens[i])))
				return "Invalid input string at " + tokens[i];

			curr = transitionFunction.get(curr).get(tokens[i]);
		}
		
		for(String s: accepted){
			if(curr.equals(s))
				return "Accepted";
		}
		return "Rejected";
	}
	
	public String testFallback(String in){
		if(!valid)
			return "Ignored";
		
		Stack<String> stack = new Stack<String>();
		int l = 0, r = 0, last_acc_pos = -1;
		
		String[] tokens = in.split(",");
		String curr = this.start;
		String last_accept = "";
		StringBuilder res = new StringBuilder();
		
		for(l = 0; l < tokens.length; ++l){
			if(!(alphabet.contains(tokens[l])))
				return "Invalid input string at " + tokens[l];
			
			stack.push(curr);
			curr = transitionFunction.get(curr).get(tokens[l]);

			if(actions.containsKey(curr)) { 
				last_accept = curr;
				last_acc_pos = l;
			}
			
			if(l == tokens.length - 1) { // last input token
				if(actions.containsKey(curr)) 
					res.append("<" + actions.get(curr).keySet().toArray()[0] + ", \"" + substringArray(tokens,r, l) +
							"\">");
				else if(last_acc_pos == -1)
					res.append("<" + "error" + ", \"" + substringArray(tokens, r, l) +
							"\">");
				else {
					l = last_acc_pos + 1;
					res.append("<" + actions.get(last_accept).keySet().toArray()[0] + ", \"" + substringArray(tokens, r, l-1)
					+ "\">");
					r = l;
					curr = this.start;
					last_acc_pos = -1;
				}
			}
		}
		return res.toString();
	}
	
	public ArrayList<String> testAllFallback(){
		ArrayList<String> res = new ArrayList<String>();
		
		if(valid)
			if(!wasNFA)
				res.add("DFA constructed");
			else {
				res.add("NFA constructed");
				res.add("Equivalent DFA:");
				res.add(states.toString());
				res.add(accepted.toString());
				res.add(actions.toString());
				res.add(alphabet.toString());
				res.add(start);
				res.add(transitionFunction.toString());
				res.add(inputs.toString());
				res.add("\nDFA constructed\n");
			}
		else
			res.addAll(errors);
		
		for(int i = 0; i < inputs.size(); ++i) 
			res.add(testFallback(inputs.get(i)));
		
		return res;
	}
	
	public ArrayList<String> testAll(){
		ArrayList<String> res = new ArrayList<String>();
		if(valid)
			if(!wasNFA)
				res.add("DFA constructed");
			else {
				res.add("NFA constructed");
				res.add("Equivalent DFA:");
				res.add(states.toString());
				res.add(accepted.toString());
				res.add(alphabet.toString());
				res.add(start);
				res.add(transitionFunction.toString());
				res.add(inputs.toString());
				res.add("\nDFA constructed");
			}
		else
			res.addAll(errors);
		
		for(String in: inputs){
			res.add(test(in));
		}
		
		res.add("\n");
		return res;
	}
	
	public static String substringArray(String[] a, int lo, int hi) {
		StringBuilder res = new StringBuilder();
		for(int i = lo; i <= hi; ++i) {
			res.append(a[i]);
			if(lo != hi)
				res.append(",");
		}
		return res.toString();
	}
}