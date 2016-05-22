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
 * Abstraction-based HWS structure
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-04-14
 */


public class ABS extends HWS{

	private static void count(HashMap<String,HashMap<String,Float>> map,String cxt,String child){
		if(map.containsKey(cxt)){
			HashMap<String,Float> childMap = map.get(cxt);
			if(childMap.containsKey(child)){
				childMap.put(child,childMap.get(child)+1.0f);
			}else{
				childMap.put(child,1.0f);
			}
			map.put(cxt,childMap);
		}else{
			HashMap<String,Float> childMap = new HashMap<String,Float>();
			childMap.put(child,1.0f);
			map.put(cxt,childMap);
		}
	}

	public void getExtraMap(String[] sens,HashMap loadedMap){
		HashMap<String,HashMap<String,Float>> absMap = new HashMap<String,HashMap<String,Float>>();

		if(loadedMap!=null){
			absMap = (HashMap<String,HashMap<String,Float>>)loadedMap;
		}else{
			absMap = new HashMap<String,HashMap<String,Float>>();
		}

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
			//for each word
			String[] tokens = sen.split(" ");
			for(int i=0;i<tokens.length;i++){
				String token = tokens[i];
				count(absMap,"<s>-R",token);
				count(absMap,"<s>-R","|C|");
				count(absMap,"</s>-L",token);
				count(absMap,"</s>-L","|C|");
				Set<String> L = new HashSet<String>(Arrays.asList(Arrays.copyOfRange(tokens,0,i)));
				Set<String> R = new HashSet<String>(Arrays.asList(Arrays.copyOfRange(tokens,i+1,tokens.length)));
				for(String l:L){
					count(absMap,token+"-L",l);
					count(absMap,token+"-L","|C|");
				}
				for(String r:R){
					count(absMap,token+"-R",r);
					count(absMap,token+"-R","|C|");
				}
			}
		}
		//prune
		for(Object key:absMap.keySet()){
			HashMap childMap = (HashMap)absMap.get(key);
			ArrayList<String> removes = new ArrayList<String>();
			for(Object child:childMap.keySet()){
				if(((String)child).equals("|C|")) continue;
				float freq = (Float)childMap.get(child);
				if(freq==1.0) removes.add((String)child);
			}
			for(String remove:removes) childMap.remove(remove);
		}
		extraMap = absMap;
	}

	public HashMap toCellMap(String[] tokens){
		initCellMap();
		ArrayList<Integer> completed = new ArrayList<Integer>();	
		while(completed.size()<tokens.length){
			int idx = 0;
			float min = 1.0f;
			for(int i=0;i<tokens.length;i++){
				if(completed.contains(i)) continue;
				String token = tokens[i];
				int l = i;
				int r = i;
				while(l==i || completed.contains(l)) l -= 1;
				while(r==i || completed.contains(r)) r += 1;
				String L = (l==-1) ? "<s>" : tokens[l]; 
				String R  = (r==tokens.length) ? "</s>" : tokens[r];
				HashMap LChildMap = (HashMap)this.extraMap.get(L+"-R");
				HashMap RChildMap = (HashMap)this.extraMap.get(R+"-L");
				float LSum = (Float)LChildMap.get("|C|");
				float RSum = (Float)RChildMap.get("|C|");
				float LFreq;
				float RFreq;
				try{
					LFreq = (Float)LChildMap.get(token);
				}catch(Exception e){
					LFreq = 1.0f;
				}
				try{
					RFreq = (Float)RChildMap.get(token);
				}catch(Exception e){
					RFreq = 1.0f;
				}
				float pr = RFreq / RSum;
				float pl = LFreq / LSum;
				float p = 2 * pr * pl / (pr + pl);
				if(p<min){
					min = p;
					idx = i;
				}
				//System.out.format("%s %s\n",token,p);
			}
			putCell(tokens.length-completed.size()-1,idx,tokens[idx]);
			completed.add(idx);
		}
		return cellMap;
	}
}
