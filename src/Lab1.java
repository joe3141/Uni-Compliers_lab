import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class Lab1 {
	
	public static void validateStateTransitions(DFA curr, 
			Map<String, HashMap<String, String>> _transitionFunction) {
		
		for(String s: curr.states){
			for(String l: curr.alphabet){
				if(_transitionFunction.containsKey(s)
						&& !(_transitionFunction.get(s).containsKey(l))        							){
					curr.inValidate();
					curr.errors.add("Missing transition for state "
							+ s);
				}
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ArrayList<DFA> parse(String path, boolean dfa) throws IOException{
		boolean close_to_end = false;
		int state = 0;
		DFA curr = null;
		ArrayList res;
		
		if(dfa) {
			res = new ArrayList<DFA>();
			curr = new DFA();
		}else {
			res = new ArrayList<NFA>();
			curr = new NFA();
		}

		try(BufferedReader br = new BufferedReader(new FileReader(path))) {
		    for(String line; (line = br.readLine()) != null; ) {
//		    	System.out.println(line);
		    	if(!(state == 1 && line.isEmpty())) {
			        if((line.isEmpty()) && close_to_end){
			        	System.out.println("hahaha");
			        	return res;
			        }
			        if(line.isEmpty()){
			        	close_to_end = true;
			        	continue;
			        }
		    	}
		        switch(state){
		        case 0:
		        	if(dfa)
		        		curr = new DFA();
		        	else
		        		curr = new NFA();
		        	close_to_end = false;
		        	curr.states = new ArrayList<String>(Arrays.asList(
		        			line.split(",")));
		        	break;
		        
		        case 1:
		        	String[] states = line.split(",");
		        	String[] actions = br.readLine().split(",");
		        	if(!line.isEmpty()) 
			        	for(int i = 0; i < states.length; ++i)
			        		if(!(curr.states.contains(states[i]))){
			        			curr.inValidate();
			        			curr.errors.add("Invalid accept state " + states[i]);
			        		}
		        	if(actions.length != states.length) {
		        		curr.inValidate();
	        			curr.errors.add("Invalid number of actions");
		        	}
		        	if(curr.valid) {
		        		curr.accepted = new ArrayList<String>(Arrays.asList(
		        				states));
		        		curr.actions = new HashMap<String, HashMap<String, Integer>>();
		        		for(int i = 0; i < actions.length; ++i) {
		        			HashMap<String, Integer> tmp = new HashMap<String, Integer>();
		        			tmp.put(actions[i], i);
		        			curr.actions.put(states[i], tmp);
		        		}
		        	}
		        	break;
		        
		        case 2:
		        	curr.alphabet = new ArrayList<String>(Arrays.asList(
		        			line.split(",")));
		        	if(!dfa)
		        		curr.alphabet.add("$");
		        	break;
		        
		        case 3:
		        	if(curr.states.contains(line))
		        		curr.start = line;
		        	else{
		        		curr.inValidate();
		        		curr.errors.add("Invalid start state " + line);
		        	}
		        	break;
		        
		        case 4:
		        	Map _transitionFunction;
		        	if(dfa) {
		        		_transitionFunction = new HashMap<String, HashMap<String, String>>();
			        }
		        	else {
		        		_transitionFunction = new HashMap<String, HashMap<String, 
		        				ArrayList<String>>>();
		        	}
		        	String[] transitions = line.split("#");
		        	
		        	for(int i = 0; i < transitions.length; ++i){
		        		String[] transition = transitions[i].split(",");
		        		if(transition.length < 3){
		        			curr.inValidate();
		        			curr.errors.add("Incomplete Transition " + 
		        			transitions[i]);
		        		}else if(!(curr.states.contains(transition[0]))){
		        			curr.inValidate();
		        			curr.errors.add("Invalid Transition " + 
				        		"state " + transition[0] + " does not exist");
		        		}else if(!(curr.states.contains(transition[1]))){
		        			curr.inValidate();
		        			curr.errors.add("Invalid Transition " + 
				        		"state " + transition[1] + " does not exist");
		        		}else if(!(curr.alphabet.contains(transition[2]))){
		        			curr.inValidate();
		        			curr.errors.add("Invalid Transition " + 
		        			"input " + transition[2] + " is not in the alphabet");
		        		}else if(transition.length > 3){
		        			curr.inValidate();
		        			curr.errors.add("Transition Too Long" + 
		        			transitions[i]);
		        		}else if(dfa){
		        			if(_transitionFunction.containsKey(transition[0])) {
		        				((HashMap<String,String>) _transitionFunction.get(transition[0])).put(
		        						transition[2], transition[1]);
		        			}else {
		        				HashMap<String, String> temp = new HashMap<String, String>();
		        				temp.put(transition[2], transition[1]);
		        				_transitionFunction.put(transition[0], temp);
		        			}
		        		}else {
		        			if(_transitionFunction.containsKey(transition[0])) {
		        				if(((HashMap<String,ArrayList<String>>) _transitionFunction.get(
		        						transition[0])).containsKey(transition[2]))
		        					
			        				((HashMap<String, ArrayList<String>>) _transitionFunction.get(
			        						transition[0])).get(transition[2]).add(transition[1]);
		        				else {
		        					ArrayList<String> t = new ArrayList<String>();
		        					t.add(transition[1]);
		        					((HashMap<String, ArrayList<String>>)_transitionFunction.get(transition[0])).
		        					put(transition[2], t);
		        				}
		        			}else {
		        				HashMap<String, ArrayList<String>> temp = 
		        						new HashMap<String, ArrayList<String>>();
		        				ArrayList<String> t = new ArrayList<String>();
		        				t.add(transition[1]);
		        				temp.put(transition[2], t);
		        				_transitionFunction.put(transition[0], temp);
		        			}
		        		}
		        	}
		        	
		        	if(dfa)
		        		validateStateTransitions(curr, _transitionFunction);
		        	
		        	if(curr.valid)
		        		if(dfa)
		        			curr.transitionFunction = _transitionFunction;
		        		else 
		        			if(curr instanceof NFA)
		        				((NFA) curr).ndTransitionFunction = _transitionFunction;
		        	break;
		        	
		        case 5:
		        	curr.inputs = new ArrayList<String>(Arrays.asList(
		        			line.split("#")));
		        	res.add(curr);
		        	break;
		        }
		        
		        ++state;
		        state%=6;
		    }
		}catch(IOException e){
			System.out.println(e.toString());
		}
		
		return res;
	}
	
	public static void main(String[] args) throws IOException{
		
		//"Lab1/NFA/in1.in"
		ArrayList<DFA> in = parse("Lab3/ins/in0.in", false);
		ArrayList<DFA> converted_nfas = new ArrayList<DFA>();
	
		for(DFA n: in) {
			NFA nn = null;
			if(n instanceof NFA)
				nn = (NFA) n;
			converted_nfas.add(nn.toDFA());
		}

		Path file = Paths.get("Lab3/my_outs/out1.out");
		ArrayList<String> acc = new ArrayList<String>();
		for(DFA dfa: converted_nfas){
			acc.addAll(dfa.testAllFallback());
			acc.add("\n");
		}
		Files.write(file, acc, 
				Charset.forName("UTF-8"));
	}
}
