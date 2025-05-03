import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Vector;

public class ParserIterative {
	/**
	 * Tokenize the input expression and build its abstract syntax tree.
	 *
	 * @return Abstract syntax tree for the input.
	 * @see <a href="https://en.wikipedia.org/wiki/Shunting_yard_algorithm">Algorithm</a>
	 */
	public static Node parse(Lexer lexer) throws Error {
		Stack<Node> operators = new Stack<>();
		Stack<Lexer.Token> parens = new Stack<>();
		Vector<Node> postfix = new Vector<>();
		boolean unary = true;

		Lexer.Token previousToken = null;
		while (lexer.hasNext()) {
			Lexer.Token token = lexer.nextToken();
			if (unary && token.getUnary() != null) {
				// switch to unary token in unary mode to use correct precedence
				token = token.getUnary();
			}

			int precedence = parens.size() * Lexer.Token.Fun.precedence + token.precedence;
			final Lexer.Token previous = previousToken;
			previousToken = token;
			switch (token) {
				case Undefined:
					if (!lexer.hasNext()) {
						continue;
					}
					break;

				case Value:
					if (!unary) {
						throw new Error("Unexpected token", token, lexer);
					}
					postfix.add(new Node(token, precedence, lexer));
					unary = false;
					continue;

				case RParen:
					if (parens.pop() != Lexer.Token.Fun || (unary && previous != Lexer.Token.Fun)) {
						throw new Error("Unexpected token", token, lexer);
					}
					if (unary) {
						// case of: `method()`
						postfix.add(null);
						unary = false;
					}
					continue;

				case RBracket:
					if (parens.pop() != Lexer.Token.Idx || (unary && previous != Lexer.Token.Idx)) {
						throw new Error("Unexpected token", token, lexer);
					}
					if (unary) {
						// case of: `array[]`
						postfix.add(null);
						unary = false;
					}
					continue;

				case Fun:
				case Idx:
					parens.push(token);
					if (unary) {
						// case of: `(x + 2)` or `[x + 1]`
						postfix.add(null);
						unary = false;
					}
					break;
			}

			if (!token.isBinaryOperator() && !token.isUnaryOperator()) {
				throw new Error("Operator expected", token, lexer);
			}
			if (unary != token.isUnaryOperator()) {
				throw new Error((unary ? "Unary" : "Binary") + " operator expected", token, lexer);
			}

			while (!operators.isEmpty() && operators.peek().precedes(precedence)) {
				postfix.add(operators.pop());
			}
			operators.push(new Node(token, precedence, lexer));
			unary = true;
		}

		if (!parens.isEmpty()) {
			throw new Error("Missing: " + parens.peek());
		}

		// flush operators left on stack
		while (!operators.isEmpty()) {
			postfix.add(operators.pop());
		}

		// build the syntax tree.
		for (Node node : postfix) {
			try {
				if (node != null) {
					if (node.token.isBinaryOperator()) {
						node.right = operators.pop();
						node.left = operators.pop();
					} else if (node.token.isUnaryOperator()) {
						node.right = operators.pop();
						node.left = null;
					}
				}
				operators.push(node);
			} catch (EmptyStackException e) {
				if (node == null) {
					throw new Error("Invalid expression");
				}
				throw new Error("Syntax error near", node);
			}
		}
		if (operators.size() != 1) {
			throw new Error("Invalid expression");
		}
		return operators.pop();
	}

	/**
	 * Abstract syntax tree node
	 */
	public static class Node extends Parser.Node {
		/**
		 * Precedence level of the node.
		 */
		public final int precedence;

		public Node(Lexer.Token token, int precedence, Lexer lexer) {
			super(token, lexer.getPosition(), lexer.getText());
			this.precedence = precedence;
		}

		/**
		 * Check if this node has a higher precedence level than the given one.
		 * @param level precedence level to check with.
		 * @return true if the precedence level is greater.
		 */
		public boolean precedes(int level) {
			if (this.token.right2left) {
				return this.precedence > level;
			}
			return this.precedence >= level;
		}
	}
}
