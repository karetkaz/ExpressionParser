import java.util.Iterator;

public class Lexer implements Iterable<Lexer.Token>, Iterator<Lexer.Token> {
	// contains the separator characters of tokens
	private final boolean[] separators;

	// contains the input string.
	private final String input;

	// slice of the current token.
	private int previous = -1;
	private int start = 0;
	private int end = 0;

	public Lexer(String input) {
		this.input = input;
		this.separators = new boolean[256];

		// use whitespace and operator's first character as separator
		for (int i = 0; i < separators.length; ++i) {
			if (isWhitespace((char) i)) {
				separators[i] = true;
			}
		}
		for (Token token : Token.values()) {
			if (token.text == null || token.text.isEmpty()) {
				continue;
			}
			separators[token.text.charAt(0)] = true;
		}
	}

	/**
	 * Get the text of the current token.
	 * @return text of the current token.
	 */
	public String getText() {
		return input.substring(start, end);
	}

	/**
	 * Get the start position of the current token.
	 * @return position of the current token.
	 */
	public int getPosition() {
		return start;
	}

	/**
	 * Lexer is iterable, so return the class as iterator.
	 * @return iterator.
	 */
	@Override
	public Iterator<Token> iterator() {
		return this;
	}

	/**
	 * Check if the input contains unprocessed tokens.
	 * @return true if not all tokens consumed.
	 */
	@Override
	public boolean hasNext() {
		return end < input.length();
	}

	/**
	 * Scan for the next token in the input.
	 * @return next token from the input.
	 */
	@Override
	public Token next() {
		int inputEnd = input.length();

		// skip white spaces
		while (end < inputEnd) {
			char chr = input.charAt(end);
			if (!isWhitespace(chr)) {
				break;
			}
			end += 1;
		}

		// next token starts where previous token ended.
		previous = start;
		start = end;

		// find next operator
		while (end < inputEnd) {
			char chr = input.charAt(end);
			if (isSeparator(chr)) {
				break;
			}
			end += 1;
		}

		if (start < end) {
			return Token.Value;
		}

		Token match = null;
		for (Token token : Token.values()) {
			if (token.text == null || !input.startsWith(token.text, end)) {
				// skip if token does not start with kind's text
				continue;
			}
			if (match == null || match.text.length() < token.text.length()) {
				// use the longest match available
				match = token;
			}
			if (match == token.unary) {
				// use the binary version
				match = token;
			}
		}

		if (match == null) {
			// separator, but not an operator
			if (end < inputEnd) {
				end += 1;
			}
			return Token.Undefined;
		}

		end += match.text.length();
		return match;
	}

	/**
	 * Rollback the last read token
	 */
	public void back() {
		if (previous < 0) {
			throw new UnsupportedOperationException("Only one rollback is possible");
		}
		end = start;
		start = previous;
		previous = -1;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private static boolean isWhitespace(char chr) {
		return Character.isWhitespace(chr);
	}

	private boolean isSeparator(char chr) {
		return chr < separators.length && separators[chr];
	}

	@Override
	public String toString() {
		// just for debugging, show the rest of the unparsed input
		return "Lexer(:" + start + ", '" + getText() + "'): " + input.substring(end);
	}

	public enum Token {
		Fun(15, false, "("),
		Idx(15, false, "["),

		Pos(14, true, "+"),
		Neg(14, true, "-"),
		Cmt(14, true, "~"),
		Not(14, true, "!"),

		Pow(14, true, "**"),

		Mul(13, false, "*"),
		Div(13, false, "/"),
		Rem(13, false, "%"),

		Add(12, false, Pos.text, Pos),
		Sub(12, false, Neg.text, Neg),

		Shl(11, false, "<<"),
		Shr(11, false, ">>"),
		Sar(11, false, ">>>"),

		Lt(10, false, "<"),
		Leq(10, false, "<="),
		Gt(10, false, ">"),
		Geq(10, false, ">="),

		Eq(9, false, "=="),
		Neq(9, false, "<>"),

		And(8, false, "&"),
		Xor(7, false, "^"),
		Ior(6, false, "|"),

		All(5, false, "&&"),
		Any(4, false, "||"),
		Chk(3, true, "?"),
		Sel(3, true, ":"),

		Coma(1, true, ","),

		Value(null),
		RParen(")"),
		RBracket("]"),
		Undefined(null);

		Token(String text) {
			this(0, false, text, null);
		}

		Token(int precedence, boolean right2Left, String text) {
			this(precedence, right2Left, text, null);
		}

		Token(int precedence, boolean right2Left, String text, Token unary) {
			this.precedence = precedence;
			this.right2Left = right2Left;
			this.text = text;
			this.unary = unary;
		}

		/**
		 * Operator precedence level
		 * multiply is greater than addition,
		 * so: 3 + 4 * 5 == (3 + (4 * 5))
		 */
		public final int precedence;

		/**
		 * Operator associativity left to right or right to left
		 * ex: 1 + 2 + 3 => ((1 + 2) + 3)
		 * but: a = b = c => (a = (b = c))
		 */
		public final boolean right2Left;

		/**
		 * Text representation of the token.
		 */
		public final String text;

		/**
		 * Link tokens with similar text
		 * minus(-) is both binary(subtract) and unary(negate)
		 */
		public final Token unary;

		public boolean isOperator() {
			switch (this) {
				case Value:
				case RParen:
				case RBracket:
				case Undefined:
					return false;
			}
			return true;
		}

		public boolean isUnaryOperator() {
			switch (this) {
				case Pos:
				case Neg:
				case Cmt:
				case Not:
					return true;
			}
			return false;
		}

		public boolean isBinaryOperator() {
			return isOperator() && !isUnaryOperator();
		}
	}
}
