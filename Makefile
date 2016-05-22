build:
	javac -cp "lib/*:" src/nlp/structures/GHS.java -d bin
	javac -cp "bin:lib/*:" src/nlp/structures/HWS.java -d bin
	javac -cp "bin:lib/*:" src/nlp/structures/NGRAM.java -d bin
	javac -cp "bin:lib/*:" src/nlp/structures/FB.java -d bin
	javac -cp "bin:lib/*:" src/nlp/structures/DB.java -d bin
	javac -cp "bin:lib/*:" src/nlp/structures/TB.java -d bin
	javac -cp "bin:lib/*:" src/nlp/structures/ABS.java -d bin
	javac -cp "bin:lib/*" src/nlp/HWS/converter.java -d bin


clean:
	rm -rf bin/*

run:
	java -cp "bin:lib/*" nlp.HWS.converter -m MIB -i corpus/ted.test -o mib.test -n 3# -e abs.train.extra

rebuild:
	make clean
	make build
