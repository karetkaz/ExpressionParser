public class Parser {
	/**
	 * Tokenize the input expression and build its abstract syntax tree.
	 *
	 * @return Abstract syntax tree for the input.
	 * @see <a href="https://eli.thegreenplace.net/2012/08/02/parsing-expressions-by-precedence-climbing">Algorithm</a>
	 */
	public static Node parse(String expression) throws Error {
		return parse(new Lexer(expression));
	}

	public static Node parse(Lexer lexer) throws Error {
		Node node = parseBinary(lexer, 0);
		if (lexer.hasNext()) {
			throw new Error("End of input expected, got", lexer.nextToken(), lexer);
		}
		return node;
	}

	private static Node parseUnary(Lexer lexer) throws Error {
		Lexer.Token token = lexer.nextToken();
		switch (token) {
			case Value:
				return new Node(token, lexer.getPosition(), lexer.getText());

			case Fun:
				int position = lexer.getPosition();
				if (lexer.nextToken() == Lexer.Token.RParen) {
					// allow empty list arguments: '(' ')'
					return new Node(token, position, token.text);
				}

				lexer.backToken();
				Node fun = new Node(token, position, token.text);
				fun.right = parseBinary(lexer, 0);
				if (lexer.nextToken() != Lexer.Token.RParen) {
					throw new Error("Right parenthesis expected, got", token, lexer);
				}
				return fun;

			case Idx:
				Node idx = new Node(token, lexer.getPosition(), token.text);
				idx.right = parseBinary(lexer, 0);
				if (lexer.nextToken() != Lexer.Token.RBracket) {
					throw new Error("Right bracket expected, got", token, lexer);
				}
				return idx;
		}

		if (token.getUnary() == null) {
			throw new Error("Unary operator expected", token, lexer);
		}

		Node node = new Node(token.getUnary(), lexer.getPosition(), token.text);
		node.right = parseUnary(lexer);
		return node;
	}

	private static Node parseBinary(Lexer lexer, int minPrecedence) throws Error {
		Node root = parseUnary(lexer);
		while (lexer.hasNext()) {
			Lexer.Token token = lexer.nextToken();
			switch (token) {
				case RParen:
				case RBracket:
				case Undefined:
					// stop parsing
					lexer.backToken();
					return root;

				case Fun:
				case Idx:
					lexer.backToken();
					Node node = parseUnary(lexer);
					node.left = root;
					root = node;
					continue;
			}

			if (!token.isBinaryOperator()) {
				throw new Error("Binary operator expected, got", token, lexer);
			}

			if (token.precedence >= root.token.precedence && root.token.isUnaryOperator()) {
				if (token.precedence > root.token.precedence || token.right2left) {
					throw new Error("Precedence error, consider using parenthesis", token, lexer);
				}
			}

			if (token.precedence <= minPrecedence) {
				if (token.precedence < minPrecedence) {
					lexer.backToken();
					break;
				}
				if (!token.right2left) {
					lexer.backToken();
					break;
				}
			}

			Node node = new Node(token, lexer.getPosition(), token.text);
			node.right = parseBinary(lexer, token.precedence);
			node.left = root;
			root = node;
		}

		return root;
	}

	/**
	 * Abstract syntax tree node
	 */
	public static class Node {
		/**
		 * Kind of the node.
		 */
		protected final Lexer.Token token;

		/**
		 * Position of token in the input.
		 * Used mainly for error reporting.
		 */
		private final int position;

		/**
		 * Text value of the node.
		 */
		private final String text;

		/**
		 * Link to the left child.
		 */
		protected Node left = null;

		/**
		 * Link to the right child.
		 */
		protected Node right = null;

		protected Node(Lexer.Token token, int position, String text) {
			this.token = token;
			this.position = position;
			this.text = text;
		}

		public Lexer.Token getToken() {
			return token;
		}

		public int getPosition() {
			return position;
		}

		public String getText() {
			return text;
		}

		public Node getLeft() {
			return left;
		}

		public Node getRight() {
			return right;
		}

		@Override
		public String toString() {
			return token + "(:" + position + ", `" + text + "`)";
		}
	}
}
