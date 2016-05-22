package smoothing;

import java.util.HashMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;


/**
 * MKN smoothing
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-05-16
 */


public class MKN implements Serializable{
	public int order;
	public HashMap<Integer, HashMap<String,HashMap<String,Integer>>> cwMap = new HashMap<Integer, HashMap<String,HashMap<String,Integer>>>();
	public HashMap<Integer, HashMap<String,HashMap<String,Integer>>> bcwMap = new HashMap<Integer, HashMap<String,HashMap<String,Integer>>>();
	public HashMap<Integer,HashMap<String,HashMap<String,Integer>>> cMap = new HashMap<Integer, HashMap<String,HashMap<String,Integer>>>();
	public HashMap<Integer, HashMap<String,HashMap<String,Integer>>> bcMap = new HashMap<Integer, HashMap<String,HashMap<String,Integer>>>();
	public HashMap<String,HashMap<String,Integer>> nMap = new HashMap<String,HashMap<String,Integer>>();
	public HashMap<Integer,HashMap<String,Float>> dMap = new HashMap<Integer, HashMap<String,Float>>();
	public HashMap<String,Float> probCache = new HashMap<String,Float>();


	public MKN(int order){
		this.order = order;
		for(int i=1;i<=order;i++){
			HashMap<String,HashMap<String,Integer>> thisCwMap = new HashMap<String,HashMap<String,Integer>>();
			HashMap<String,HashMap<String,Integer>> thisBcwMap = new HashMap<String,HashMap<String,Integer>>();
			HashMap<String,HashMap<String,Integer>> thisCMap = new HashMap<String,HashMap<String,Integer>>();
			HashMap<String,HashMap<String,Integer>> thisBcMap = new HashMap<String,HashMap<String,Integer>>();
            HashMap<String,Float> thisDMap = new HashMap<String,Float>();
			this.cwMap.put(i,thisCwMap);
			this.bcwMap.put(i,thisBcwMap);
			this.cMap.put(i,thisCMap);
			this.bcMap.put(i,thisBcMap);
            this.dMap.put(i,thisDMap);
		}
	}

	private float getDiscount(int cxtCount,int order){
		float D = 0.0f;
		if(cxtCount==0){
			D = 0.0f;
		}else if(cxtCount==1){
			D = this.dMap.get(order).get("D1");
		}else if(cxtCount==2){
			D = this.dMap.get(order).get("D2");
		}else{
			D = this.dMap.get(order).get("D3plus");
		}
		return D;
	}

	private float getGamma(String cxt,int order){
		float D1 = this.dMap.get(order).get("D1");
		float D2 = this.dMap.get(order).get("D2");
		float D3plus = this.dMap.get(order).get("D3plus");
		HashMap<String,Integer> thisCxtMap = this.cMap.get(order).get(cxt);
		int N1 = (thisCxtMap!=null && thisCxtMap.containsKey("N1")) ? thisCxtMap.get("N1") : 0;
		int N2 = (thisCxtMap!=null && thisCxtMap.containsKey("N2")) ? thisCxtMap.get("N2") : 0;
		int N3plus = (thisCxtMap!=null && thisCxtMap.containsKey("N3plus")) ? thisCxtMap.get("N3plus") : 0;
		//System.out.format("%s %s %s %s %s %s\n",D1,N1,D2,N2,D3plus,N3plus);
		return D1*N1 + D2*N2 + D3plus*N3plus;
	}

	private float getLowerProb(String[] contexts,String child){
		float p = 0.0f;
		String cxt = StringUtils.join(contexts," ");
		String cw = String.format("%s %s",cxt,child);
		int order = contexts.length + 1;
		HashMap<String,Integer> thisBcwMap = this.bcwMap.get(order+1).get(cxt);
		HashMap<String,Integer> thisCxtMap = this.cMap.get(order+1).get(cxt);
		//System.out.println(Arrays.toString(contexts)+"->"+child);
		int c = (thisBcwMap!=null && thisBcwMap.containsKey(child)) ? thisBcwMap.get(child) : 0;
		float D = getDiscount(c,order);
		float gamma = getGamma(cxt,order);
		int denom =  (this.bcMap.get(order+1).containsKey(cxt)) ? this.bcMap.get(order+1).get(cxt).get("N1") : 0;
		//System.out.println(String.format("%s %s %s %s",c,D,gamma,denom));
		if(contexts.length==0){
			int V = this.cwMap.get(1).get("").size();
			int n = this.cMap.get(1).get("").get("C");
			p = (c - D + gamma / V) / (float)n;
		}else{
			String[] bcs = Arrays.copyOfRange(contexts,1,contexts.length);
			if(denom==0) p = getLowerProb(bcs,child);
			else p = (Math.max(c-D,0) + gamma * getLowerProb(bcs,child)) / denom;
		}
		return p;
	}

	public float getProb(String[] context,String child){
		String[] contexts = new String[order-1];
		int size = context.length;
		if(size<order-1){
			for(int i=0;i<order-1-size;i++){
				contexts[i] = "<B>";
			}
		}
		for(int i=order-1-size;i<order-1;i++){
			contexts[i] = context[i-order+1+size];
		}
		
		//System.out.println(Arrays.asList(contexts));

		String query = String.format("%s|%s",Arrays.toString(contexts),child);
		if(this.probCache.containsKey(query)) return this.probCache.get(query);
		float p = 0.0f;
		String cxt = StringUtils.join(contexts," ");
		String cw = String.format("%s %s",cxt,child);
		int order = contexts.length + 1;
		HashMap<String,HashMap<String,Integer>> thisCxtMap = this.cMap.get(order);
		HashMap<String,HashMap<String,Integer>> thisCwMap = this.cwMap.get(order);
		int c = (thisCwMap.containsKey(cxt) && thisCwMap.get(cxt).containsKey(child)) ? thisCwMap.get(cxt).get(child) : 0;
		float D = getDiscount(c,order);
		float gamma = getGamma(cxt,order);
		int denom = (thisCxtMap.containsKey(cxt)) ? thisCxtMap.get(cxt).get("C") : 0;
		//System.out.println(String.format("%s %s %s %s",c,D,gamma,denom));
		String[] bcs = Arrays.copyOfRange(contexts,1,contexts.length);
		if(denom==0) p = getLowerProb(bcs,child);
		else p = (Math.max(c-D,0) + gamma * getLowerProb(bcs,child)) / denom;
		this.probCache.put(query,p);
		return p;
	}

	private static void count(HashMap<String,HashMap<String,Integer>> map,String parent,String child,int freq){
		if(map.containsKey(parent)){
			HashMap<String,Integer> childMap = map.get(parent);
			if (map.get(parent).containsKey(child)){
				int count = (Integer)map.get(parent).get(child);
				childMap.put(child,count+freq);
			}else{
				childMap.put(child,freq);
			}
		}else{
			HashMap<String,Integer> childMap = new HashMap<String,Integer>();
			childMap.put(child,freq);
			map.put(parent,childMap);
		}
	}

	public void read(String seq){
		//System.out.println("seq:"+seq);
		String[] tokens = new String[order];
		String[] words = seq.split(" ");
		int size = words.length;
		if(size<order){
			for(int i=0;i<order-size;i++){
				tokens[i] = "<B>";
			}
		}
		for(int i=order-size;i<order;i++){
			tokens[i] = words[i-order+size];
		}
		String token = tokens[tokens.length-1];
		for(int i=0;i<order;i++){
			String context = StringUtils.join(Arrays.copyOfRange(tokens,i,order-1)," ");
			count(this.cwMap.get(order-i),context,token,1);
		}
	}

	public void train(){
		for(int o=1;o<=this.order;o++){
            System.out.format("training %s-gram model...\n",o);
			HashMap<String,HashMap<String,Integer>> thisMap = this.cwMap.get(o);
			//System.out.println(thisMap);
            int counter = 0;
            int size = thisMap.size();
			Iterator it = thisMap.entrySet().iterator();
			while(it.hasNext()){
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
                //train parameters
				Entry pair = (Entry)it.next();
				String cxt = (String)pair.getKey();
				String[] parents = cxt.split(" ");
				String bcx = StringUtils.join(Arrays.copyOfRange(parents,1,parents.length)," ");
				Iterator it2 = ((HashMap)pair.getValue()).entrySet().iterator();
				while(it2.hasNext()){
					Entry pair2 = (Entry)it2.next();
					String child = (String)pair2.getKey();
					Integer freq = (Integer)pair2.getValue();
					count(this.cMap.get(o),cxt,"C",freq);
					count(this.bcMap.get(o),bcx,"N1",1);
					if(o!=1) count(this.bcwMap.get(o),bcx,child,1);
					if(freq==1){
						count(this.nMap,String.valueOf(o),"n1",1);
						count(this.cMap.get(o),cxt,"N1",1);
					}
					if(freq==2){
						count(this.nMap,String.valueOf(o),"n2",1);
						count(this.cMap.get(o),cxt,"N2",1);
					}
					if(freq==3){
						count(this.nMap,String.valueOf(o),"n3",1);
					}
					if(freq==4){
						count(this.nMap,String.valueOf(o),"n4",1);
					}
					if(freq>=3){
						count(this.cMap.get(o),cxt,"N3plus",1);
					}
				}
			}
		}
        //nMap to dMap
        for(int o=1;o<=this.order;o++){
            HashMap<String,Integer> thisnMap = this.nMap.get(String.valueOf(o));
            HashMap<String,Float> thisdMap = this.dMap.get(o);
            int n1 = thisnMap.containsKey("n1") ? thisnMap.get("n1") : 0;
            int n2 = thisnMap.containsKey("n2") ? thisnMap.get("n2") : 0;
            int n3 = thisnMap.containsKey("n3") ? thisnMap.get("n3") : 0;
            int n4 = thisnMap.containsKey("n4") ? thisnMap.get("n4") : 0;
            float Y = (n1+n2!=0) ? ((float)n1)/(n1+n2) : 0.0f;
            float D1 = (n1!=0) ? 1-2*Y*n2/n1 : 0;
            float D2 = (n2!=0) ? 2-3*Y*n3/n2 : 0;
            float D3plus = (n3!=0) ? 3-4*Y*n4/n3 : 0;
            thisdMap.put("D1",D1);
            thisdMap.put("D2",D2);
            thisdMap.put("D3plus",D3plus);
        }
	}
}
