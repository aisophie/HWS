build:
	javac -cp "bin:lib/*:" src/smoothing/MKN.java -d bin
	javac -cp "bin:lib/*:" src/structures/GHS.java -d bin
	javac -cp "bin:lib/*:" src/structures/HWS.java -d bin
	javac -cp "bin:lib/*:" src/structures/NGRAM.java -d bin
	javac -cp "bin:lib/*:" src/structures/NST.java -d bin
	javac -cp "bin:lib/*:" src/structures/FB.java -d bin
	javac -cp "bin:lib/*:" src/structures/DB.java -d bin
	javac -cp "bin:lib/*:" src/structures/TB.java -d bin
	javac -cp "bin:lib/*:" src/structures/ABS.java -d bin
	javac -cp "bin:lib/*:" src/Converter.java -d bin
	javac -cp "bin:lib/*:" src/Trainer.java -d bin
	javac -cp "bin:lib/*:" src/Evaluater.java -d bin
run:
	java -cp "bin:lib/*" Trainer
