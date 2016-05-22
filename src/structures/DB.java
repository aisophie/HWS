package structures;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;
import java.util.Map;


/**
 * Dice-coefficient-based HWS structure
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-04-14
 */


public class DB extends HWS{

	public void getExtraMap(String[] sens,HashMap loadedMap){
		HashMap<String,HashMap<String,Integer>> waMap;
		if(loadedMap!=null){
			waMap = (HashMap<String,HashMap<String,Integer>>)loadedMap;
		}else{
			waMap = new HashMap<String,HashMap<String,Integer>>();
		}
		waMap.put("<s>",new HashMap<String,Integer>());
		waMap.get("<s>").put("|C|",sens.length);
		int counter = 0;
		int size = sens.length;
		for(String sen:sens){
			//progressbar
			double complete = (counter+1.0)/size*100.0;
			counter++;
			double incomplete = 100.0 - complete;
			System.out.print("\r[");
			for(int j=0;j<complete/2.0;j++) System.out.print("=");
			System.out.print(">");
			for(int k=0;k<incomplete/2.0;k++) System.out.print(" ");
			System.out.format("]%.2f",complete);
			if(complete==100.0) System.out.print("\n");
			//unification
			Set<String> tokens = new HashSet<String>(Arrays.asList(sen.split(" ")));
			for(String token:tokens){
				if(!waMap.containsKey(token)) waMap.put(token,new HashMap<String,Integer>());
				if(!waMap.get(token).containsKey("|C|")) waMap.get(token).put("|C|",0);
				waMap.get(token).put("|C|",waMap.get(token).get("|C|")+1);
				if(!waMap.get("<s>").containsKey(token)) waMap.get("<s>").put(token,0);
				waMap.get("<s>").put(token,waMap.get("<s>").get(token)+1);
				for(String token2:tokens){
					if(token.equals(token2)) continue;
					if(!waMap.get(token).containsKey(token2)) waMap.get(token).put(token2,0);
					waMap.get(token).put(token2,waMap.get(token).get(token2)+1);
				}
			}
		}
		this.extraMap = waMap;
	}

	private void getHead(HashMap<String,HashMap<String,Integer>> waMap,String cxt,String[] tokens,int row,int start,int end){
		if(start==end) return;
		//get head
		int cc = waMap.get(cxt).get("|C|");
		int idx = 0;
		double max = 0.0;
		for(int i=start;i<end;i++){
			if(tokens[i].equals(cxt)){
				idx = i;
				break;
			}
			int cw = waMap.get(tokens[i]).get("|C|");
			int ccw = waMap.get(cxt).get(tokens[i]);
			//System.out.format("%s %s %s\n",cc,cw,ccw);
			double dc = 2.0 * ccw / (cc + cw);
			if(dc>max){
				max = dc;
				idx = i;
			}
		}
		String head = tokens[idx];
		super.putCell(row,idx,head);
		getHead(waMap,head,tokens,row+1,start,idx);	
		getHead(waMap,head,tokens,row+1,idx+1,end);	
	}

	public HashMap toCellMap(String[] tokens){
		super.initCellMap();
		getHead(this.extraMap,"<s>",tokens,0,0,tokens.length);
		return this.cellMap;
	}

}
