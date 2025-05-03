/**
 * The Parser class provides functionality to build the abstract syntax tree (AST).
 * It uses the {@code Lexer} class to read and tokenize the input.
 * It uses the precedence climbing algorithm to build up the tree.
 * @see <a href="https://eli.thegreenplace.net/2012/08/02/parsing-expressions-by-precedence-climbing">Algorithm</a>
 */
public class Parser {
	/**
	 * Parses the given expression string and generates an abstract syntax tree.
	 *
	 * @param expression The input expression in string format to be parsed.
	 * @return The root node of the resulting abstract syntax tree.
	 * @throws Error If there are any parsing errors.
	 */
	public static Node parse(String expression) throws Error {
		return parse(new Lexer(expression));
	}

	/**
	 * Parses the input provided by the lexer and generates an abstract syntax tree.
	 * The method ensures that the entirety of the input has been consumed during parsing.
	 *
	 * @param lexer The lexer instance that tokenizes the input stream for parsing.
	 * @return The root node of the resulting abstract syntax tree.
	 * @throws Error If there are any parsing errors.
	 */
	public static Node parse(Lexer lexer) throws Error {
		Node node = parseBinary(lexer, 0);
		if (lexer.hasNext()) {
			throw new Error("End of input expected, got", lexer.nextToken(), lexer);
		}
		return node;
	}

	/**
	 * Parses unary expressions from the input provided by the lexer. It evaluates tokens
	 * representing unary operators, values, functions, or indexed elements and creates
	 * corresponding abstract syntax tree nodes.
	 *
	 * @param lexer The lexer instance that tokenizes the input stream for parsing.
	 * @return A node representing the parsed unary expression or structure in the abstract syntax tree.
	 * @throws Error If the syntax of the input fails to match the expected unary expression.
	 */
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

	/**
	 * Parses binary expressions from the input provided by the lexer.
	 * This method handles tokens representing binary operators and constructs
	 * the corresponding abstract syntax tree nodes based on operator precedence
	 * and associative rules.
	 *
	 * @param lexer The lexer instance that tokenizes the input stream for parsing.
	 * @param minPrecedence The minimum operator precedence allowed for this parsing.
	 *                       Operators with precedence below this value will not be parsed.
	 * @return A node representing the parsed binary expression or structure in the abstract syntax tree.
	 * @throws Error If the syntax of the input fails to match the expected binary expression.
	 */
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
					throw new Error("Precedence error, consider using parenthesis around", token, lexer);
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
	 * Represents a node in the abstract syntax tree.
	 * Each node contains details about its token (kind, text, position), and links to its left and right child nodes.
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

		/**
		 * Constructs a Node with the specified token, position, and text.
		 *
		 * @param token the token associated with this node
		 * @param position the position of the token in the input
		 * @param text the text value of this node
		 */
		protected Node(Lexer.Token token, int position, String text) {
			this.token = token;
			this.position = position;
			this.text = text;
		}

		/**
		 * Retrieves the token associated with this node.
		 *
		 * @return the token of this node
		 */
		public Lexer.Token getToken() {
			return token;
		}

		/**
		 * Retrieves the position of the token associated with this node in the input.
		 *
		 * @return the position of the token in the input
		 */
		public int getPosition() {
			return position;
		}

		/**
		 * Retrieves the text value associated with this node.
		 *
		 * @return the text value of this node
		 */
		public String getText() {
			return text;
		}

		/**
		 * Retrieves the left child node of the current node.
		 *
		 * @return the left child node, or null if no left child exists
		 */
		public Node getLeft() {
			return left;
		}

		/**
		 * Retrieves the right child node of the current node.
		 *
		 * @return the right child node, or null if no right child exists
		 */
		public Node getRight() {
			return right;
		}

		@Override
		public String toString() {
			return token + "(:" + position + ", `" + text + "`)";
		}
	}
}
