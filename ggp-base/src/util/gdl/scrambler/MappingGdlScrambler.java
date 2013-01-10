package util.gdl.scrambler;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import external.Base64Coder.Base64Coder;

import util.crypto.BaseHashing;
import util.files.FileUtils;
import util.gdl.factory.GdlFactory;
import util.gdl.factory.exceptions.GdlFormatException;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlVariable;
import util.symbol.factory.exceptions.SymbolFormatException;

public class MappingGdlScrambler implements GdlScrambler {	
	private Map<String,String> scrambleMapping;	
	private Map<String,String> unscrambleMapping;
	private Stack<String> prepopulatedFakeWords;
	private Random random;
	
	public MappingGdlScrambler() {
		random = new Random();
		scrambleMapping = new HashMap<String,String>();
		unscrambleMapping = new HashMap<String,String>();
		
		prepopulatedFakeWords = new Stack<String>();
		String wordListFile = FileUtils.readFileAsString(new File("src/util/gdl/scrambler/WordList"));
		if (wordListFile != null && !wordListFile.isEmpty()) {
			String[] words = wordListFile.split("[\n\r]");
			for (String word : words) {
				prepopulatedFakeWords.push(word);
			}
			Collections.shuffle(prepopulatedFakeWords);
		}		
	}
	
	private class ScramblingRenderer extends GdlRenderer {
		@Override
		protected String renderConstant(GdlConstant constant) {
			return scrambleWord(constant.getValue());
		}
		@Override
		protected String renderVariable(GdlVariable variable) {
			return scrambleWord(variable.toString());
		}		
	}
	private class UnscramblingRenderer extends GdlRenderer {
		@Override
		protected String renderConstant(GdlConstant constant) {
			return unscrambleWord(constant.getValue());
		}
		@Override
		protected String renderVariable(GdlVariable variable) {
			return unscrambleWord(variable.toString());
		}		
	}	
	
	@Override
	public String scramble(Gdl x) {
		return new ScramblingRenderer().renderGdl(x);
	}
	
	@Override
	public Gdl unscramble(String x) throws SymbolFormatException, GdlFormatException {
		return GdlFactory.create(new UnscramblingRenderer().renderGdl(GdlFactory.create(x)));
	}
	
	@Override
	public boolean scrambles() {
		return true;
	}
	
	private String scrambleWord(String realWord) {
		if (!shouldMap(realWord)) {
			return realWord;
		}
		if (!scrambleMapping.containsKey(realWord)) {
			String fakeWord = generateNewRandomWord();
			if (realWord.startsWith("?")) {
				fakeWord = "?" + fakeWord;
			}
			scrambleMapping.put(realWord, fakeWord);
			unscrambleMapping.put(fakeWord, realWord);
		}
		return scrambleMapping.get(realWord);
	}
	
	private String unscrambleWord(String fakeWord) {
		if (!shouldMap(fakeWord)) {
			return fakeWord;
		}
		fakeWord = fakeWord.toLowerCase();
		if (!unscrambleMapping.containsKey(fakeWord)) {
			throw new RuntimeException("Could not find scramble mapping for: " + fakeWord);
		}
		return unscrambleMapping.get(fakeWord);
	}

	private String generateNewRandomWord() {
		String word = null;		
		do {
			word = generateRandomWord().toLowerCase();
		} while (unscrambleMapping.containsKey(word));
		return word;
	}
	
	private String generateRandomWord() {
		if (prepopulatedFakeWords.isEmpty()) {
			return Base64Coder.encodeString(BaseHashing.computeSHA1Hash("" + System.currentTimeMillis() + random.nextLong())).replace('+', 'a').replace('/', 'b').replace('=','c').toLowerCase();
		} else {
			return prepopulatedFakeWords.pop();
		}
	}
	
	private static boolean shouldMap(String token) {
		if (GdlPool.keywords.contains(token.toLowerCase())) {
			return false;
		}
		try {
			Integer.parseInt(token);
			return false;
		} catch (NumberFormatException e) {
			;
		}
		return true;
	}
}