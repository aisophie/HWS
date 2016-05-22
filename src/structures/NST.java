package structures;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map.Entry;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * Node Selection Tree
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-05-19
 */

public class NST implements Serializable{
	public DefaultMutableTreeNode root = new DefaultMutableTreeNode("<B>-R");
	public String assumption;
	public int order;
	public HashMap<String,Integer> dist;
	public int V = 0;

	/**
	 * Get Unigrams from sentences (with unifiication).
	 * @param sens Input sentences.
	 * @return unigrams.
	 **/
	private HashMap<String,Integer> getUnigram(ArrayList<String[]> sens){
		HashMap<String,Integer> unigram = new HashMap<String,Integer>();
		for(String[] sen:sens){
			Set<String> senset = new HashSet<String>(Arrays.asList(sen));
			sen = senset.toArray(new String[senset.size()]);
			for(String token:sen){
				if(unigram.containsKey(token)){
					int count = unigram.get(token);
					unigram.put(token,count+1);
				}else{
					unigram.put(token,1);
				}
			}
		}
		return unigram;
	}

	private class Pair{
		private String head;
		private int ccw;

		public Pair(String head, int ccw){
			this.head = head;
			this.ccw = ccw;
		}
	}

	/**
	 * Get the next node from (sub)sentences.
	 * @param parent parentnode(context).
	 * @param cc count of context.
	 * @param sens (sub)sentences.
	 * @return the next node and the count of accumulated contexts;
	 **/
	private Pair getNode(DefaultMutableTreeNode parent,int cc,ArrayList<String[]> sens){
		HashMap<String,Integer> unigram = getUnigram(sens);
		Iterator entries = unigram.entrySet().iterator();
		String head = "";
		double max = 0.0;
		int headccw = 0;
		while(entries.hasNext()){
			Entry entry = (Entry)entries.next();
			String w = (String)entry.getKey();
			int ccw = (Integer)entry.getValue();
			int cw = this.dist.get(w);
			String parentCore = parent.toString().substring(0,parent.toString().length()-2);
			cc = this.dist.get(parentCore);
			//calculate by certain score
			double score = 0.0;
			if(this.assumption.equals("FB")) score = ccw;
			else if(this.assumption.equals("MIB")) score = (cw/this.V) * Math.log((ccw*this.V)/(cc*cw));
			else if(this.assumption.equals("DB")) score = 2.0 * ccw / (cc + cw);
			else if(this.assumption.equals("TB")) score = (ccw-cc*cw/this.V) / Math.pow(ccw,0.5);
			else{
				System.out.println("Assumption used for 'NST' mode has to be 'FB','DB' or 'TB'");
				System.exit(-1);
			}
			//update max score
			if(score>max){
				max = score;
				head = w;
				headccw = ccw;
			}
		}
		Pair pair = new Pair(head,headccw);
		return pair;
	}

	/**
	 * Constuct a NST recursivly.
	 * @param parent parentnode(context).
	 * @param cc count of context.
	 * @param sens (sub)sentences.
	 **/
	private void getTree(DefaultMutableTreeNode parent,int cc,ArrayList<String[]> sens){
		if(sens.size()==0) return;
		//get head node
		Pair pair = getNode(parent,cc,sens);
		String head = pair.head;
		int ccw = pair.ccw;
		//System.out.format("%s,%s,%s\n",parent,head,ccw);
		DefaultMutableTreeNode childL = new DefaultMutableTreeNode(head+"-L");
		DefaultMutableTreeNode childR = new DefaultMutableTreeNode(head+"-R");
		DefaultMutableTreeNode childN = new DefaultMutableTreeNode(head+"-N");
		//split each sentence
		ArrayList<String[]> L = new ArrayList<String[]>();
		ArrayList<String[]> R = new ArrayList<String[]>();
		ArrayList<String[]> N = new ArrayList<String[]>();
		for(String[] sen:sens){
			if(!Arrays.asList(sen).contains(head)){
				N.add(sen);
			}else{
				int ind = Arrays.asList(sen).indexOf(head);
				String[] l = Arrays.copyOfRange(sen,0,ind);
				String[] r = Arrays.copyOfRange(sen,ind+1,sen.length);
				if(l.length>0) L.add(l);
				if(r.length>0) R.add(r);
			}
		}
		//attach nodes
		parent.add(childL);
		parent.add(childR);
		parent.add(childN);
		//recurse
		getTree(childL,ccw,L);
		getTree(childR,ccw,R);
		getTree(childN,ccw,N);
	}

	/**
	 * Initiation Function.
	 * @param sentences Sentences used for converting to sequences.
	 */
	public void init(String[] sentences){
		ArrayList<String[]> sens = new ArrayList<String[]>();
		for(String sen:sentences){
			String[] words = sen.split(" ");
			sens.add(words);
		}
		this.dist = getUnigram(sens);
		this.dist.put("<B>",sentences.length);
		for(int i:this.dist.values()) this.V+=i;
		getTree(this.root,this.dist.get("<B>"),sens);
	}

	/**
	 * Re-Initiation Function.
	 * @param sentences sentences used for modifiying unigrams.
	 */
	public void reinit(String[] sentences){
		ArrayList<String[]> sens = new ArrayList<String[]>();
		for(String sen:sentences){
			String[] words = sen.split(" ");
			sens.add(words);
		}
		HashMap<String,Integer> newdist = getUnigram(sens);
		newdist.put("<B>",sentences.length);
		for(Object k:newdist.keySet()){
			String key = (String)k;
			int value = (int)newdist.get(key);
			if(this.dist.containsKey(key)){
				this.dist.put(key,this.dist.get(key)+value);
			}else{
				this.dist.put(key,value);
			}
		}
	}

	/**
	 * Construct Function.
	 *
	 * @param assumption The assumption used for sequences convertion.
	 * @param order The order of word sequences. 
	 **/
	public NST(String assumption,int order){
		this.assumption = assumption;
		this.order = order;
	}


	/**
	 * Divide a (sub)sentence recursively by frequncy as a cover of NST method
	 *
	 * @param subtree subtree of a NST.
	 * @param tokens (sub)tokens of original sentence.
	 **/
	private void byFreq(String[] tokens,String cxt,ArrayList<String> seqs){
		//no more tokens
		if(tokens.length==0){
			String[] cxtTokens = cxt.split(" ");
			String realCxt = StringUtils.join(Arrays.copyOfRange(cxtTokens,Math.max(0,cxtTokens.length-this.order+1),cxtTokens.length)," ");
			seqs.add(realCxt+" <E>");
			return;
		}
		String child = "";
		int count = 0;
		for(String token:tokens){
			if(this.dist.get(token) > count){
				count = this.dist.get(token);
				child = token;
			}
		}
		int index = Arrays.asList(tokens).indexOf(child);
		String[] cxtTokens = cxt.split(" ");
		String realCxt = StringUtils.join(Arrays.copyOfRange(cxtTokens,Math.max(0,cxtTokens.length-this.order+1),cxtTokens.length)," ");
		seqs.add(realCxt+" "+child);
		String[] L = Arrays.copyOfRange(tokens,0,index);
		String[] R = Arrays.copyOfRange(tokens,index+1,tokens.length);
		byFreq(L,cxt+" "+child+"-L",seqs);
		byFreq(R,cxt+" "+child+"-R",seqs);
	}

	/**
	 * Divide a (sub)sentence recursively by a trained NST.
	 *
	 * @param subtree subtree of a NST.
	 * @param tokens (sub)tokens of original sentence.
	 * @param cxt the context of the next node.
	 **/
	public void divideSen(DefaultMutableTreeNode subtree,String[] tokens,String cxt,ArrayList<String> seqs){
		//no more tokens
		if(tokens.length==0){
			String[] cxtTokens = cxt.split(" ");
			String realCxt = StringUtils.join(Arrays.copyOfRange(cxtTokens,Math.max(0,cxtTokens.length-this.order+1),cxtTokens.length)," ");
			seqs.add(realCxt+" <E>");
			return;
		}
		//no more subtree
		int childCount = subtree.getChildCount();
		if(childCount==0){
			//by freq
			byFreq(tokens,cxt,seqs);
			return;
		}
		//enumerate nst tree chldren
		DefaultMutableTreeNode lTree = new DefaultMutableTreeNode();
		DefaultMutableTreeNode rTree = new DefaultMutableTreeNode();
		DefaultMutableTreeNode nTree = new DefaultMutableTreeNode();
		Enumeration children = subtree.children();
		while(children.hasMoreElements()){
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
			if(child.toString().endsWith("-L")) lTree = child;
			else if(child.toString().endsWith("-R")) rTree = child;
			else nTree = child;
		}
		//get the next node name in the NST
		String nextNode = ((DefaultMutableTreeNode)subtree.getFirstChild()).toString();
		nextNode = nextNode.substring(0,nextNode.length()-2);
		int index = Arrays.asList(tokens).indexOf(nextNode);
		if(index==-1){
			divideSen(nTree,tokens,cxt,seqs);
		}else{
			String[] cxtTokens = cxt.split(" ");
			String realCxt = StringUtils.join(Arrays.copyOfRange(cxtTokens,Math.max(0,cxtTokens.length-this.order+1),cxtTokens.length)," ");
			seqs.add(realCxt+" "+nextNode);
			String[] L = Arrays.copyOfRange(tokens,0,index);
			String[] R = Arrays.copyOfRange(tokens,index+1,tokens.length);
			divideSen(lTree,L,cxt+" "+nextNode+"-L",seqs);
			divideSen(rTree,R,cxt+" "+nextNode+"-R",seqs);
		}
	}

	/**
	 * Convert raw sentences into sequences using a trained NST.
	 * @param sen A sentence.
	 **/
	public ArrayList<String> convert(String sen){
		ArrayList<String> seqs = new ArrayList<String>();
		String[] words = sen.split(" ");
		divideSen(this.root,words,"<B>",seqs);
		return seqs;
	}
}
