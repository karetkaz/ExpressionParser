public class Lexer {
	// contains the separator characters of tokens
	private final boolean[] delimiters;

	// contains the input string.
	private final String input;

	// slice of the current token.
	private int previous = -1;
	private int start = 0;
	private int end = 0;

	private static final Token[] TOKEN_VALUES = Token.values();

	public Lexer(String input) {
		if (input == null) {
			throw new IllegalArgumentException("Input cannot be null");
		}
		this.input = input;
		this.delimiters = new boolean[256];

		// use whitespace and operator's first character as separator
		for (int i = 0; i < delimiters.length; i++) {
			delimiters[i] = isWhitespace((char) i);
		}

		for (Token token : Token.values()) {
			if (token.text != null && !token.text.isEmpty()) {
				delimiters[token.text.charAt(0)] = true;
			}
		}
	}

	/**
	 * Check if the input contains unprocessed tokens.
	 * @return true if not all tokens consumed.
	 */
	public boolean hasNext() {
		return end < input.length();
	}

	/**
	 * Scan for the next token in the input.
	 * @return next token from the input.
	 */
	public Token nextToken() {
		// skip white spaces
		while (end < input.length()) {
			if (!isWhitespace(input.charAt(end))) {
				break;
			}
			end++;
		}

		// next token starts where previous token ended.
		previous = start;
		start = end;

		// find next operator
		while (end < input.length()) {
			if (isDelimiter(input.charAt(end))) {
				break;
			}
			end++;
		}

		if (start < end) {
			return Token.Value;
		}

		Token match = null;
		for (Token token : TOKEN_VALUES) {
			if (token.text == null || !input.startsWith(token.text, start)) {
				// skip if token does not start with kind's text
				continue;
			}
			if (match == null || match.text.length() < token.text.length()) {
				// use the longest match available
				match = token;
			}
			if (match == token.getUnary() && match.text.equals(token.text)) {
				// use the binary version
				match = token;
			}
		}

		if (match != null) {
			end += match.text.length();
			return match;
		}

		// separator, but not an operator
		end += 1;
		return Token.Undefined;
	}

	/**
	 * Rollback the last read token
	 */
	public void backToken() throws Error {
		if (previous < 0) {
			throw new Error("can not push back: ", Token.Undefined, this);
		}
		end = start;
		start = previous;
		previous = -1;
	}

	/**
	 * Get the start position of the current token.
	 * @return position of the current token.
	 */
	public int getPosition() {
		return start;
	}

	/**
	 * Get the text of the current token.
	 * @return text of the current token.
	 */
	public String getText() {
		return input.substring(start, end);
	}

	private static boolean isWhitespace(char chr) {
		return Character.isWhitespace(chr);
	}

	private boolean isDelimiter(char chr) {
		return chr < delimiters.length && delimiters[chr];
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

		Add(12, false, "+"),
		Sub(12, false, "-"),

		Shl(11, false, "<<"),
		Shr(11, false, ">>>"),
		Sar(11, false, ">>"),

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

		Set(2, true, "="),
		SetAdd(2, true, "+="),
		SetSub(2, true, "-="),
		SetMul(2, true, "*="),
		SetDiv(2, true, "/="),
		SetRem(2, true, "%="),

		Coma(1, false, ","),

		Value(0, false, null),
		RParen(0, false, ")"),
		RBracket(0, false, "]"),
		Undefined(0, false, null);

		Token(int precedence, boolean right2left, String text) {
			this.precedence = precedence;
			this.right2left = right2left;
			this.text = text;
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
		public final boolean right2left;

		/**
		 * Text representation of the token.
		 */
		public final String text;

		public Token getUnary() {
			// allow some operators to behave as unary, the left branch will be null
			switch (this) {
				case Neg: // -9
				case Pos: // +9
				case Cmt: // ~9
				case Not: // !9
					return this;

				case Add: // +9
					return Pos;

				case Sub: // -9
					return Neg;
			}
			return null;
		}

		public boolean isUnaryOperator() {
			return this == getUnary();
		}

		public boolean isBinaryOperator() {
			// disallow some tokens to be used as (binary) operators
			switch (this) {
				case Neg:
				case Pos:
				case Cmt:
				case Not:
				case Value:
				case RParen:
				case Undefined:
					return false;
			}
			return true;
		}
	}
}
