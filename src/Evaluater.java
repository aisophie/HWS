import org.apache.commons.cli.Options;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.ObjectInputStream;


import smoothing.MKN;
import structures.NST;

/**
 * HWS Evaluater
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-05-19
 **/

public class Evaluater{
	private static String mode;
	private static String assumption;
	private static int order;
	private static String input;
	private static String output;
	private static String modelFile;
	private static String outputPatterns;
	private static Converter converter;
	private static NST nst;
	
	
	/**
	 * Parse command line options.
	 * 
	 * @param args command line options.
	 * @throws ParseException if options are invalid.
	 */
	private static void parseOpts(String[] args) throws ParseException{
		Options opts = new Options();
		opts.addOption("h", "help",false, "Print help for this application.");
		opts.addOption("i","input",true,"The path of training data file.");
		opts.addOption("o","output",true,"The name of output file.");
		opts.addOption("m","model",true,"The model trained from training data.");
		DefaultParser parser = new DefaultParser();
		CommandLine cl = parser.parse(opts,args);
		if(cl.hasOption("h")){
			HelpFormatter hf = new HelpFormatter();
			hf.printHelp("OptionsTip",opts);
			System.exit(-1);
		}else{
			//input
			if(!cl.hasOption("i")){
				System.out.println("Please specify the input file. Use -h for help.");
				System.exit(-1);
			}else{
				input = cl.getOptionValue("i");
			}
			//model
			if(!cl.hasOption("m")){
				System.out.println("Please specify the model file. Use -h for help.");
				System.exit(-1);
			}else{
				modelFile = cl.getOptionValue("m");
			}
			//output
			if(!cl.hasOption("o")){
				System.out.println("Please specify the name of output file. Use -h for help.");
				System.exit(-1);
			}else{
				output = cl.getOptionValue("o");
			}
		}
	}



	/**
	 * Main function.
	 *
	 * @param args command line options. Use -h to print help.
	 * @throws ParseException if options are invalid.
	 * @throws IOException if input file is invalid.
	 **/
	public static void main(String[] args) throws ParseException,IOException,ClassNotFoundException{
		parseOpts(args);
		//read test data
		File file = new File(input);
		String text = FileUtils.readFileToString(file);
		//String[] sens = StringUtils.split(text,System.lineSeparator());
		String[] sens = StringUtils.split(text,"\n");
		//read model
		FileInputStream fis = new FileInputStream(modelFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		HashMap model = (HashMap)ois.readObject();
		mode = (String)model.get("mode");
		order = (int)model.get("order");
		assumption = (String)model.get("assumption");
		MKN mkn = (MKN)model.get("mkn");
		ois.close();
		//init converter
		if(mode.equals("ASM")){
			converter = new Converter(mode,assumption,order);
			HashMap extra = (HashMap)model.get("extra");
			converter.init(sens,extra);//extract patterns from sens
		}else if(mode.equals("NST")){
			nst = (NST)model.get("extra");
			nst.reinit(sens);
		}else{
			System.out.println("Mode has to be 'NST' or 'ASM'");
			System.exit(-1);
		}

		int counter = 0;
		int size = sens.length;
		file = new File(output+".pattern");
		if(file.exists()) file.delete();
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output+".pattern", true)));
		File file2 = new File(output+".results");
		if(file2.exists()) file2.delete();
		PrintWriter pw2 = new PrintWriter(new BufferedWriter(new FileWriter(output+".results", true)));
		//for each sentence
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
			//processing
			ArrayList<String> seqs = new ArrayList<String>();
			if(mode.equals("ASM")){
				seqs = converter.convert(sen);
			}else if(mode.equals("NST")){
				seqs = nst.convert(sen);
			}else{
				System.out.println("Mode has to be 'NST' or 'ASM'");
				System.exit(-1);
			}

			double totalprob = 0.0;
			for(String seq:seqs){
				String[] tokens = seq.split(" ");
				//System.out.println(Arrays.asList(tokens));
				String[] contexts = Arrays.copyOfRange(tokens,0,tokens.length-1);
				String child = tokens[tokens.length-1];
				float prob = mkn.getProb(contexts,child);
				double logprob = -Math.log(prob);
				totalprob += logprob;
				pw.println(seq);
			}
			double pp = Math.exp(totalprob/seqs.size());
			pw.println("</s>");
			pw2.println(String.format("%s\t%s",sen,pp));
		}
		pw.close();
		pw2.close();

	}

}
