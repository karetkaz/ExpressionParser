public class Parser {
	/**
	 * Tokenize the input expression and build its abstract syntax tree.
	 *
	 * @return Abstract syntax tree for the input.
	 * @see <a href="https://eli.thegreenplace.net/2012/08/02/parsing-expressions-by-precedence-climbing">Algorithm</a>
	 */
	public static Node parse(String expression) throws Error {
		Lexer lexer = new Lexer(expression);
		Node root = parseBinary(lexer, 0);
		if (lexer.hasNext()) {
			throw new Error("Invalid expression", lexer.next(), lexer);
		}
		return root;
	}

	private static Node parseUnary(Lexer lexer) throws Error {
		Lexer.Token token = lexer.next();
		switch (token) {
			case RParen:
			case RBracket:
			case Undefined:
				// stop parsing
				lexer.back();
				return null;

			case Fun:
				Node fun = new Node(token, lexer);
				fun.right = parseBinary(lexer, 0);
				skipToken(lexer, Lexer.Token.RParen);
				return fun;

			case Idx:
				Node idx = new Node(token, lexer);
				idx.right = parseBinary(lexer, 0);
				skipToken(lexer, Lexer.Token.RBracket);
				return idx;

			case Value:
				return new Node(token, lexer);
		}

		if (!token.isUnaryOperator()) {
			if (token.unary == null) {
				throw new Error("Unary operator expected, got", token, lexer);
			}
			token = token.unary;
		}

		Node node = new Node(token, lexer);
		node.right = parseUnary(lexer);
		if (node.right == null) {
			throw new Error("Incomplete unary operator", node);
		}
		return node;
	}

	public static Node parseBinary(Lexer lexer, int minPrecedence) throws Error {
		Node root = parseUnary(lexer);
		if (root == null) {
			return null;
		}

		while (lexer.hasNext()) {
			Lexer.Token token = lexer.next();
			switch (token) {
				case RParen:
				case RBracket:
				case Undefined:
					// stop parsing
					lexer.back();
					return root;

				case Fun:
				case Idx:
					lexer.back();
					Node node = parseUnary(lexer);
					if (node == null) {
						throw new Error("Invalid expression", token, lexer);
					}
					node.left = root;
					root = node;
					continue;
			}

			if (!token.isBinaryOperator()) {
				throw new Error("Binary operator expected, got", token, lexer);
			}

			if (root.token.precedence <= token.precedence && root.isOperator()) {
				if (root.token.precedence < token.precedence || token.right2Left) {
					throw new Error("Precedence error, consider using parenthesis", token, lexer);
				}
			}

			if (token.precedence <= minPrecedence) {
				if (token.precedence < minPrecedence) {
					lexer.back();
					break;
				}
				if (!token.right2Left) {
					lexer.back();
					break;
				}
			}

			Node node = new Node(token, lexer);
			node.right = parseBinary(lexer, token.precedence);
			if (node.right == null) {
				throw new Error("Incomplete binary operator", node);
			}
			node.left = root;
			root = node;
		}

		return root;
	}

	private static void skipToken(Lexer lexer, Lexer.Token kind) throws Error {
		Lexer.Token token = lexer.next();
		if (kind != token) {
			throw new Error("Unexpected token", token, lexer);
		}
	}

	/**
	 * Abstract syntax tree node
	 */
	public static class Node {
		/**
		 * Kind of the node.
		 */
		private final Lexer.Token token;

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

		protected Node(Lexer.Token token, Lexer lexer) {
			this.token = token;
			this.text = lexer.getText();
			this.position = lexer.getPosition();
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

		/**
		 * Check whether the node is an operator.
		 * @return true if the node is operator.
		 */
		public boolean isOperator() {
			return token.isOperator();
		}

		/**
		 * Check whether the node is a unary operator.
		 * @return true if the node is unary operator.
		 */
		public boolean isUnaryOperator() {
			return token.isUnaryOperator();
		}

		@Override
		public String toString() {
			return token + "(:" + position + ", `" + text + "`)";
		}
	}
}
