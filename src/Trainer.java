import org.apache.commons.cli.Options;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import structures.NST;
import smoothing.MKN;

/**
 * HWS Trainer
 *
 * @author Xiaoyi Wu
 * @version 0.9
 * @since 2016-05-18
 **/

public class Trainer{
	private static String mode;
	private static String assumption;
	private static int order;
	private static String input;
	private static String output;
	private static String outputPatterns;
	private static HashMap model = new HashMap(); 
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
		opts.addOption("m","mode",true,"The mode of conversion of HWS sequences. \n 'ASM': both training data and test data will be converted by the same assumption; \n 'NST': the test data will be converted by an NST trained from the training data. \n Default mode is 'ASM'.");
		opts.addOption("h", "help",false, "Print help for this application.");
		opts.addOption("a", "assumption",true,"The assumption used for sequences convertion. \n 'NGRAM': classical n-gram assumption; \n 'FB': frequency-based HWS assumption; \n 'DB': dice-coefficient-based HWS assumption; \n 'TB': T-Score-based HWS assumption; \n 'ABS': abstraction-based HWS assumption. \n Default assumption is 'FB'.");
		opts.addOption("n","order",true,"The order of word sequences. Default order is 3.");
		opts.addOption("i","input",true,"The path of training data file.");
		opts.addOption("o","output",true,"The name of output file.");
		DefaultParser parser = new DefaultParser();
		CommandLine cl = parser.parse(opts,args);
		if(cl.hasOption("h")){
			HelpFormatter hf = new HelpFormatter();
			hf.printHelp("OptionsTip",opts);
			System.exit(-1);
		}else{
			//mode
			if(cl.hasOption("m")){
				String[] modes = {"NST","ASM"};
				mode = cl.getOptionValue("m");
				if(!Arrays.asList(modes).contains(mode)){
					System.out.println("Mode has to be 'ASM' or 'NST'");
					System.exit(-1);
				}
			}else{
				mode = "ASM";
			}
			//assumption
			if(cl.hasOption("a")){
				String[] asms = {"NGRAM","FB","DB","TB","ABS"};
				assumption = cl.getOptionValue("a");
				if(!Arrays.asList(asms).contains(assumption)){
					System.out.println("Assumption has to be 'NGRAM','FB','DB','TB' or 'ABS'");
					System.exit(-1);
				}
			}else{
				assumption = "FB";
			}
			//order
			if(cl.hasOption("n")){
				order = Integer.parseInt(cl.getOptionValue("n"));
			}else{
				order = 3;
			}
			//input
			if(!cl.hasOption("i")){
				System.out.println("Please specify the input file. Use -h for help.");
				System.exit(-1);
			}else{
				input = cl.getOptionValue("i");
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
	public static void main(String[] args) throws ParseException,IOException{
		parseOpts(args);
		//read file
		File file = new File(input);
		String text = FileUtils.readFileToString(file);
		//String[] sens = StringUtils.split(text,System.lineSeparator());
		String[] sens = StringUtils.split(text,"\n");
		if(mode.equals("ASM")){
			//init converter
			converter = new Converter(mode,assumption,order);
			converter.init(sens,new HashMap());//extract patterns from sens
		}else if(mode.equals("NST")){
			nst = new NST(assumption,order);	
			nst.init(sens);
		}else{
			System.out.println("Mode has to be 'ASM' or 'NST'");
			System.exit(-1);
		}

		int counter = 0;
		int size = sens.length;
		file = new File(output+".pattern");
		if(file.exists()) file.delete();
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output+".pattern", true)));
		MKN mkn = new MKN(order);
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
				System.out.println("Mode has to be 'ASM' or 'NST'");
				System.exit(-1);
			}
			for(String seq:seqs){
				mkn.read(seq);
				pw.println(seq);
			}
			pw.println("</s>");
		}
		pw.close();
		mkn.train();
		//saving model
		System.out.println("Saving model file...");
		model.put("order",order);
		model.put("assumption",assumption);
		model.put("mode",mode);
		model.put("mkn",mkn);
		if(mode.equals("ASM")){
			model.put("extra",converter.hws.extraMap);
		}else if(mode.equals("NST")){
			model.put("extra",nst);
		}else{
			System.out.println("Mode has to be 'ASM' or 'NST'");
			System.exit(-1);
		}
		FileOutputStream fout = new FileOutputStream(output+".model");
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(model);
		oos.close();
	}

}
